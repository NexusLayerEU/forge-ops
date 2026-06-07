# ForgeOps — Architecture

## Component Map

```
┌─────────────────────────────────────────────────────────────┐
│                        Browser / CLI                         │
└────────────────────────┬────────────────────────────────────┘
                         │ HTTP / WebSocket
┌────────────────────────▼────────────────────────────────────┐
│                  ForgeOps API Server                         │
│              (Spring Boot 3, Java 21)                        │
│                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐  │
│  │  REST API    │  │  WS/STOMP    │  │  Auth/RBAC       │  │
│  │  Controllers │  │  Broker      │  │  JWT Filter      │  │
│  └──────┬───────┘  └──────┬───────┘  └──────────────────┘  │
│         │                 │                                   │
│  ┌──────▼─────────────────▼──────────────────────────────┐  │
│  │                   Service Layer                        │  │
│  │  InventoryService │ ForgeService │ RunService          │  │
│  │  DriftService     │ VaultService │ AuditService        │  │
│  └──────┬─────────────────────────────────────────────┬──┘  │
│         │                                             │      │
│  ┌──────▼──────────┐                    ┌────────────▼───┐  │
│  │  ForgeSpec      │                    │  Run Engine    │  │
│  │  Parser &       │                    │  (Executor)    │  │
│  │  Validator      │                    │                │  │
│  │  Policy Engine  │                    │ SSH (JSch)     │  │
│  └─────────────────┘                    │ WinRM (async)  │  │
│                                         │ Task Scheduler │  │
│                                         └────────────────┘  │
└──────────────────────┬──────────────────────────────────────┘
                       │
         ┌─────────────┼──────────────┐
         ▼             ▼              ▼
   ┌──────────┐  ┌──────────┐  ┌──────────┐
   │PostgreSQL│  │  Redis   │  │  Target  │
   │  16      │  │  7       │  │  Nodes   │
   │ (state)  │  │ (cache,  │  │ (SSH/    │
   │          │  │  pubsub) │  │  WinRM)  │
   └──────────┘  └──────────┘  └──────────┘
```

## Frontend Architecture

```
ui/
├── src/
│   ├── app/          # Router, global providers
│   ├── features/     # Feature modules (inventory, forges, runs, drift)
│   │   ├── inventory/
│   │   ├── forges/
│   │   ├── runs/
│   │   ├── drift/
│   │   └── settings/
│   ├── components/   # Shared UI components
│   ├── hooks/        # Shared React hooks (useWebSocket, useApi)
│   ├── lib/          # API client, utils
│   └── styles/       # Tailwind config, global CSS
```

## Backend Package Structure

```
src/main/java/eu/forgeops/
├── api/
│   ├── controller/     # REST controllers
│   ├── dto/            # Request/Response DTOs
│   └── websocket/      # STOMP handlers
├── domain/
│   ├── inventory/      # Node, Group entities + services
│   ├── forge/          # Forge, ForgeVersion entities + services
│   ├── run/            # Run, RunTask, RunLog entities + services
│   ├── drift/          # DriftReport, DriftItem entities + services
│   ├── vault/          # Secret, Credential entities + services
│   └── audit/          # AuditEvent entity + service
├── engine/
│   ├── parser/         # ForgeSpec YAML parser
│   ├── validator/      # ForgeSpec semantic validator
│   ├── policy/         # Policy engine (desired state)
│   ├── executor/       # Task executor orchestrator
│   ├── modules/        # Built-in modules (package, service, file, etc.)
│   ├── ssh/            # SSH connection pool + execution
│   └── winrm/          # WinRM execution
├── infra/
│   ├── persistence/    # JPA repositories
│   ├── cache/          # Redis config + caching
│   └── security/       # JWT, RBAC config
└── ForgeOpsApplication.java
```

## Communication Patterns

### REST API
Standard request-response for CRUD operations on all resources.

### WebSocket (STOMP over SockJS)
Real-time streaming for:
- Run output lines as they arrive from target nodes
- Drift detection results
- Job status updates

Topic structure:
```
/topic/runs/{runId}/output      # streaming task output
/topic/runs/{runId}/status      # run status changes
/topic/drift/{reportId}         # drift check results
/topic/notifications            # system-wide alerts
```

### Redis PubSub
Internal decoupling between the Run Engine worker threads and the WebSocket broker. The engine publishes output lines to Redis channels; the Spring WebSocket layer subscribes and forwards to connected browsers.

## Deployment (Docker Compose)

```
services:
  forgeops-api      # Spring Boot API server (port 8080)
  forgeops-ui       # React app via nginx (port 3000)
  postgres          # PostgreSQL 16 (port 5432)
  redis             # Redis 7 (port 6379)
```

All inter-service communication on internal Docker network `forgeops-net`.
Only ports 3000 and 8080 exposed to host.
