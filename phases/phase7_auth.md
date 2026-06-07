# Phase 7 — Authentication & Security Hardening

## Goal
Complete the security layer: JWT authentication, RBAC enforcement, Vault encryption, security headers.

## Tasks

### 7.1 — JWT Filter
`JwtAuthenticationFilter` — validates token on every request, populates `SecurityContext`.
Handles expired tokens with 401 + clear error message.

### 7.2 — RBAC
Apply `@PreAuthorize` to every service method per RBAC table in `docs/08_security.md`.
Create custom `@HasRole` annotations for cleaner code.

### 7.3 — Vault Encryption
`AesGcmVaultEncryptor` — AES-256-GCM with random IV per secret.
Unit tests for encrypt/decrypt round-trip.

### 7.4 — Security Headers
Configure Spring Security `headers()` per `docs/08_security.md`.

### 7.5 — CORS
Configure allowed origins from `FORGEOPS_CORS_ORIGINS` env var.

### 7.6 — Audit Integration
Ensure `AuditService.record()` is called on every mutating operation across all services.

## Acceptance Criteria
- [ ] Unauthenticated requests to `/api/v1/nodes` return 401
- [ ] VIEWER role gets 403 on POST /nodes
- [ ] Secrets are encrypted at rest (verify in DB directly)
- [ ] All audit events recorded for create/update/delete operations
- [ ] Force password change prevents API access until changed

---

