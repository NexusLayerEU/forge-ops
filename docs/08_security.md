# ForgeOps — Security

## Authentication

- JWT Bearer tokens
- Token expiry: 24 hours (configurable)
- Refresh token: 7 days
- Stored in `Authorization: Bearer <token>` header
- Tokens are stateless — validated via signature only
- Token blacklisting via Redis for logout/revocation

### JWT Payload
```json
{
  "sub": "user-uuid",
  "username": "admin",
  "roles": ["ADMIN"],
  "iat": 1700000000,
  "exp": 1700086400
}
```

### Spring Security Config
- `JwtAuthenticationFilter` on all `/api/v1/**` except `/api/v1/auth/**`
- CSRF disabled (stateless API)
- CORS: configurable allowed origins via `FORGEOPS_CORS_ORIGINS` env var

---

## RBAC

Roles are enforced via `@PreAuthorize` on service methods.

| Endpoint | ADMIN | OPERATOR | NODE_MANAGER | VIEWER |
|---|---|---|---|---|
| GET any resource | ✅ | ✅ | ✅ | ✅ |
| POST /nodes, /groups | ✅ | ✅ | ✅ | ❌ |
| POST /forges | ✅ | ✅ | ❌ | ❌ |
| POST /runs | ✅ | ✅ | ❌ | ❌ |
| POST /drift/reports | ✅ | ✅ | ❌ | ❌ |
| POST /vault/secrets | ✅ | ✅ | ❌ | ❌ |
| DELETE any resource | ✅ | ✅ | ✅ (nodes/groups) | ❌ |
| GET/POST /users | ✅ | ❌ | ❌ | ❌ |
| GET /audit | ✅ | ✅ | ❌ | ✅ |

---

## Vault / Secret Encryption

- Algorithm: AES-256-GCM
- Key: derived from `FORGEOPS_VAULT_ENCRYPTION_KEY` env var via PBKDF2
- IV: randomly generated per secret, stored alongside encrypted value
- Decryption: only at execution time, inside the Run Engine, never returned via API
- SSH private keys stored as encrypted BLOB in `secrets.encrypted_value`

---

## Node Credentials

Each node references a `credential_id` pointing to a `secrets` record.

Supported secret types and their usage:
- `ssh_key` → loaded via JSch `addIdentity()`
- `password` → SSH password authentication
- `token` → WinRM token auth
- `certificate` → WinRM certificate auth

---

## Audit Logging

Every mutating API call triggers an `AuditEvent`:
- Recorded in `audit_events` table
- Fields: user, action, resource type, resource ID, IP address, full request payload
- Non-repudiable: audit records are insert-only (no UPDATE/DELETE on audit table)
- Action types: `NODE_CREATED`, `NODE_UPDATED`, `NODE_DELETED`, `FORGE_CREATED`, `FORGE_UPDATED`, `RUN_CREATED`, `RUN_CANCELLED`, `SECRET_CREATED`, `SECRET_DELETED`, `USER_CREATED`, `USER_UPDATED`, `DRIFT_REMEDIATED`, etc.

---

## Security Headers (API Server)

Spring Security adds:
```
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
Strict-Transport-Security: max-age=31536000 (if HTTPS)
```

## Security Headers (nginx / UI)

```nginx
add_header Content-Security-Policy "default-src 'self'; script-src 'self'; connect-src 'self' ws:";
add_header X-Frame-Options DENY;
add_header X-Content-Type-Options nosniff;
```

---

## Default Admin User

Seeded via Flyway migration V2 on first startup:
```
Username: admin
Password: ForgeOps@Change_Me_Now!
Role: ADMIN
```
Force password change on first login (flag in users table: `must_change_password`).
