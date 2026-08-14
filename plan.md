# AI Crypto Advisor — Implementation Plan

## Context

`specs.md` (already written, repo-root at `C:\Users\david\AI_Crypto`) fully specifies the AI Crypto Advisor take-home: a React + Spring Boot + Postgres app with JWT auth, an onboarding quiz, a 4-section daily dashboard (Coin Prices / Market News / AI Insight / Meme), and per-item voting, deployed to Netlify + Railway. The repo (`https://github.com/DavidSalmon13/AI-Crypto-Advisor`) is currently **empty**, and the local `AI_Crypto` folder is **not yet a git repository** — nothing has been scaffolded. This plan turns the spec into an ordered build sequence: what to build, in what order, and how to verify each increment before moving on, so integration problems (auth headers, CORS, DTO mismatches) surface early rather than in one large final integration pass.

## Decisions resolved before planning

1. **Local dev DB**: Docker Compose Postgres 16 in `/backend`, matching prod schema (Flyway runs against it too). Local dev never touches the Railway DB.
2. **Frontend tests**: none automated — matches specs.md's explicit backend-only test scoping. Each frontend phase is verified manually in a browser.
3. **CI**: a test-only GitHub Actions workflow (`mvn test`, no frontend step since there are no frontend tests) runs on every push/PR — separate from Railway/Netlify's native deploy hooks, which are untouched.
4. **Build sequencing**: vertical slices — each phase ships a working, demoable increment (backend + frontend + verification) for one feature area, in the order Auth → Onboarding → Dashboard → Feedback → Deployment.

## Flags — assumptions made to keep this plan concrete (not blocking, but worth a sanity check before/while executing)

- **Git bootstrap**: the local folder has no `.git` and the GitHub repo is empty. Phase 0 does `git init`, sets the `origin` remote to the given URL, and makes the first commit — confirm this is desired (vs. you cloning the empty repo fresh and copying files in) before Phase 0 runs.
- **API key acquisition is on the critical path**: OpenRouter (Phase 5) requires free-tier signup for an API key *before* its step can be verified end-to-end. Market News uses public RSS feeds (no key) as of 2026-08, after CryptoPanic, CryptoCompare/CoinDesk Data, and CoinGecko's News endpoint all turned out to require a paid plan.
- **`OPENROUTER_MODEL` drift**: specs.md pins `poolside/laguna-xs-2.1:free` as the default (verified live 2026-08, after `meta-llama/llama-3.1-8b-instruct:free` was retired from the free tier). Free-model availability on OpenRouter changes over time, and several current free models are reasoning models whose hidden chain-of-thought consumes `max_tokens` before any visible text is produced — verify a candidate replacement actually finishes with `finish_reason: "stop"` (not `"length"`) under the real prompt before swapping, not just that it responds.
- **Secrets locally**: backend uses `backend/.env` (loaded via `spring-dotenv` or Docker Compose `env_file`) — not committed; `backend/.env.example` documents required keys. Frontend uses `frontend/.env.local` (Vite convention) with `.env.example` counterpart. This mechanism isn't in specs.md and is filled in here as an implementation detail, not a spec change.

---

## Phase 0 — Repo & environment bootstrap

**Status**: Done

**What**: Initialize git, connect to the GitHub remote, create the monorepo skeleton folders, add root `.gitignore`, and confirm toolchain versions are installed locally.

**Files/components**: `AI_Crypto/.git`, `AI_Crypto/.gitignore`, `AI_Crypto/backend/` (empty), `AI_Crypto/frontend/` (empty). `specs.md` and `AI_USAGE.md` (create empty stub with a short header) move into this repo root as tracked files.

**Depends on**: nothing.

**Verify**:
- `git remote -v` shows `origin` pointing at `https://github.com/DavidSalmon13/AI-Crypto-Advisor.git`.
- `java -version` → 21.x, `mvn -version`, `node -version` → 20.x, `docker --version` all resolve.
- Initial commit pushed; GitHub repo shows `specs.md`, `AI_USAGE.md`, empty `backend/`/`frontend/` dirs (via `.gitkeep`).

