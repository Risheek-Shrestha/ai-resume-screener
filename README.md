# AI Resume Screener

An AI-assisted resume screening platform. Recruiters post jobs, candidates upload resumes, and the system automatically extracts resume text, scores it against the job using keyword + TF-IDF + semantic similarity, and returns actionable improvement suggestions, job recommendations, and a downloadable PDF report.

## Architecture

Three services, wired together over a Docker network:

```
┌──────────────────┐        ┌──────────────────┐        ┌──────────────────┐
│   PostgreSQL 15   │◄──────►│  Spring Boot API  │──────►│  FastAPI ML svc   │
│  (jobs, resumes,  │  JDBC  │   (Java 21, :8081) │ HTTP  │  (Python, :8000)  │
│  scores, users)   │        │                    │       │                    │
└──────────────────┘        └──────────────────┘        └──────────────────┘
                                       ▲
                                       │ JWT-authenticated REST
                                       │
                                   Client / Swagger UI
```

- **Spring Boot API** — owns auth, jobs, resumes, scores, suggestions, and PDF reports. Calls the ML service over HTTP for anything that requires NLP.
- **FastAPI ML microservice** — stateless. Extracts text from PDF/DOCX, scores a resume against a job (keyword matching with synonym normalization + TF-IDF + sentence-transformer semantic similarity), generates improvement suggestions, and ranks candidate jobs against a resume.
- **PostgreSQL** — persists users, jobs, job skills, resumes (including raw file bytes + parsed text), and scores (matched/missing keywords stored as JSONB).

## Tech stack

| Layer | Technology |
|---|---|
| API | Java 21, Spring Boot 4, Spring Data JPA, Spring Security, JJWT |
| ML service | Python, FastAPI, scikit-learn, sentence-transformers, pdfplumber, docx2txt |
| Database | PostgreSQL 15 |
| Docs | springdoc-openapi (Swagger UI) |
| Reports | Apache PDFBox |
| Infra | Docker, Docker Compose |

## Features

- JWT auth with access + refresh tokens (register, login, refresh, revoke)
- Job CRUD with per-job skill lists and an optional application window (`applicationStartsAt` / `applicationDeadline`)
- Resume upload (PDF/DOCX) with automatic text extraction
- AI match score: weighted blend of keyword coverage, TF-IDF content similarity, and semantic similarity
- Skill synonym normalization (e.g. `js` ↔ `javascript`, `k8s` ↔ `kubernetes`)
- Improvement suggestions: weak-area detection, actionable steps, free learning resources, resume-writing tips
- Job recommendations: ranks open jobs against a candidate's resume
- ATS-style application screening: applying auto-evaluates the candidate's score against the job, auto-accepting (`APPLIED`) or auto-rejecting (`REJECTED`) on the spot, and returns improvement suggestions alongside the result
- Employer application management: view all applicants for a job, view accepted applicants ranked by score, and move applications through a `SHORTLISTED → HIRED` / `REJECTED` status workflow with transition validation
- Downloadable PDF score report
- Redis caching on job listing/detail endpoints, invalidated on job create/update/delete
- Graceful degradation: if the ML service is unreachable, the API falls back to keyword-only matching instead of failing the request

## API reference

All endpoints are prefixed `/api/v1`. Full interactive docs are at `/swagger-ui/index.html` once the API is running.

| Method | Endpoint | Description |
|---|---|---|
| POST | `/auth/register` | Create an account |
| POST | `/auth/login` | Get access + refresh tokens |
| POST | `/auth/refresh` | Exchange a refresh token for a new access token |
| POST | `/auth/revoke` | Log out / invalidate a refresh token |
| POST | `/jobs` | Create a job posting |
| GET | `/jobs/{id}` | Get a job by ID |
| GET | `/jobs` | List jobs (paginated) |
| PUT | `/jobs/{id}` | Update a job |
| DELETE | `/jobs/{id}` | Delete a job |
| POST | `/resumes` | Upload a resume for a job |
| GET | `/resumes` | List your resumes |
| GET | `/resumes/{id}` | Get a resume by ID |
| PUT | `/resumes/{id}` | Replace a resume |
| DELETE | `/resumes/{id}` | Delete a resume |
| GET | `/scores/resume/{resumeId}` | Get the AI match score for a resume |
| GET | `/scores/my-scores` | List all your scores |
| GET | `/suggestions/improve/{resumeId}` | Get improvement suggestions for a resume |
| GET | `/suggestions/jobs/{resumeId}` | Get recommended jobs for a resume |
| POST | `/applications/jobs/{jobId}` | Apply to a job with a resume — auto-scored and auto-accepted/rejected |
| GET | `/applications/me` | List your own applications |
| GET | `/applications/jobs/{jobId}` | List all applicants for a job *(job owner only)* |
| GET | `/applications/jobs/{jobId}/accepted` | List accepted applicants for a job, ranked by score *(job owner only)* |
| PATCH | `/applications/{applicationId}/status` | Move an application to `SHORTLISTED`, `HIRED`, or `REJECTED` *(job owner only)* |
| GET | `/reports/resume/{resumeId}` | Get a structured report (score + suggestions) |
| GET | `/reports/resume/{resumeId}/pdf` | Download the report as a PDF |

The FastAPI ML service (`/extract-text`, `/analyze`, `/suggest`, `/match-jobs`) is internal — the Spring Boot API is the only intended caller, so it isn't exposed to end users in production setups.

**Application status workflow:** `APPLIED → SHORTLISTED → HIRED`, with `REJECTED` reachable from either `APPLIED` or `SHORTLISTED`. `HIRED` and `REJECTED` are terminal — any further transition attempt is rejected.

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

This starts three containers: `postgres`, `ml-service`, and `resume-screener`. First build takes a few minutes (the ML image bakes in the sentence-transformer model). Once it's up:

- API: `http://localhost:8081`
- Swagger UI: `http://localhost:8081/swagger-ui/index.html`
- ML service (internal): `http://localhost:8000`

## Running locally without Docker

**Prerequisites:** Java 21, Maven, Python 3.11+, a local PostgreSQL instance.

**Database**
```sql
CREATE DATABASE resume_screener;
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
# edit application.properties: datasource credentials, jwt.secret, jwt.expiration
./mvnw spring-boot:run
```

`ml.service.url` defaults to `http://localhost:8000` in `application.properties.example`, which is correct for local (non-Docker) runs.

## Project structure

```
ai-resume-screener/
├── docker-compose.yml
├── .env.example
├── resume-screener/        # Spring Boot API
│   ├── Dockerfile
│   └── src/main/java/com/risheek/resume_screener/
│       ├── controller/     # Auth, Job, Resume, Score, Suggestion, Application, Report
│       ├── service/        # business logic + ML service HTTP calls
│       ├── entity/         # JPA entities
│       ├── repository/     # Spring Data repositories
│       ├── jwt/            # JWT generation/validation
│       └── config/         # Security, Swagger, WebClient config
└── ml-service/             # FastAPI ML microservice
    ├── Dockerfile
    ├── requirements.txt
    └── main.py
```

## Author

Risheek Shrestha — [github.com/Risheek-Shrestha](https://github.com/Risheek-Shrestha)