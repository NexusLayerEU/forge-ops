# Phase 5 — Full REST API Layer

## Goal
Complete all remaining API endpoints not covered in earlier phases: Drift, Vault, Users, Audit.

## Tasks

### 5.1 — Vault / Secrets
`Secret` entity + `SecretRepository`.
`VaultService` — AES-256-GCM encryption/decryption using `FORGEOPS_VAULT_ENCRYPTION_KEY`.
`VaultController` — all endpoints from `docs/05_api_spec.md` under `/vault`.
Ensure `encrypted_value` is NEVER returned in API responses.

### 5.2 — Drift Engine
`DriftReport`, `DriftItem` entities + repositories.
`DriftService`:
- `createReport` — async, iterates nodes, calls `module.checkState()` for each policy
- Streams results via WebSocket as nodes are checked
- `remediateReport` — creates a remediation Run for all drifted items
`DriftController` — all endpoints from `docs/05_api_spec.md` under `/drift`.

### 5.3 — Drift Scheduler
`DriftScheduler` — Spring `@Scheduled` task.
On each tick: queries `forge_group_bindings` where `NOW() - last_check > check_interval`.
Triggers drift reports for due bindings.
Only runs if `FORGEOPS_DRIFT_SCHEDULER_ENABLED=true`.

### 5.4 — User Management
`UserController` — all endpoints from `docs/05_api_spec.md` under `/users` (ADMIN only).
Include force password change flow.

### 5.5 — Audit Controller
`AuditController` — `GET /audit` with full filtering from `docs/05_api_spec.md`.

### 5.6 — Auth Controllers
`AuthController` — `/auth/login`, `/auth/refresh`, `/auth/me`.
JWT generation + validation with JJWT library.
Redis-based token revocation on logout.

## Acceptance Criteria
- [ ] All API endpoints from `docs/05_api_spec.md` return correct responses
- [ ] Secrets API never exposes encrypted values
- [ ] Drift check runs and produces DriftItems
- [ ] Drift scheduler triggers correctly
- [ ] Auth flow: login → get token → use token → logout invalidates token

---

