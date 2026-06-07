# Project Memory

Captures architectural decisions, hard-won lessons, and constraints for ForgeOps.
Read before making any significant changes.

---

## Architectural Decisions

### Java/Maven
- Java 21 (virtual threads, ZGC) — Dockerfile.api installs Maven via `apk add maven`, not using mvnw
- Spring Boot 3.3 — Flyway 10+ requires explicit `flyway-database-postgresql` dependency or migrations fail
- BCryptPasswordEncoder cost=10 — stored as `$2b$` prefix (passlib Python), accepted by Spring Security

### Database
- PostgreSQL 16 — tables created in FK-dependency order (secrets before nodes, groups before forge_group_bindings)
- `must_change_password` column added to `users` table (mentioned in security spec, missing from original schema)
- Admin password: `ForgeOps@Change_Me_Now!`, bcrypt hash in V2 migration

### Testing
- Tests use H2 in-memory (PostgreSQL mode), Flyway disabled
- Redis disabled in test profile via `spring.autoconfigure.exclude`
- Active profile: `test`

### UI
- `@radix-ui/react-badge` does not exist — removed from package.json
- Vite types: must add `"types": ["vite/client"]` to tsconfig.json for `import.meta.env`
- Tailwind color tokens: background=#0a0e1a, surface=#111827, etc. defined in tailwind.config.js

### Docker
- Modified Dockerfile.api to use `apk add maven` instead of `./mvnw` (simpler, no wrapper setup)
- All services on `forgeops-net` bridge network
- Only ports 3000 (UI) and 8080 (API) exposed to host

---

## Gotchas & Lessons

- **Flyway 10+ PostgreSQL**: Always add `flyway-database-postgresql` alongside `flyway-core`
- **MapStruct + Lombok ordering**: Lombok must come before MapStruct in annotationProcessorPaths
- **H2 test config**: Set `DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH` for PostgreSQL mode compatibility
- **BCrypt $2b vs $2a**: Python passlib generates `$2b$`, Spring Security accepts both
