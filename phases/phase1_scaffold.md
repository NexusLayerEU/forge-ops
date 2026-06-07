# Phase 1 — Project Scaffold & Infrastructure

## Goal
Create the skeleton of the ForgeOps project: directory structure, Spring Boot app, React app, Docker Compose, and database migrations. At the end of this phase, `docker compose up -d` brings up all services and the API health endpoint responds.

## Tasks

### 1.1 — Spring Boot Project Scaffold
Create `pom.xml` with all dependencies from `docs/09_docker_compose.md`.
Create `ForgeOpsApplication.java` with `@SpringBootApplication`.
Create `src/main/resources/application.properties` with all config keys from `docs/09_docker_compose.md` env section, using `${ENV_VAR:default}` syntax.

### 1.2 — Database Migrations (Flyway)
Create `src/main/resources/db/migration/V1__initial_schema.sql` with ALL tables from `docs/03_data_model.md`.
Create `src/main/resources/db/migration/V2__seed_admin_user.sql` — insert default admin user (bcrypt hash of `ForgeOps@Change_Me_Now!`, `must_change_password = true`).
Create `src/main/resources/db/migration/V3__indexes.sql` — add performance indexes.

### 1.3 — React UI Scaffold
Create `ui/` directory.
Create `ui/package.json` with all dependencies from `docs/09_docker_compose.md`.
Create `ui/vite.config.ts` — proxy `/api` to `http://localhost:8080`.
Create `ui/tailwind.config.js` — configure with the color palette from `docs/06_webui_spec.md`.
Create `ui/src/main.tsx`, `ui/src/App.tsx` with react-router-dom v6 `<Routes>` shell (placeholder routes for all screens).
Create `ui/src/app/Layout.tsx` — sidebar + main content shell as described in `docs/06_webui_spec.md`.

### 1.4 — Docker Compose
Create `docker-compose.yml` (exact content from `docs/09_docker_compose.md`).
Create `Dockerfile.api` (exact content from `docs/09_docker_compose.md`).
Create `ui/Dockerfile.ui` (exact content from `docs/09_docker_compose.md`).
Create `ui/nginx.conf` (exact content from `docs/09_docker_compose.md`).
Create `.env.example` (exact content from `docs/09_docker_compose.md`).

### 1.5 — Health Endpoint
Create `HealthController.java` at `GET /api/v1/health` returning `{ "status": "ok", "version": "1.0.0" }`.

## Acceptance Criteria
- [ ] `docker compose up -d` starts all 4 services without errors
- [ ] `curl http://localhost:8080/api/v1/health` returns `{"status":"ok"}`
- [ ] `curl http://localhost:3000` serves the React app HTML
- [ ] PostgreSQL has all tables from V1 migration
- [ ] Redis is reachable from the API container
- [ ] No hardcoded secrets — all from env vars
