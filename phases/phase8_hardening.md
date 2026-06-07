# Phase 8 — Hardening, Metrics & Final QA

## Goal
Production-readiness: error handling, health checks, metrics, logging, and final end-to-end testing.

## Tasks

### 8.1 — Global Exception Handler
`GlobalExceptionHandler` (`@ControllerAdvice`):
Maps all exceptions to standard error response format from `docs/05_api_spec.md`.
`ConstraintViolationException` → 400
`EntityNotFoundException` → 404
`DataIntegrityViolationException` → 409
`AccessDeniedException` → 403
`Exception` → 500

### 8.2 — Actuator + Metrics
Enable Spring Actuator endpoints: `/actuator/health`, `/actuator/info`, `/actuator/metrics`.
Custom metrics: `forgeops.runs.total`, `forgeops.runs.failed`, `forgeops.drift.reports`.

### 8.3 — Logging
Structured JSON logging via Logback.
Log run start/end at INFO level.
Log all API errors at WARN/ERROR with request details.
Never log secrets or credentials.

### 8.4 — End-to-End Test
Write a single E2E test script (`scripts/e2e_test.sh`) using curl that:
1. Logs in as admin
2. Creates a node
3. Creates a forge with a valid ForgeSpec
4. Triggers a run against localhost (test node)
5. Polls run status until complete
6. Triggers a drift check
7. Verifies drift report

### 8.5 — README
Create `README.md` at project root with:
- Project description (what ForgeOps is)
- Quick start (`docker compose up`)
- Default credentials
- Link to docs/ directory
- ForgeSpec example

## Acceptance Criteria
- [ ] All invalid inputs return proper structured error responses
- [ ] `/actuator/health` returns UP
- [ ] E2E test script completes successfully
- [ ] No secrets in logs
- [ ] README is complete and accurate
- [ ] `docker compose up -d` from clean state brings everything up in < 60 seconds
