# CLAUDE.md

Guidance for Claude Code (and any other agent) working in this repository.

## What this project is

**AI Crypto Advisor** — a personalized crypto investor dashboard, built as a coding take-home (Moveo, WEB track). A user registers, completes a one-time onboarding quiz (crypto assets of interest, investor type, preferred content style), and lands on a **Daily Dashboard** with four sections: Coin Prices, Market News, AI Insight of the Day, and a Fun Crypto Meme. Every content item (news, AI insight, meme) can be voted 👍/👎; votes are persisted for a future recommendation model (design only — not implemented, see specs.md §7.4).

Full technical specification: **`specs.md`**. Phased build plan and current build status: **`plan.md`**. This file covers the stable facts an agent needs before touching code — not task sequencing, which lives in `plan.md`.

## Tech stack

| Layer | Choice |
|---|---|
| Frontend | React 18 + Vite 5 + TypeScript, Tailwind CSS, TanStack Query, React Router v6 |
| Backend | Java 21 + Spring Boot 3.3.x (Web, Security, Validation, Data JPA) |
| Database | PostgreSQL 16 (Railway-managed in prod, Docker Compose locally) |
| Migrations | Flyway |
| Auth | JWT (HS256, 24h expiry), Spring Security — stateless, no sessions, no refresh tokens |
| AI | OpenRouter (free-tier model, default `meta-llama/llama-3.1-8b-instruct:free`) |
| Market data | CoinGecko public API |
| News | CryptoCompare News API (CoinDesk Data) free tier + static JSON fallback on failure |
| Memes | Static curated JSON (no live scraping) |
| Backend host | Railway (root dir `/backend`) |
| Frontend host | Netlify (base dir `/frontend`) |
| CI | GitHub Actions — test-only (`mvn test`), separate from deploy |
| Deploy trigger | Railway + Netlify native "deploy on push to `main`" — no deploy step in GitHub Actions |

## Architecture

Monorepo, two independently deployed apps talking over stateless REST/JSON (`Authorization: Bearer <jwt>` header, no cookies):

```
AI-Crypto-Advisor/
├── backend/     Spring Boot app (Maven)
├── frontend/    React app (Vite)
├── specs.md     full technical spec (source of truth for contracts/schema)
├── plan.md      phased build plan
└── CLAUDE.md    this file
```

**Backend layering**: `controller → service → repository`, plus `client/` for outbound HTTP integrations (CoinGecko, CryptoCompare, OpenRouter) and `security/` for JWT. Entities are never serialized directly — every endpoint has a request/response DTO in `dto/`.

**Key architectural decisions** (see specs.md for full rationale):
- All 4 dashboard sections always render for every user — onboarding's `content_types` preference biases tone/framing (e.g. the AI Insight prompt), it does not hide sections.
- "Daily" content (AI Insight, Meme) is generated **lazily on first request per user per UTC day** and cached in `daily_content` — not a scheduled/cron job, not regenerated on every page load.
- Coin Prices and Market News are fetched live on every dashboard request (Coin Prices: no caching, needs to be current; Market News: 20-minute server-wide in-memory cache, not per-user).
- External API failures degrade gracefully, never surface as a 5xx to the client: News falls back to a static JSON file; AI Insight falls back to canned text with a `fallback: true` flag.
- All DB primary keys are `UUID` (not sequential ids) to avoid exposing enumerable identifiers via the API or JWT.

## Conventions

### Backend (`backend/src/main/java/com/moveo/aicryptoadvisor/`)

```
config/       SecurityConfig, CorsConfig, CacheConfig, RestClientConfig
security/     JwtService, JwtAuthFilter
controller/   one per resource — AuthController, PreferencesController, DashboardController, FeedbackController
service/      business logic — one per concern, e.g. AiInsightService, MemeService, FeedbackService
client/       outbound HTTP wrappers — CoinGeckoClient, CryptoCompareClient, OpenRouterClient
repository/   Spring Data JPA repositories
entity/       JPA entities — never returned from controllers directly
dto/          request/ and response/ subpackages
exception/    GlobalExceptionHandler + ApiException subtypes
```

