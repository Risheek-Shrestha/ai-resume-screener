# Changelog

All notable changes to this project are documented here. Format loosely follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Fixed
- CORS configuration now reads allowed origin from `APP_FRONTEND_URL` instead of a hardcoded localhost value.
- Auth interceptor on the frontend no longer masks login failures behind a misleading "invalid refresh token" error.
- Externalized all secrets (datasource, JWT, mail, RabbitMQ) from `application.properties` into environment-variable placeholders for safe production deployment.

## [0.3.0] — Deployment

### Added
- Production deployment: Spring Boot API and ML microservice on Render, frontend on Vercel, RabbitMQ via CloudAMQP.
- Education entity and CRUD endpoints.

### Changed
- Full frontend styling pass across all pages with a consistent dark slate/cyan design system.

## [0.2.0] — Core feature build

### Added
- Application lifecycle management with validated status transitions (`APPLIED` → `SHORTLISTED`/`REJECTED` → `HIRED`/`REJECTED`).
- Email notifications via RabbitMQ + Mailtrap for application status changes.
- Redis caching on job listings.
- Role-based access control (RBAC) across all endpoints.
- Job search and filtering (keyword, type, level, skill).
- Full authentication flow: JWT access tokens, refresh token rotation, protected/role-gated routes.

### Testing
- Full test pyramid: repository tests (`@DataJpaTest` + Testcontainers), service tests (Mockito), controller tests (`@WebMvcTest`).

## [0.1.0] — Initial build

### Added
- Core domain models: User, Job, Resume, Application, Score.
- FastAPI ML microservice for resume parsing and job-match scoring (TF-IDF + sentence-transformers).
- Dockerized local development environment (API + ML service + Postgres + RabbitMQ).
- Swagger/OpenAPI documentation with JWT bearer auth support.