---

## Phase 1 — Backend skeleton + local Postgres

**Status**: Done

**What**: Generate the Spring Boot project (Web, Security, Validation, Data JPA, Flyway, PostgreSQL driver, Actuator), set up package structure per specs.md §3.1, add `docker-compose.yml` for local Postgres 16, write the `V1__init.sql` Flyway migration (full schema from specs.md §5.1), and expose `/actuator/health`.

**Files/components**:
- `backend/pom.xml`
- `backend/src/main/java/com/moveo/aicryptoadvisor/AiCryptoAdvisorApplication.java`
- `backend/src/main/java/com/moveo/aicryptoadvisor/{config,security,controller,service,client,repository,entity,dto,exception}/` (empty packages with `.gitkeep` or a marker class where needed)
- `backend/src/main/resources/application.yml`, `application-local.yml`
- `backend/src/main/resources/db/migration/V1__init.sql` (schema exactly as specs.md §5.1)
- `backend/docker-compose.yml` (Postgres 16 service, exposed on 5432, volume for persistence)
- `backend/.env.example`, `backend/.gitignore` addition for `.env`

**Depends on**: Phase 0.

**Verify**:
- `docker compose up -d` starts Postgres locally.
- `mvn spring-boot:run` (with local profile pointed at the Docker Postgres) boots cleanly; Flyway logs show `V1__init.sql` applied.
- `psql` into the local container and confirm all 4 tables (`users`, `user_preferences`, `daily_content`, `feedback`) and both indexes exist exactly as specs.md §5.1.
- `curl localhost:8080/actuator/health` → `{"status":"UP"}`.

---

## Phase 2 — Frontend skeleton

**Status**: Done

**What**: Scaffold the Vite + React + TypeScript app, install Tailwind, TanStack Query, React Router, Axios; build the folder structure from specs.md §3.2 with placeholder pages; wire `AppRouter` with the route guard rules (public `/login`/`/register`, everything else behind auth) using stub `AuthContext` (not yet backed by real auth).

**Files/components**:
- `frontend/` (Vite scaffold), `frontend/tailwind.config.js`, `frontend/src/{api,lib,features,components,types,routes}/`
- `frontend/src/routes/AppRouter.tsx`, `frontend/src/components/ProtectedRoute.tsx`
- `frontend/.env.example` (`VITE_API_BASE_URL=http://localhost:8080`)

