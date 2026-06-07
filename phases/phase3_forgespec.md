# Phase 3 — ForgeSpec Parser & Policy Engine

## Goal
Implement the ForgeSpec YAML DSL parser, semantic validator, and policy engine. At the end, a ForgeSpec file can be stored, versioned, and validated.

## Tasks

### 3.1 — ForgeSpec Object Model
Create Java record/POJO classes mirroring the ForgeSpec structure from `docs/04_forgespec_dsl.md`:
`ForgeSpecDocument`, `ForgeTask`, `ForgePolicy`, `ForgeHandler`, `ForgeTarget`, `ModuleParams`.

### 3.2 — YAML Parser
`ForgeSpecParser` — uses Jackson YAML to deserialize ForgeSpec YAML into the object model.
Catches malformed YAML with clear error messages.

### 3.3 — Semantic Validator
`ForgeSpecValidator` — validates parsed object graph:
All 10 validation rules from `docs/04_forgespec_dsl.md`.
Returns `ValidationResult` with list of `ValidationError` (line number, message, severity).

### 3.4 — Variable Resolver
`VariableResolver` — resolves `{{ variable }}` expressions.
Implements precedence chain: task vars → forge vars → node vars → group vars → builtins.
Runtime-only variables (e.g., `{{ run_id }}`) flagged but not errors.

### 3.5 — Forge JPA Entities + Repositories
Create `Forge`, `ForgeVersion`, `ForgeGroupBinding` entities.
Create `ForgeRepository`, `ForgeVersionRepository`, `ForgeGroupBindingRepository`.

### 3.6 — Forge Service
`ForgeService`:
- `createForge` — parses + validates content, creates Forge + ForgeVersion(1)
- `updateForge` — creates a new ForgeVersion, runs validation
- `validateContent` — parse + validate only, no persistence
- `getForge`, `listForges`, `deleteForge`
- `pinVersion`, `getVersion`
- `addBinding`, `removeBinding`, `listBindings`

### 3.7 — Forge REST Controllers
`ForgeController` — all endpoints from `docs/05_api_spec.md` under `/forges`.

### 3.8 — Integration Tests
Test parse + validate round-trip for valid and invalid ForgeSpec YAML.
Test all ForgeSpec module definitions from `docs/04_forgespec_dsl.md`.

## Acceptance Criteria
- [ ] Valid ForgeSpec from `docs/04_forgespec_dsl.md` Full Example parses without errors
- [ ] Invalid ForgeSpec returns appropriate errors with line numbers
- [ ] `POST /api/v1/forges` stores and validates
- [ ] `POST /api/v1/forges/{id}/validate` returns errors array
- [ ] ForgeVersion history works correctly

---

