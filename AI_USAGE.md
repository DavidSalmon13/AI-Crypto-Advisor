# AI Usage

This project was built with Claude Code (Anthropic) across multiple sessions, from initial
planning through live deployment. This is a genuine account of how it was used, including
where the AI got things wrong and had to be corrected — not a sanitized summary.

## Planning

`specs.md` (full technical spec — API contracts, schema, orchestration logic, deployment plan)
and `plan.md` (phased build sequence) were drafted collaboratively with Claude Code before any
code was written. The plan deliberately used vertical slices (Auth → Onboarding → Dashboard →
Feedback → CI → Deployment) so each phase shipped something demoable and integration problems
(auth headers, CORS, DTO mismatches) would surface early instead of in one large final pass.

## Implementation (Phases 0-7)

Backend (Spring Boot 3.3, Java 21) and frontend (React 18 + Vite + TypeScript) were built
phase-by-phase by Claude Code, following the layering and conventions fixed in `CLAUDE.md`
(controller → service → repository, DTOs never expose entities, error envelope, etc.). Each
phase was verified against its spec'd acceptance criteria (curl checks, JUnit tests, or a
manual browser pass) before moving to the next.

## Where AI-driven decisions were wrong, and how that was caught

This is the part worth being specific about, since it's the most informative part of an
AI-collaboration log:

- **CryptoPanic → CryptoCompare (docs-only swap, wrong on both counts).** Mid-build, a prior
  session found CryptoPanic's free tier gone and swapped the design to CryptoCompare
  (CoinDesk Data) on the claim that it had a working 100k-calls/month free tier. Neither claim
  was actually verified — a later session checked both live and found CryptoCompare's free
  tier had *also* been retired (May 2026, confirmed via CoinDesk's own blog post, not a
  third-party summary), and CryptoPanic's status was murkier than the original claim suggested
  too. Lesson applied afterward: don't trust a provider's free-tier status from memory or a
  single search result — verify against the vendor's own current documentation, or by making
  a real authenticated call, before committing to a design decision around it.
- **News provider: settled on public RSS feeds, not a fourth vendor.** Rather than keep
  chasing another "free tier" (CoinGecko's News endpoint turned out to be paid-only too,
  confirmed by testing a real API key against it and getting an explicit
  `PRO API subscribers only` error), the design switched to Cointelegraph + Decrypt RSS feeds
  — publisher-syndicated, no key, no vendor pricing risk. This was verified live end-to-end
  (a real Java client run against the real feeds, not just curl) before being committed,
  including checking actual Unicode encoding correctness, not just that articles came back.
- **The pinned OpenRouter model was also dead.** `meta-llama/llama-3.1-8b-instruct:free`,
  specified in the original spec, returned `404` against a real API key. Six live free-model
  candidates were tested against the real AI Insight prompt (not just "does it respond" — the
  actual 3-5-sentence generation task with the real token budget). Several were rate-limited,
  several were too slow for the 10s timeout, and one (`nvidia/nemotron-nano-9b-v2:free`) looked
  fine on a short test but turned out to be a reasoning model whose hidden chain-of-thought
  consumed the entire `max_tokens` budget on a real prompt, truncating the actual visible
  output mid-sentence (`finish_reason: "length"`) — a failure mode invisible unless you check
  `finish_reason` and not just whether the call returned `200`. `poolside/laguna-xs-2.1:free`
  was the model that actually held up under the real prompt, with `max_tokens` raised to give
  its own reasoning overhead room.

## Deployment (Phase 8)

Railway (backend) and Netlify (frontend) were configured through their dashboards (no CLI
access from the assistant side) with the assistant providing exact values and verifying each
step against the live URLs rather than trusting "should be good now":

- Caught a real secret before it reached git: an OpenRouter API key had been pasted into the
  tracked `backend/.env.example` template instead of the git-ignored `backend/.env` — found via
  `git diff` before committing, moved to the correct file, reverted the tracked one.
- Caught that `meta-llama/llama-3.1-8b-instruct:free` being dead (see above) would have
  silently degraded every AI Insight to fallback text in production if not caught pre-deploy.
- Caught a placeholder-URL bug in the live Netlify deploy: the literal example text
  `https://<your-railway-url-from-part-1>` had been pasted into `VITE_API_BASE_URL` instead of
  the real URL. Found by fetching the deployed JS bundle directly and grepping for the baked-in
  `baseURL` value — not visible from the page loading fine, since Vite bakes env vars in at
  build time and the static shell renders regardless.
- Diagnosed a CORS `403` after the Netlify env fix by checking `CorsConfig.java`'s exact-match
  behavior and confirming the timing issue (Railway's `FRONTEND_ORIGIN` variable had been
  updated but the service hadn't finished redeploying — Spring reads that value once at boot,
  not live).
- Verified the full flow against the *live* deployment (not localhost) after each fix: register
  → set preferences → dashboard with real external data → vote → reload with the vote
  persisted — confirmed via direct `curl` calls with the real `Origin` header, not assumed from
  the dashboards saying "deployed successfully."

## Takeaway

The recurring pattern worth naming: several of this build's real bugs were introduced by an
earlier AI session's *unverified* claims (a dead free tier assumed still free, a pinned model
assumed still live, a config value assumed correct because it "looked" filled in). All of them
were caught by a later session actually testing the live system — a real API call, a real
deployed bundle, a real cross-origin request — rather than trusting documentation, memory, or
a green checkmark in a dashboard UI.