- REST base path: `/api`. All enum values over the wire are `UPPER_SNAKE_CASE` (e.g. `HODLER`, `MARKET_NEWS`).
- Every non-2xx response uses the shared error envelope: `{ "error": "CODE", "message": "...", "fieldErrors": {} }` — see specs.md §4.5 for the full status-code mapping. Don't invent ad hoc error shapes per endpoint.
- Bean validation (`@Valid` + `jakarta.validation` annotations) at the DTO layer; business-rule validation (e.g. "coin id must be in the curated list") in the service layer, raising a typed `ApiException`.
- Static reference data (curated coin list, meme pool, news fallback) lives in `src/main/resources/data/*.json`, loaded into in-memory beans at startup — not DB tables, since it's read-only and only changes via a code deploy.

### Frontend (`frontend/src/`)

```
api/          thin wrappers over apiClient, one file per resource — authApi.ts, dashboardApi.ts, etc.
lib/          apiClient.ts (axios + JWT interceptor), queryClient.ts (TanStack Query)
features/     one folder per feature area — auth/, onboarding/, dashboard/ — pages + feature-local components/hooks
components/   shared, feature-agnostic UI — Button, Card, Spinner, ProtectedRoute
types/        TypeScript types mirroring backend DTOs field-for-field
routes/       AppRouter.tsx
```

- Server state (API data) goes through TanStack Query — don't hand-roll fetch + `useState`/`useEffect` for anything that hits the backend. Auth state lives in a lightweight `AuthContext`, nothing heavier (no Redux).
- Styling is Tailwind utility classes; no separate CSS-in-JS or component library.
- Route guarding is centralized in `AppRouter.tsx`/`ProtectedRoute.tsx`, not scattered `if (!user)` checks inside individual pages.

### Testing

- **Backend**: light, targeted JUnit tests — not full coverage. Priority order: JWT generation/validation, the daily-cache-or-generate logic (`AiInsightService`/`MemeService` cache-hit vs cache-miss branches), feedback upsert-on-conflict semantics, plus one Spring Boot integration test hitting a real endpoint. Run with `mvn test`.
- **Frontend**: no automated tests — deliberately out of scope to conserve assignment time (matches specs.md's testing-scope decision). Verify frontend changes by running the dev server and exercising the flow in a browser.
- **CI**: `.github/workflows/test.yml` runs `mvn -B test` on every push/PR. It does not deploy — deploys are handled natively by Railway/Netlify on push to `main`.

### Secrets / environment

- Backend: `backend/.env` (git-ignored), documented in `backend/.env.example`. Required keys: `DATABASE_URL`, `JWT_SECRET`, `OPENROUTER_API_KEY`, `OPENROUTER_MODEL`, `CRYPTOCOMPARE_API_KEY`, `FRONTEND_ORIGIN`.
- Frontend: `frontend/.env.local` (git-ignored, Vite convention), documented in `frontend/.env.example`. Required key: `VITE_API_BASE_URL`.
- Never commit real keys/secrets; only `.env.example` files are tracked.

## Build / run / test

### Backend

```bash
cd backend
docker compose up -d          # local Postgres 16 on :5432
mvn spring-boot:run            # boots the app, Flyway migrations run automatically
mvn test                       # run the JUnit suite
```
Health check: `curl localhost:8080/actuator/health` → `{"status":"UP"}`.

### Frontend

```bash
cd frontend
npm install
npm run dev                    # dev server, expects backend at VITE_API_BASE_URL
npm run build                  # production build to frontend/dist
```

### Full local stack

Run the backend (with Docker Postgres) and frontend dev server in parallel, in two terminals, from the commands above. There is no single root-level "run everything" script.
