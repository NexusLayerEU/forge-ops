# Agent Handover

> You are an AI agent starting a new session. Read this file first, then `memory.md`, then `state.md`.

---

## Project Overview

**ForgeOps** — Infrastructure automation platform combining Ansible's agentless push model with Puppet's declarative desired-state model. Java 21 Spring Boot 3.3 backend + React 18/Vite/Tailwind frontend + PostgreSQL 16 + Redis 7.

Docker Compose brings everything up with `docker compose up -d`.
Default login: `admin` / `ForgeOps@Change_Me_Now!`

---

## Current State

Phase 1 complete. Phase 2 (Inventory API) is next.

See `state.md` for full phase status.

---

## What To Do Next

**Phase 2 — Inventory API:**
1. JPA entities: `Node`, `NodeVariable`, `Group`, `GroupVariable` (NodeGroupMembership is a join table — handle via @ManyToMany)
2. Repositories with custom queries
3. DTOs + MapStruct mappers
4. `InventoryService` — full CRUD for nodes, groups, variables, group memberships, + ping
5. `NodeController`, `GroupController` — all REST endpoints from docs/05_api_spec.md
6. RBAC annotations per docs/08_security.md
7. Audit logging on all mutating operations
8. Integration tests

Create packages under:
- `src/main/java/eu/forgeops/domain/inventory/` (entities, services)
- `src/main/java/eu/forgeops/api/controller/` (controllers)
- `src/main/java/eu/forgeops/api/dto/` (DTOs)

---

## Key Files & Directories

```
/Users/admin/Documents/Thomas-SRC/ForgeOps/
├── pom.xml                              # Maven dependencies
├── docker-compose.yml                   # Full stack
├── Dockerfile.api                       # Spring Boot (uses apk add maven)
├── src/main/java/eu/forgeops/
│   ├── ForgeOpsApplication.java
│   ├── api/controller/HealthController.java
│   └── infra/security/SecurityConfig.java
├── src/main/resources/
│   ├── application.properties
│   └── db/migration/V1-V3 (schema, seed, indexes)
├── ui/                                  # React frontend
│   ├── src/app/                         # Layout, AuthContext, Router
│   ├── src/features/                    # Feature modules (placeholder)
│   └── src/lib/api.ts                   # Axios client
└── docs/ + phases/                      # Full specification
```

---

## Java Package Structure (target)
```
eu.forgeops
├── api/
│   ├── controller/    # REST + HealthController
│   ├── dto/           # Request/Response DTOs
│   └── websocket/     # STOMP handlers (Phase 4)
├── domain/
│   ├── inventory/     # Node, Group, etc. (Phase 2)
│   ├── forge/         # Forge, ForgeVersion (Phase 3)
│   ├── run/           # Run, RunTask, RunLog (Phase 4)
│   ├── drift/         # DriftReport, DriftItem (Phase 5)
│   ├── vault/         # Secret (Phase 5)
│   └── audit/         # AuditEvent (Phase 5)
├── engine/            # ForgeSpec parser, executor, modules (Phases 3-4)
└── infra/
    ├── persistence/   # JPA repositories
    ├── cache/         # Redis config
    └── security/      # JWT, RBAC
```
