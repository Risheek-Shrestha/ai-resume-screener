# Contributing

Thanks for your interest in this project. It started as a solo build, but contributions, bug reports, and suggestions are welcome.

## Getting started

1. Fork the repo and clone your fork.
2. Copy `.env.example` to `.env` (or set up `application-local.properties` per the README) and fill in your local values.
3. Run the stack with `docker compose up --build`.
4. Make sure the app boots cleanly and existing tests pass before making changes:
   ```
   cd resume-screener
   ./mvnw test
   ```

## Making changes

- Create a feature branch off `main`: `git checkout -b feat/short-description`.
- Keep commits focused — one logical change per commit.
- Follow [Conventional Commits](https://www.conventionalcommits.org/) for commit messages, e.g.:
  ```
  feat(applications): add status transition validation
  fix(auth): correct refresh token expiry check
  ```
- Add or update tests for any behavior change.
- Run the full test suite before opening a PR.

## Pull requests

- Describe what changed and why.
- Link any related issue.
- Keep PRs scoped to a single feature or fix where possible — smaller PRs are easier to review.

## Reporting bugs

Open an issue with:
- Steps to reproduce
- Expected vs actual behavior
- Relevant logs or screenshots if applicable

## Code style

- Follow existing package structure and naming conventions in the codebase.
- Prefer constructor injection over field injection for Spring beans.
- Keep controllers thin — business logic belongs in services.
