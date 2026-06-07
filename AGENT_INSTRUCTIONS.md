# AGENT_INSTRUCTIONS.md — ForgeOps

## What You Are Building

**ForgeOps** — A unified infrastructure automation platform combining the agentless push model of Ansible with the declarative desired-state model of Puppet. It features a React-based Web UI, a REST + WebSocket API server, a policy engine, an inventory manager, and a run engine with real-time streaming output.

## Stack

| Layer | Technology |
|---|---|
| Backend API | Java 21, Spring Boot 3.3, WebSocket (STOMP) |
| Policy Engine | YAML-based DSL (ForgeSpec), Groovy script hooks |
| Run Engine | SSH (JSch), WinRM (async), parallel task executor |
| Frontend | React 18, Vite, Tailwind CSS, shadcn/ui |
| Database | PostgreSQL 16 (state store, audit log) |
| Cache | Redis 7 |
| Containerization | Docker Compose (no Kubernetes required) |
| Auth | JWT + RBAC |

## Documents to Read First (in order)

1. `docs/01_overview.md` — Product vision, concepts, terminology
2. `docs/02_architecture.md` — System architecture, component map
3. `docs/03_data_model.md` — All database schemas and entity relationships
4. `docs/04_forgespec_dsl.md` — The ForgeSpec policy/playbook DSL specification
5. `docs/05_api_spec.md` — Full REST API and WebSocket contract
6. `docs/06_webui_spec.md` — Web UI screens, components, wireframe descriptions
7. `docs/07_run_engine.md` — Run engine internals, SSH/WinRM execution, parallelism
8. `docs/08_security.md` — Auth, RBAC, secrets management
9. `docs/09_docker_compose.md` — All Docker Compose files and environment config

## Build Phases

Execute phases in order. Do not start a phase until the previous one passes its acceptance criteria.

| Phase | File | Description |
|---|---|---|
| 1 | `phases/phase1_scaffold.md` | Project scaffold, Docker Compose, DB migrations |
| 2 | `phases/phase2_inventory.md` | Inventory API — hosts, groups, variables |
| 3 | `phases/phase3_forgespec.md` | ForgeSpec parser, validator, policy engine |
| 4 | `phases/phase4_run_engine.md` | SSH/WinRM run engine, job executor, streaming |
| 5 | `phases/phase5_api.md` | Full REST API and WebSocket layer |
| 6 | `phases/phase6_webui.md` | React Web UI — all screens |
| 7 | `phases/phase7_auth.md` | JWT auth, RBAC, secrets vault integration |
| 8 | `phases/phase8_hardening.md` | Error handling, audit log, metrics, final QA |

## Global Rules

- All source goes under `src/` (backend) and `ui/` (frontend)
- All secrets via environment variables only — never hardcoded
- Every API endpoint must have a corresponding integration test
- Commit message format: `[Phase N] description`
- Docker Compose must bring the full stack up with `docker compose up -d`
- The Web UI must be fully functional with no placeholder screens
