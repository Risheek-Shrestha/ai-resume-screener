# AI Resume Screener

An AI-assisted resume screening platform. Recruiters post jobs, candidates upload resumes, and the system automatically extracts resume text, scores it against the job using keyword + TF-IDF + semantic similarity, and returns actionable improvement suggestions, job recommendations, and a downloadable PDF report. Status change notifications are delivered asynchronously via RabbitMQ.

## Architecture

Four services, wired together over a Docker network:

```
┌──────────────────┐        ┌───────────────────┐        ┌──────────────────┐
│   PostgreSQL 15   │◄──────►│  Spring Boot API   │──────►│  FastAPI ML svc  │
│  (jobs, resumes,  │  JDBC  │  (Java 21, :8081)  │ HTTP  │ (Python, :8000)  │
│  scores, users)   │        │                    │       │                  │
└──────────────────┘        └───────────────────┘        └──────────────────┘
                                       │  ▲
                             AMQP      │  │ JWT-authenticated REST
                                       ▼  │
                             ┌─────────────────┐       Client / Swagger UI
                             │    RabbitMQ      │
                             │  (:5672/:15672)  │
                             └─────────────────┘
```

- **Spring Boot API** — owns auth, jobs, resumes, scores, suggestions, applications, and PDF reports. Calls the ML service over HTTP for NLP tasks. Publishes application status change events to RabbitMQ.
- **FastAPI ML microservice** — stateless. Extracts text from PDF/DOCX, scores a resume against a job (keyword matching with synonym normalization + TF-IDF + sentence-transformer semantic similarity), generates improvement suggestions, and ranks candidate jobs against a resume.
- **PostgreSQL** — persists users, jobs, job skills, resumes (raw file bytes + parsed text), scores (matched/missing keywords stored as JSONB), and applications.
- **RabbitMQ** — brokers email notifications. When an application status changes (applied, shortlisted, hired, rejected), the API publishes an event to `applicationNotificationsExchange` which routes to the `applicationNotifications` queue and triggers an email to the applicant.

## Tech stack

| Layer | Technology |
|---|---|
| API | Java 21, Spring Boot 4.1, Spring Data JPA, Spring Security, JJWT |
| Messaging | RabbitMQ, Spring AMQP |
| ML service | Python, FastAPI, scikit-learn, sentence-transformers, pdfplumber, docx2txt |
| Database | PostgreSQL 15 |
| Caching | Redis (falls back to `ConcurrentMapCacheManager` if Redis is unavailable) |
| Docs | springdoc-openapi (Swagger UI) |
| Reports | Apache PDFBox |
| Infra | Docker, Docker Compose |

## Features

- JWT auth with access + refresh tokens (register, login, refresh, revoke)
- Job CRUD with per-job skill lists and an application window (`applicationStartsAt` / `applicationDeadline`)
- Resume upload (PDF/DOCX) with automatic text extraction via ML service
- AI match score: weighted blend of keyword coverage, TF-IDF content similarity, and semantic similarity
- Skill synonym normalization (e.g. `js` ↔ `javascript`, `k8s` ↔ `kubernetes`, `jpa` ↔ `hibernate`)
- Improvement suggestions: weak-area detection, actionable steps, learning resources, and resume-writing tips
- Job recommendations: ranks open jobs against a candidate's resume
- ATS-style application screening: applying auto-scores the candidate and auto-accepts (`APPLIED`) or auto-rejects (`REJECTED`) on the spot, returning improvement suggestions alongside the result
- Employer application management: view all applicants for a job, view accepted applicants ranked by score, and move applications through a status workflow with transition validation
- **Email notifications via RabbitMQ**: applicants receive an email whenever their application status changes (applied, shortlisted, hired, or rejected)
- Downloadable PDF score report
- Redis caching on job listing/detail endpoints, invalidated on create/update/delete; graceful in-memory fallback when Redis is unavailable
- Graceful ML degradation: if the ML service is unreachable, the API falls back to keyword-only matching instead of failing the request

## Application status workflow

```
APPLIED ──► SHORTLISTED ──► HIRED
   │              │
   └──────────────┴──► REJECTED
```

