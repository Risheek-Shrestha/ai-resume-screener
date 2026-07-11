# Architecture

This document goes deeper than the README into how the system is put together and why.

## System overview

```
                              ┌──────────────────────────┐
                              │   React + TS Frontend     │
                              │   (Vercel)                 │
                              └────────────┬──────────────┘
                                           │ HTTPS, JWT bearer
                                           ▼
┌──────────────────┐        ┌───────────────────┐        ┌──────────────────┐
│   PostgreSQL 15   │◄──────►│  Spring Boot API   │──────►│  FastAPI ML svc  │
│  (Render managed) │  JDBC  │  (Render, Docker)   │ HTTP  │ (Render, Docker) │
└──────────────────┘        └─────────┬─────────┘        └──────────────────┘
                                       │  ▲
                             AMQP/TLS  │  │
                                       ▼  │
                             ┌─────────────────┐
                             │    RabbitMQ      │
                             │   (CloudAMQP)     │
                             └─────────────────┘
```

Four independently deployable pieces:

- **Frontend** (Vercel) — stateless SPA, talks to the API over HTTPS with a JWT bearer token.
- **Spring Boot API** (Render) — the only service the frontend or any external client talks to. Owns auth, jobs, resumes, scores, suggestions, applications, and PDF reports.
- **FastAPI ML microservice** (Render) — stateless, internal-only. Does the actual NLP work: text extraction, scoring, suggestion generation, job ranking. Not exposed to end users; the Spring Boot API is its only caller.
- **RabbitMQ** (CloudAMQP) — decouples "an application status changed" from "send the email." The API publishes an event and returns immediately; a listener picks it up and sends the notification asynchronously.

## Why a separate ML service instead of doing NLP in Java

Two reasons:

1. **Library fit.** `scikit-learn`, `sentence-transformers`, and PDF/DOCX text extraction all have mature, well-supported Python libraries. Replicating sentence-transformer embeddings in Java would mean either a much thinner feature set or fighting the ecosystem.
2. **Failure isolation.** If the ML service goes down or times out, the API degrades to keyword-only matching (see below) instead of failing the request outright. Keeping it as a separate process makes that fallback a clean HTTP-call-fails branch rather than an in-process exception from a library embedded in the main app.

The tradeoff is an extra network hop and an extra service to deploy/monitor — acceptable here since resume scoring isn't on the hot path for every request, only for score generation and application submission.

## Scoring pipeline

When a resume is scored against a job, three signals are blended:

1. **Keyword coverage** — the job's required skills are checked against the resume's extracted text, with synonym normalization (`js` ↔ `javascript`, `k8s` ↔ `kubernetes`, `jpa` ↔ `hibernate`, etc.) so near-misses in phrasing don't tank the score.
2. **TF-IDF similarity** — term-frequency weighted overlap between resume and job description text, catching relevant vocabulary beyond the explicit skill list.
3. **Semantic similarity** — sentence-transformer embeddings compare the *meaning* of resume and job text, catching relevant experience described in different words than the job posting uses.

The three are combined into a single weighted score, plus a breakdown (matched keywords, missing keywords, weak areas) that feeds the suggestions endpoint.

**Graceful degradation:** if the ML service is unreachable, the API catches the failed HTTP call and falls back to keyword-only matching rather than failing the scoring request. The user still gets a result, just a less nuanced one.

## Application lifecycle

```
APPLIED ──► SHORTLISTED ──► HIRED
   │              │
   └──────────────┴──► REJECTED
```

- Applying is **auto-scored and auto-decided**: the ML score determines whether the application starts as `APPLIED` (passed the bar) or is immediately `REJECTED` (didn't). This mirrors how real ATS pre-screening works — not every application reaches a human reviewer.
- `HIRED` and `REJECTED` are terminal — any further transition attempt returns `400 Bad Request`. Transition validity is enforced in the service layer, not just the database, so invalid transitions fail fast with a clear error rather than silently corrupting state.
- Every valid transition publishes a RabbitMQ event, which triggers an email to the applicant. This is fire-and-forget from the API's perspective — a slow or failing mail send doesn't block the status update response.

## Auth

- **Access tokens**: short-lived JWTs (15 min default), stateless — validated per-request via a custom `JwtAuthFilter`, no session store.
- **Refresh tokens**: longer-lived (7 days), stored server-side in Postgres so they can be revoked (logout invalidates the specific token; a user's tokens are cleared on password reset).
- **Why not just long-lived access tokens?** Short-lived access tokens limit the blast radius if one is intercepted. The refresh token trade-off moves the "how long until an attacker is locked out" question to a token that's revocable server-side, instead of a token that's valid until it naturally expires.
- CORS is scoped to a single configured frontend origin (`APP_FRONTEND_URL`), not `*`, since credentials (the Authorization header) are involved.

## Caching

Job listing and detail endpoints are cached with Redis (`@Cacheable`), invalidated on any create/update/delete (`@CacheEvict(allEntries = true)`). If Redis is unavailable, the app falls back to an in-memory `ConcurrentMapCacheManager` via a `@ConditionalOnMissingBean` bean — caching degrades to per-instance rather than shared, but the app doesn't fail to start or throw on every request.

## Data model notes

- Resume files are stored as `bytea` in Postgres directly (not `@Lob`, which caused OID/large-object stream errors with this driver/dialect combination) — simpler ops story than wiring up S3-equivalent storage for a project this size, at the cost of larger row sizes and no CDN-backed serving.
- Matched/missing keywords on a `Score` are stored as JSONB rather than a join table — they're read as a unit (never queried by individual keyword) and don't need relational integrity, so JSONB avoids unnecessary joins.
- `Application` keeps a full history rather than one row per (user, job) — a user can be rejected and reapply, and the system needs to reason about "the most recent application for this job," not just "the only one."

## Testing strategy

- **Repository layer**: Testcontainers spins up a real PostgreSQL instance per test run rather than mocking the database or using H2 — catches real SQL/dialect issues that an in-memory substitute would miss.
- **Service layer**: Mockito-based unit tests, including the ML-unreachable fallback path and status-transition validation.
- **Controller layer**: `@WebMvcTest` + `@Import(SecurityConfig.class)` so security rules (role checks, unauthenticated/forbidden responses) are exercised as part of the same test, not verified separately.

## Deployment topology

Each piece is deployed independently and communicates over the public internet (Render's internal networking is used where both services are on Render, i.e. API ↔ Postgres and API ↔ ML service):

| Piece | Where | Notes |
|---|---|---|
| Frontend | Vercel | Static build, SPA fallback rewrite for client-side routing |
| API | Render (Docker) | Free tier — spins down on inactivity |
| ML service | Render (Docker) | Free tier — spins down on inactivity |
| Postgres | Render (managed) | Internal connection string used by the API |
| RabbitMQ | CloudAMQP | TLS required (`SPRING_RABBITMQ_SSL_ENABLED=true`, port `5671`) |

All environment-specific configuration is injected via environment variables at the platform level (`APP_FRONTEND_URL`, `SPRING_DATASOURCE_URL`, `JWT_SECRET`, mail and RabbitMQ credentials, etc.) — nothing environment-specific is hardcoded in the committed `application.properties`.