**Depends on**: Phase 0 (independent of Phase 1's backend internals, but both must exist before Phase 3 connects them).

**Verify**:
- `npm run dev` serves the app; navigating to `/dashboard` while unauthenticated redirects to `/login` (using the stub context); Tailwind classes render styled output (not unstyled HTML) on a placeholder page.

---

## Phase 3 — Auth vertical slice

**Status**: Done

**What**: Implement register/login end-to-end per specs.md §4.1: `User` entity + repository, `BCryptPasswordEncoder`, `JwtService` (HS256, 24h expiry, claims per spec), `JwtAuthFilter`, `SecurityConfig` (CORS allow-list from `FRONTEND_ORIGIN`, permit `/api/auth/**` and `/actuator/health`, authenticate everything else), `AuthController` + `AuthService`, `GlobalExceptionHandler` (error envelope per §4.5, at minimum: validation → 400, duplicate email → 409, bad login → 401). Frontend: `LoginPage`, `RegisterPage`, real `AuthContext` (stores JWT + user in memory, exposes `login`/`register`/`logout`), `apiClient.ts` with request interceptor attaching `Authorization: Bearer` and a 401 response interceptor that clears context and redirects to `/login`.

**Files/components**: backend `entity/User.java`, `repository/UserRepository.java`, `security/{JwtService,JwtAuthFilter}.java`, `config/SecurityConfig.java`, `controller/AuthController.java`, `service/AuthService.java`, `dto/request/{RegisterRequest,LoginRequest}.java`, `dto/response/AuthResponse.java`, `exception/GlobalExceptionHandler.java`. Frontend `features/auth/{LoginPage,RegisterPage,AuthContext}.tsx`, `hooks/useAuth.ts`, `api/authApi.ts`, `lib/apiClient.ts`.

**Depends on**: Phase 1 (DB/entity layer), Phase 2 (frontend shell/router).

**Verify**:
- Backend: `curl -X POST /api/auth/register` with valid body → `201` with token; duplicate email → `409 EMAIL_TAKEN`; weak password (no digit) → `400` with `fieldErrors.password`. `curl -X POST /api/auth/login` with wrong password → `401 INVALID_CREDENTIALS`.
- JUnit: `JwtServiceTest` (token round-trip: generate → parse claims → validate expiry), `AuthServiceTest` (duplicate email rejected, password is hashed not stored plain).
- Frontend, in a real browser against the local backend: register a user → redirected (no preferences yet, so this will 404 against a not-yet-built endpoint — acceptable at this phase, confirm the JWT is stored and attached on the next call via browser devtools network tab). Login/logout round-trip works; an expired/invalid token correctly bounces to `/login`.

---

## Phase 4 — Onboarding vertical slice

**Status**: Done

**What**: Per specs.md §4.2, §5.2: seed `data/coins.json` (30 curated coins) as a startup-loaded bean, `UserPreferences` entity + migration is already in `V1__init.sql` from Phase 1 (no new migration needed — confirm no schema drift), `PreferenceService` (validates `investorType`/`contentTypes` enums and `interests` against the curated list), `PreferencesController` (`GET /options`, `GET /preferences`, `PUT /preferences`). Frontend: `OnboardingPage` with the 3-step quiz (`AssetsStep`, `InvestorTypeStep`, `ContentStep`), wired to `GET /options` for choices and `PUT /preferences` on submit; the router gate from specs.md §3.2 (`GET /preferences` 404 → force `/onboarding`) becomes real.

**Files/components**: backend `entity/UserPreferences.java`, `repository/UserPreferencesRepository.java`, `service/PreferenceService.java`, `controller/PreferencesController.java`, `dto/{request,response}/Preferences*.java`, `src/main/resources/data/coins.json`. Frontend `features/onboarding/*`, `api/preferencesApi.ts`, `routes/AppRouter.tsx` (add the 404-gate redirect logic).

**Depends on**: Phase 3 (needs an authenticated user to attach preferences to).

**Verify**:
- `curl GET /api/preferences/options` (no auth) → 30 coins + both enum lists.
- Authenticated `curl GET /api/preferences` on a fresh user → `404 PREFERENCES_NOT_SET`. `PUT` with an interest not in the curated list → `400 UNKNOWN_COIN_ID`. `PUT` with valid data → `200`, then `GET` returns it back unchanged.
- Browser: register a new user → automatically routed to `/onboarding` (confirms the 404-gate works) → complete the quiz → submitting redirects to `/dashboard` (which will 404/error at this phase since it doesn't exist yet — expected, confirms routing only).

---

## Phase 5 — Dashboard vertical slice (4 sections + aggregation)

**Status**: Done

**What**: The largest phase; build incrementally, section by section, per specs.md §4.3/§7.1–7.3, verifying each section's service in isolation before wiring the aggregating controller.

1. **Coin Prices**: `CoinGeckoClient` (calls `/simple/price`), `CoinPriceService`. No caching (per spec, always live).
2. **Market News**: `RssNewsClient` (parses Cointelegraph + Decrypt RSS feeds, no key required, field mapping per specs.md §4.3), `NewsService` with the 20-min in-memory cache (Spring `@Cacheable` + Caffeine) and `data/news-fallback.json` fallback on error/timeout.
3. **AI Insight**: `daily_content` repository access, `OpenRouterClient`, `AiInsightService` implementing the try-insert-then-read cache pattern (specs.md §7.2) and the exact prompt template (§7.1), fallback text on failure. *Requires an OpenRouter API key — acquire before this step.*
4. **Meme**: seed `data/memes.json` (25 entries), `MemeService` using the deterministic `hash(userId+date) mod 25` pick, same cache pattern as AI Insight.
5. **Aggregation**: `DashboardController` (`GET /api/dashboard`) calling all 4 services plus a `Feedback` lookup to populate `userVote` on each item (Feedback entity/repository is introduced here as a read dependency; write endpoints come in Phase 6).

Frontend: `DashboardPage` + `CoinPricesSection`, `MarketNewsSection`, `AiInsightSection`, `MemeSection` (read-only rendering, no vote buttons yet — those are Phase 6), `api/dashboardApi.ts`, TanStack Query hook `useDashboard`.

**Files/components**: backend `client/{CoinGeckoClient,RssNewsClient,OpenRouterClient}.java`, `service/{CoinPriceService,NewsService,AiInsightService,MemeService,DashboardService}.java`, `entity/{DailyContent,Feedback}.java` + repositories, `controller/DashboardController.java`, `config/CacheConfig.java`, `data/{memes.json,news-fallback.json}`. Frontend `features/dashboard/*`, `api/dashboardApi.ts`.

**Depends on**: Phase 4 (dashboard needs a user's saved preferences to know which coins/tone to use).

**Verify** (per sub-step, before moving to the next):
1. Coin Prices: unit test or manual call confirms `CoinPriceService` returns correct shape for a known coin id; verify against 2-3 real coin ids.
2. News: first call hits the RSS feeds (confirm via logs/network), second call within 20 min is served from cache (no outbound call — verify via a temporary log line or a cache-hit counter); kill network/point at a bad URL temporarily to confirm the fallback JSON is served instead of a 5xx.
3. AI Insight: first `GET /api/dashboard` call for a user generates and persists a row in `daily_content`; a second call the same day returns the identical cached text (verify via DB query, not just API response, to confirm no duplicate insert — the unique constraint holds); force an API failure (bad key) to confirm the fallback text + `"fallback": true` path, not a 500.
4. Meme: confirm the same user gets the same meme across repeated calls same-day, and (spot-check) a different simulated date/user yields a different pick.
5. Aggregation: full `GET /api/dashboard` returns all 4 sections in the exact shape from specs.md §4.3, with `userVote: null` for everything (no votes exist yet).
- JUnit: `AiInsightServiceTest` and `MemeServiceTest` covering the cache-hit vs cache-miss branches (the "risky logic" specs.md's testing scope calls out).
- Browser: logged-in user with completed onboarding sees a populated dashboard with real prices, real (or fallback) news, an AI-generated insight, and a meme.

**Still open after this phase** (neither blocks Phase 6):
- **Live-API verification**: the suite proves the degraded path (all three integrations failing → still `200` with fallbacks). Confirming the happy path needs a real `OPENROUTER_API_KEY` value and outbound network, i.e. a local run (RSS news needs no key).
- **Meme artwork**: `data/memes.json` ships 25 entries whose `imageUrl`s are generated placeholders. Swapping in real curated images is a content edit to that one file — no code change.

---

## Phase 6 — Feedback voting vertical slice

**Status**: Done

**What**: Per specs.md §4.4: `FeedbackController` (`POST /api/feedback` upsert, `DELETE /api/feedback/{itemType}/{itemRef}`), `FeedbackService` using the `(user_id, item_type, item_ref)` unique constraint for upsert semantics, server-set `item_date`. Frontend: `VoteButtons` component wired into `MarketNewsSection`, `AiInsightSection`, `MemeSection` (not `CoinPricesSection`, per spec), using TanStack Query mutations with optimistic updates against the `useDashboard` cache.

**Files/components**: backend `controller/FeedbackController.java`, `service/FeedbackService.java`, `dto/request/FeedbackRequest.java`. Frontend `components/VoteButtons.tsx`, `api/feedbackApi.ts`, mutation hooks in `features/dashboard/`.

**Depends on**: Phase 5 (voting targets need real `itemRef` values — news article ids, `daily_content.id` — that only exist once the dashboard endpoint is live).

**Verify**:
- `curl POST /api/feedback` with a valid `itemRef` from a live dashboard response → `200`; repeat with a different `vote` value → same `id` returned, row updated not duplicated (confirm via DB query — one row per `(user, item_type, item_ref)`).
- `DELETE` on an existing vote → `204`; `DELETE` on a non-existent vote → `204` (idempotent, not `404`).
- JUnit: `FeedbackServiceTest` covering upsert-on-conflict and delete-when-absent.
- Browser: clicking thumbs up/down on a news item, the AI insight, and the meme visibly updates and persists across a page reload (`GET /api/dashboard` now returns the correct `userVote`).

---

## Phase 7 — CI (test-only GitHub Actions)

**Status**: Done

**What**: Add `.github/workflows/test.yml` running `mvn -B test` on every push/PR to `main` (Java 21 setup, no deploy step — Railway/Netlify's own hooks remain the only deploy trigger, per specs.md §1.1 and the sequencing decision above).

**Files/components**: `.github/workflows/test.yml`.

**Depends on**: Phase 6 (so there's a meaningful test suite for it to run — could technically land earlier, placed here so it validates the full accumulated suite at least once before deployment).

**Verify**: push a branch with a deliberately failing test → Actions run shows red; fix it → green. Confirm the workflow does *not* attempt any deploy action.

---

## Phase 8 — Deployment

**Status**: Not started

**What**: Per specs.md §9. Railway: create project, attach Postgres plugin, set root dir `/backend`, set all env vars from §9.1's table, confirm healthcheck path. Netlify: set base dir `/frontend`, build command `npm run build`, publish dir `frontend/dist`, add `netlify.toml` SPA redirect, set `VITE_API_BASE_URL` to the live Railway URL. Update Railway's `FRONTEND_ORIGIN` to the live Netlify URL (circular dependency — Railway must be deployed first to get a URL for Netlify's env var, then Netlify's URL feeds back into Railway's CORS config, requiring one redeploy of the backend).

**Files/components**: `frontend/netlify.toml`, Railway/Netlify dashboard configuration (not repo files, aside from `netlify.toml`).

**Depends on**: Phase 6 (feature-complete) and Phase 7 (green CI) — deploying broken or untested code is exactly what the vertical-slice ordering was meant to avoid.

**Verify**:
- Push to `main` triggers both a Railway build and a Netlify build automatically (confirm in both dashboards).
- Live Netlify URL: full flow works end-to-end against the live Railway backend — register, onboard, view dashboard with real external API data, vote, reload and see the vote persisted.
- `curl <railway-url>/actuator/health` → `UP`.

---

## Phase 9 — DB access deliverable + final deliverables checklist

**Status**: Not started

**What**: Per specs.md §9.3/§9.4: create the read-only `reviewer` Postgres role on the Railway DB with the exact grants specified, populate `AI_USAGE.md` with a real summary of AI-tool collaboration during this build, and walk the full deliverables checklist.

**Files/components**: `AI_USAGE.md` (content), no code changes.

**Depends on**: Phase 8 (Railway DB must be the live one, not local Docker Postgres).

**Verify**: connect to the Railway DB using the `reviewer` credentials from a plain `psql` client (not your own admin credentials) and confirm `SELECT` works on all 4 tables but any `INSERT`/`UPDATE` is rejected. Re-read specs.md §9.4 checklist top to bottom and confirm every box is genuinely satisfied (repo public, both URLs live, DB credentials ready to hand off, `AI_USAGE.md` populated).
