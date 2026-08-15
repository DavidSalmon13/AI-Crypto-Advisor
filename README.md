# AI Crypto Advisor

A personalized crypto investor dashboard, built as a coding take-home (Moveo, WEB track).
A user registers, completes a one-time onboarding quiz (crypto assets of interest, investor
type, preferred content style), and lands on a **Daily Dashboard** with four sections: Coin
Prices, Market News, AI Insight of the Day, and a Fun Crypto Meme. Every content item can be
voted 👍/👎; votes are persisted for a future recommendation model (design only, not
implemented — see [`specs.md` §7.4](specs.md)).

## Live app

- **Frontend:** https://stupendous-sunshine-b08de9.netlify.app
- **Backend:** https://ai-crypto-advisor-production-24cc.up.railway.app (health check: `/actuator/health`)

## Tech stack

| Layer | Choice |
|---|---|
| Frontend | React 18 + Vite 5 + TypeScript, Tailwind CSS, TanStack Query, React Router v6 |
| Backend | Java 21 + Spring Boot 3.3.x (Web, Security, Validation, Data JPA) |
| Database | PostgreSQL 16 (Railway-managed in prod, Docker Compose locally) |
| Migrations | Flyway |
| Auth | JWT (HS256, 24h expiry), Spring Security — stateless, no sessions |
| AI | OpenRouter (free-tier model) |
| Market data | CoinGecko public API |
| News | Publisher RSS feeds (Cointelegraph, Decrypt) + static JSON fallback |
| Memes | Reddit (r/CryptoCurrencyMemes, no key) + static curated JSON fallback |
| CI | GitHub Actions — `mvn test` on every push/PR |
| Deploy | Railway (backend) + Netlify (frontend), both deploy on push to `main` |

## Repo structure

```
AI-Crypto-Advisor/
├── backend/     Spring Boot app (Maven)
├── frontend/    React app (Vite)
├── specs.md     full technical spec — source of truth for API contracts and DB schema
├── plan.md      phased build plan and status
├── AI_USAGE.md  summary of AI-tool collaboration during this build
└── CLAUDE.md    conventions/guidance for AI coding agents working in this repo
```

## Running locally

**Backend** (needs Docker for local Postgres):
```bash
cd backend
docker compose up -d          # Postgres 16 on :5432
cp .env.example .env          # fill in OPENROUTER_API_KEY (only external key needed)
mvn spring-boot:run           # Flyway migrations run automatically
mvn test                      # run the test suite
```
Health check: `curl localhost:8080/actuator/health` → `{"status":"UP"}`

**Frontend:**
```bash
cd frontend
npm install
cp .env.example .env.local
npm run dev                   # expects backend at VITE_API_BASE_URL
```

Run both in parallel (two terminals) for the full local stack — there's no single
root-level "run everything" script.

## Documentation

- [`specs.md`](specs.md) — full technical spec: API contracts, DB schema, orchestration
  logic, deployment plan, and the bonus write-up on using feedback for a future
  recommendation model (§7.4)
- [`plan.md`](plan.md) — phased build plan with current status per phase
- [`AI_USAGE.md`](AI_USAGE.md) — how AI tools were used during this build, including where
  earlier AI-driven decisions were wrong and how later verification caught them
