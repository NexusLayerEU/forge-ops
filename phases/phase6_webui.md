# Phase 6 — React Web UI (All Screens)

## Goal
Build the complete React Web UI as specified in `docs/06_webui_spec.md`. All screens must be fully functional (not placeholder), connected to the real API.

## Tasks

### 6.1 — Foundation
`lib/api.ts` — Axios instance with base URL, JWT interceptor (attach token, refresh on 401).
`hooks/useWebSocket.ts` — STOMP client hook (connect, subscribe, unsubscribe lifecycle).
`app/AuthContext.tsx` — auth state (user, token, login, logout).
`app/router.tsx` — all routes with `<ProtectedRoute>` wrapper.
`app/Layout.tsx` — sidebar navigation, top bar, notification area.

### 6.2 — Dashboard Screen
`features/dashboard/DashboardPage.tsx`
4 stat cards (real data from API).
Recent Runs table.
Drift overview chart (Recharts).
Live Activity Feed (WebSocket `/topic/notifications`).

### 6.3 — Inventory — Nodes
`features/inventory/NodesPage.tsx` — grid/table with search + filters.
`features/inventory/NodeCard.tsx` — card component.
`features/inventory/NodeDetailPage.tsx` — all 5 tabs.
`features/inventory/NodeDrawer.tsx` — add/edit slide-in drawer.

### 6.4 — Inventory — Groups
`features/inventory/GroupsPage.tsx` — tree + detail panel.

### 6.5 — Forges
`features/forges/ForgesPage.tsx` — table with filters.
`features/forges/ForgeEditorPage.tsx` — Monaco editor + validation panel + version history sidebar.
`features/forges/ForgeDetailPage.tsx` — all 4 tabs.
`features/forges/RunModal.tsx` — target selection + run trigger.

### 6.6 — Runs
`features/runs/RunsPage.tsx` — table with status filters.
`features/runs/RunDetailPage.tsx` — per-node tabs + live streaming output panel + summary sidebar.
WebSocket output stream connected.

### 6.7 — Drift
`features/drift/DriftPage.tsx` — cards + reports table.
`features/drift/DriftReportDetailPage.tsx` — node grid + drift items table + remediate button.

### 6.8 — Vault
`features/vault/VaultPage.tsx` — secrets table + add drawer.

### 6.9 — Audit
`features/audit/AuditPage.tsx` — filterable table with expandable rows.

### 6.10 — Settings
`features/settings/SettingsPage.tsx` — Profile / Users / System tabs.

### 6.11 — Auth
`features/auth/LoginPage.tsx` — login form.
Force password change modal on first login.

## Acceptance Criteria
- [ ] All screens render without errors
- [ ] No placeholder/lorem ipsum content
- [ ] Dashboard stats reflect real API data
- [ ] Forge editor validates in real time
- [ ] Run detail page streams live output via WebSocket
- [ ] Drift report shows per-node status
- [ ] Login/logout flow works
- [ ] RBAC: Viewer cannot see Create/Edit buttons

---

