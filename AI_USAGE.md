# AI Usage

I built this project using Claude Code (Anthropic) as my primary AI collaborator, across
multiple sessions from initial planning through live deployment and post-launch fixes. I
drove the direction throughout — I decided what to build, reviewed what came back, tested it
myself before trusting it, and I'm the one accountable for everything that ended up in this
repo. This is a genuine account of that process, including the places I let AI get things
wrong and had to step in and correct it — not a sanitized summary.

## How I worked

I had Claude Code draft `specs.md` (API contracts, schema, orchestration logic, deployment
plan) and `plan.md` (phased build sequence) with me before any code was written, and I read
and adjusted both — in particular, the assignment brief left real ambiguity (whether the
4 dashboard sections should be gated by content-type preference, whether every section needs
voting, how "dynamic" the meme should be), and I made the calls on how to resolve each one
rather than accepting the first interpretation offered.

I chose to build in vertical slices (Auth → Onboarding → Dashboard → Feedback → CI →
Deployment) specifically so I'd catch integration problems — auth headers, CORS, DTO
mismatches — early, phase by phase, instead of in one large final integration pass. I set the
acceptance bar for each phase myself (curl checks, JUnit tests, or a manual pass in a real
browser) and didn't move on until I'd verified it, not just read a summary claiming it worked.

## Where I caught AI getting it wrong

This is the part worth being specific about, since it's the most informative part of an
AI-collaboration log:

- **CryptoPanic → CryptoCompare, wrong on both counts.** Mid-build, an earlier session of mine
  found CryptoPanic's free tier gone and swapped the design to CryptoCompare (CoinDesk Data)
  on the claim it had a working 100k-calls/month free tier. I hadn't verified that claim myself
  before accepting it, and a later session I ran caught it: I checked both live and found
  CryptoCompare's free tier had *also* been retired (May 2026, confirmed against CoinDesk's own
  blog post, not a third-party summary), and CryptoPanic's status was murkier than the original
  claim suggested too. The lesson I applied afterward: I don't accept a provider's free-tier
  status from an AI's memory or a single search result anymore — I make it verify against the
  vendor's own current docs, or with a real authenticated call, before I let a design decision
  rest on it.
- **Settled on public RSS feeds instead of chasing a fourth vendor.** Rather than keep chasing
  another "free tier" (CoinGecko's own News endpoint turned out to be paid-only too — I had it
  test a real API key against it and it came back with an explicit `PRO API subscribers only`
  error), I decided to switch the design to Cointelegraph + Decrypt RSS feeds — publisher-
  syndicated, no key, no vendor pricing risk to track. I didn't accept this until I'd seen it
  verified live end-to-end — a real Java client run against the real feeds, not just a curl
  check — including confirming Unicode encoding came through correctly, not just that articles
  came back at all.
- **The pinned OpenRouter model was dead too.** `meta-llama/llama-3.1-8b-instruct:free`, the
  model specified in my original spec, returned `404` against a real API key. I had six live
  free-model candidates tested against the actual AI Insight prompt — not "does it respond,"
  the real 3–5-sentence generation task under the real token budget. Several were rate-limited,
  several too slow for the 10s timeout, and one (`nvidia/nemotron-nano-9b-v2:free`) looked fine
  on a short test but turned out to be a reasoning model whose hidden chain-of-thought consumed
  the entire `max_tokens` budget on a real prompt, truncating the visible output mid-sentence
  (`finish_reason: "length"`) — a failure mode I'd have missed if I'd only checked for a `200`
  and not looked at `finish_reason`. I settled on `poolside/laguna-xs-2.1:free`, the one that
  actually held up, and raised `max_tokens` to give its reasoning overhead room.

## Deployment — I verified the live system myself, not the dashboards' word for it

I configured Railway (backend) and Netlify (frontend) myself through their dashboards, and I
insisted on verifying each step against the live URLs rather than trusting a "deployed
successfully" message:

- I caught a real secret before it reached git: an OpenRouter API key had ended up pasted into
  the tracked `backend/.env.example` template instead of the git-ignored `backend/.env` — I
  caught it in `git diff` before committing, moved it to the correct file, and reverted the
  tracked one.
- I caught that the dead `meta-llama/llama-3.1-8b-instruct:free` model (above) would have
  silently degraded every AI Insight to fallback text in production if I hadn't checked it
  pre-deploy.
- I caught a placeholder-URL bug in the live Netlify deploy: the literal example text
  `https://<your-railway-url-from-part-1>` had been pasted into `VITE_API_BASE_URL` instead of
  the real URL. I found it by fetching the deployed JS bundle myself and grepping for the
  baked-in `baseURL` value — it wasn't visible just from the page loading fine, since Vite
  bakes env vars in at build time and the static shell renders regardless of whether the API
  calls actually work.
- I diagnosed a CORS `403` after fixing the Netlify env var by checking `CorsConfig.java`'s
  exact-match behavior myself and confirming the real cause was a timing issue — Railway's
  `FRONTEND_ORIGIN` variable had been updated but the service hadn't finished redeploying, and
  Spring reads that value once at boot, not live.
- I verified the full flow against the *live* deployment (not localhost) after every fix:
  register → set preferences → dashboard with real external data → vote → reload with the vote
  persisted — confirmed with direct `curl` calls carrying the real `Origin` header myself, not
  assumed from a dashboard saying "deployed successfully."

## Post-launch: I went back and re-checked against the actual assignment brief

After the initial build was live, I asked Claude Code to do a deep compliance pass — not just
against `specs.md` (which I'd already written and could be wrong in the same ways I was), but
against the original assignment PDF itself, plus real manual testing (booting the real stack,
hitting every endpoint, driving the actual UI in a browser). I wanted an independent check
against the source document, not just my own derived spec agreeing with itself.

That pass surfaced a real bug I'd missed: the JWT was only kept in memory client-side, so any
page reload silently logged a user out even though the token was still valid server-side for
24h. I reviewed the proposed fix, tested it myself in a real browser (reload keeps the session,
a second tab shares it, logout still works correctly) before I approved pushing it.

The same review also surfaced two places where my build had quietly resolved assignment
ambiguity in a way I wasn't fully comfortable with once I saw the literal wording again: the
meme was cached once per day instead of being "shown dynamically each time the dashboard
updates" as the brief states, and the section title in the UI ("Fun Crypto Meme") was
redundant next to the caption already shown. I made the call on both — changed the meme to be
picked fresh on every dashboard load (verified live: distinct memes across repeated reloads)
and removed the redundant title — and had the accompanying tests rewritten to match the new
behavior rather than leaving stale assertions in place.

## Takeaway — and where I land on responsibility

The recurring pattern worth naming: several of this build's real bugs came from *unverified*
claims an earlier session of mine had accepted too easily — a dead free tier assumed still
free, a pinned model assumed still live, a config value assumed correct because it "looked"
filled in, a session-persistence assumption I hadn't actually tested by hand. Every one of them
was caught only when I made a later session actually test the live system — a real API call, a
real deployed bundle, a real cross-origin request, a real browser reload — rather than trusting
documentation, memory, or a green checkmark in a dashboard UI.

I used Claude Code to generate most of the code in this repo, but I reviewed the diffs, ran the
tests, and did the manual verification myself before anything shipped. I take responsibility
for what's in this repository — AI-assisted or not.