`HIRED` and `REJECTED` are terminal states — any further transition attempt returns `400 Bad Request`. An email notification is sent to the applicant on every valid transition.

## API reference

All endpoints are prefixed `/api/v1`. Full interactive docs are at `/swagger-ui/index.html` once the API is running.

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/users/register` | — | Create an account |
| POST | `/auth/login` | — | Get access + refresh tokens |
| POST | `/auth/refresh` | — | Exchange a refresh token for a new access token |
| POST | `/auth/revoke` | — | Log out / invalidate a refresh token |
| POST | `/auth/forgot-password` | — | Request a password reset email |
| POST | `/auth/reset-password` | — | Reset password with a valid reset token |
| GET | `/users/me` | USER | Get the current user's profile |
| PUT | `/users/me` | USER | Update the current user's profile |
| POST | `/jobs` | ADMIN | Create a job posting |
| GET | `/jobs/{id}` | — | Get a job by ID |
| GET | `/jobs` | — | List all jobs (paginated) |
| GET | `/jobs/open` | USER | List open jobs not yet applied to by the current user (paginated) |
| PUT | `/jobs/{id}` | ADMIN | Update a job |
| DELETE | `/jobs/{id}` | ADMIN | Delete a job |
| POST | `/resumes` | USER | Upload a resume for a job |
| GET | `/resumes` | USER | List your resumes |
| GET | `/resumes/{id}` | USER | Get a resume by ID |
| PUT | `/resumes/{id}` | USER | Replace a resume |
| DELETE | `/resumes/{id}` | USER | Delete a resume |
| POST | `/scores` | USER | Generate an AI match score for a resume/job pair |
| GET | `/scores/resume/{resumeId}/job/{jobId}` | USER | Get the AI match score for a resume against a job |
| GET | `/scores/my-scores` | USER | List all your scores |
| GET | `/suggestions/improve/{resumeId}/job/{jobId}` | — | Get improvement suggestions for a resume |
| GET | `/suggestions/jobs/{resumeId}/job/{jobId}` | — | Get recommended jobs for a resume |
| POST | `/applications/jobs/{jobId}` | USER | Apply to a job — auto-scored and auto-accepted/rejected |
| GET | `/applications/me` | USER | List your own applications |
| GET | `/applications/jobs/{jobId}` | ADMIN | List all applicants for a job *(job owner only)* |
| GET | `/applications/jobs/{jobId}/accepted` | ADMIN | List accepted applicants ranked by score *(job owner only)* |
| PATCH | `/applications/{id}/status` | ADMIN | Move an application to `SHORTLISTED`, `HIRED`, or `REJECTED` |
| GET | `/reports/resume/{resumeId}/job/{jobId}` | USER | Get a structured score report |
| GET | `/reports/resume/{resumeId}/job/{jobId}/pdf` | USER | Download the report as a PDF |
| GET | `/educations` | USER | List your education records |
| GET | `/educations/{id}` | USER | Get an education record by ID |
| POST | `/educations` | USER | Add an education record |
| PUT | `/educations/{id}` | USER | Update an education record |
| DELETE | `/educations/{id}` | USER | Delete an education record |
| GET | `/courses` | — | List all courses |
| GET | `/courses/{id}` | — | Get a course by ID |
| POST | `/courses` | ADMIN | Create a course |
| PUT | `/courses/{id}` | ADMIN | Update a course |
| DELETE | `/courses/{id}` | ADMIN | Delete a course *(rejected if referenced by a user or job)* |
| GET | `/notifications` | — | List your notifications (paginated) |
| GET | `/notifications/unread-count` | — | Get your unread notification count |
| PUT | `/notifications/{id}/read` | — | Mark a notification as read |
| PUT | `/notifications/read-all` | — | Mark all notifications as read |
| POST | `/admin/users` | ADMIN | Create an admin account |

The FastAPI ML service endpoints (`/extract-text`, `/analyze`, `/suggest`, `/match-jobs`) are internal — the Spring Boot API is the only intended caller and they are not exposed to end users.

## Running with Docker

**Prerequisites:** Docker and Docker Compose.

```bash
git clone https://github.com/Risheek-Shrestha/ai-resume-screener.git
cd ai-resume-screener
cp .env.example .env
```

Edit `.env` and set a real `JWT_SECRET` (generate one with `openssl rand -base64 32`) and a `POSTGRES_PASSWORD`.

```bash
docker compose up --build
```

This starts four containers: `postgres`, `rabbitmq`, `ml-service`, and `resume-screener`. The first build takes a few minutes (the ML image bakes in the sentence-transformer model). Once up:

| Service | URL |
|---|---|
| API | `http://localhost:8081` |
| Swagger UI | `http://localhost:8081/swagger-ui/index.html` |
| RabbitMQ Management | `http://localhost:15672` (guest / guest) |
| ML service (internal) | `http://localhost:8000` |

