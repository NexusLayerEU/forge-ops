# Phase 2 — Inventory API (Nodes & Groups)

## Goal
Implement the complete inventory management system: Nodes, Groups, Variables, and Group Memberships — with full REST API and integration tests.

## Tasks

### 2.1 — JPA Entities
Create `@Entity` classes for all inventory tables from `docs/03_data_model.md`:
`Node`, `NodeVariable`, `Group`, `GroupVariable`, `NodeGroupMembership`.
Use Lombok `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`.
Relationships: `@ManyToMany` between Node and Group via join table.

### 2.2 — Repositories
Create `JpaRepository` interfaces: `NodeRepository`, `GroupRepository`, `NodeVariableRepository`, `GroupVariableRepository`.
Add custom query methods: `findByStatus`, `findByNameContaining`, `findByGroupId`.

### 2.3 — DTOs and MapStruct Mappers
Create request/response DTOs for all node and group endpoints (see `docs/05_api_spec.md`).
Create MapStruct mappers: `NodeMapper`, `GroupMapper`.

### 2.4 — Service Layer
`InventoryService` with methods:
- `createNode`, `updateNode`, `deleteNode`, `getNode`, `listNodes` (paginated)
- `createGroup`, `updateGroup`, `deleteGroup`, `getGroup`, `listGroups`
- `addNodesToGroup`, `removeNodeFromGroup`
- `addNodeVariable`, `deleteNodeVariable`
- `addGroupVariable`, `deleteGroupVariable`
- `pingNode` — async SSH ping, returns reachability + latency

### 2.5 — REST Controllers
`NodeController` — all endpoints from `docs/05_api_spec.md` under `/nodes`.
`GroupController` — all endpoints from `docs/05_api_spec.md` under `/groups`.
Apply `@PreAuthorize` per RBAC table in `docs/08_security.md`.
Add audit logging on all mutating operations.

### 2.6 — Integration Tests
Test every endpoint: happy path, 404, 409 (duplicate name), 403 (wrong role).
Use `@SpringBootTest` + `TestRestTemplate` + H2 in-memory for tests.

## Acceptance Criteria
- [ ] `POST /api/v1/nodes` creates a node and returns 201
- [ ] `GET /api/v1/nodes` returns paginated list
- [ ] `POST /api/v1/nodes/{id}/ping` returns reachability result
- [ ] `POST /api/v1/groups/{id}/members` adds nodes to group
- [ ] Node and Group variables CRUD works
- [ ] All 403 RBAC rules enforced
- [ ] All endpoints have integration tests passing

---