## Running locally without Docker

**Prerequisites:** Java 21, Maven, Python 3.11+, a local PostgreSQL instance, a local RabbitMQ instance.

**Database**
```sql
CREATE DATABASE resume_screener;
```

**RabbitMQ** (if not already running)
```bash
# macOS
brew install rabbitmq && brew services start rabbitmq

# Ubuntu
sudo apt install rabbitmq-server && sudo systemctl start rabbitmq-server
```

**ML service**
```bash
cd ml-service
pip install -r requirements.txt
uvicorn main:app --reload --port 8000
```

**Spring Boot API**
```bash
cd resume-screener
cp src/main/resources/application.properties.example src/main/resources/application.properties
# Edit application.properties: datasource credentials, jwt.secret, jwt.expiration,
# spring.mail.* (for email notifications), spring.rabbitmq.* if non-default
./mvnw spring-boot:run
```

`ml.service.url` defaults to `http://localhost:8000` in `application.properties.example`, which is correct for local runs.

## Tests

The Spring Boot test suite covers 28 test classes and ~250 tests across all layers. The FastAPI ML microservice does not yet have automated test coverage.

| Layer | Classes | What's covered |
|---|---|---|
| Controllers | 15 | MockMvc slice tests for all endpoints, security rules (role checks, unauthenticated/forbidden access), and error response mapping |
| Services | 12 | Unit tests for all business logic, ownership checks, ML fallback paths, and status transitions |
| Repositories | 12 | Testcontainers (PostgreSQL) integration tests for all custom queries, including empty-result and not-found cases |
| Exception handling | 1 | Parameterized coverage of every custom exception → HTTP status/body mapping in the global exception handler |
| JWT | 2 | Token generation, validation, expiry, tampering, and the auth filter |
| Scheduler | 1 | Job-open notification scheduling |
| Listener | 1 | RabbitMQ notification listener delegation, including the user-not-found branch |
| Validation | 1 | Education date range validation |
| Specification | 1 | Dynamic job search/filter query building |
| Utility | 1 | Skill normalizer alias mapping |

```bash
# Run all tests (requires Docker for Testcontainers)
./mvnw test
```

## Project structure

```
ai-resume-screener/
├── docker-compose.yml
├── .env.example
├── resume-screener/                          # Spring Boot API
│   ├── Dockerfile
│   └── src/
│       ├── main/java/com/risheek/resume_screener/
│       │   ├── controller/                   # REST controllers
│       │   ├── service/                      # Business logic + ML HTTP calls
│       │   ├── listener/                     # RabbitMQ notification listener
│       │   ├── entity/                       # JPA entities
│       │   ├── repository/                   # Spring Data repositories
│       │   ├── dto/                          # Request / response DTOs
│       │   ├── exception/                    # Custom exceptions + global handler
│       │   ├── jwt/                          # JWT generation and validation
│       │   ├── util/                         # SkillNormalizer
│       │   └── config/                       # Security, RabbitMQ, Cache, WebClient, OpenAPI
│       └── test/                             # Unit + integration tests
└── ml-service/                               # FastAPI ML microservice
    ├── Dockerfile
    ├── requirements.txt
    └── main.py
```

## Author

Risheek Shrestha — [github.com/Risheek-Shrestha](https://github.com/Risheek-Shrestha)