# ForgeOps — Web UI Specification

## Design System

- **Framework**: React 18 + Vite
- **UI Library**: Tailwind CSS + shadcn/ui
- **State Management**: Zustand (global), React Query (server state)
- **WebSocket**: @stomp/stompjs + SockJS
- **Code Editor**: Monaco Editor (for ForgeSpec editing)
- **Icons**: Lucide React
- **Charts**: Recharts

### Color Palette
```
Background (dark):  #0a0e1a
Surface:            #111827
Surface Elevated:   #1a2236
Border:             #1e2d45
Accent (primary):   #3b82f6  (blue-500)
Accent (success):   #22c55e  (green-500)
Accent (warning):   #f59e0b  (amber-500)
Accent (danger):    #ef4444  (red-500)
Text Primary:       #f1f5f9
Text Secondary:     #94a3b8
```

### Typography
```
Display font:  'JetBrains Mono' (headings, monospace UI elements)
Body font:     'Inter' (general UI text)
```

---

## Layout

### Shell Layout
Persistent across all authenticated pages:

```
┌─────────────────────────────────────────────────────┐
│  SIDEBAR (240px)          │  MAIN CONTENT            │
│                           │                           │
│  [ForgeOps logo]          │  [Page Header]            │
│                           │                           │
│  Navigation:              │  [Page Content]           │
│  ▪ Dashboard              │                           │
│  ▪ Inventory              │                           │
│  ▪ Forges                 │                           │
│  ▪ Runs                   │                           │
│  ▪ Drift                  │                           │
│  ▪ Vault                  │                           │
│  ▪ Audit                  │                           │
│  ▪ Settings               │                           │
│                           │                           │
│  [User avatar + name]     │                           │
└─────────────────────────────────────────────────────┘
```

---

## Screens

---

### 1. Dashboard `/`

**Purpose**: Overview of the entire infrastructure state at a glance.

**Components**:

**Stats Row** (4 cards):
- Total Nodes | Online nodes highlighted in green
- Active Runs | Count of currently running jobs
- Drifted Nodes | Count from latest drift reports (red if > 0)
- Forges | Total number of forges

**Recent Runs Table**:
Columns: Status badge | Forge name | Target count | Duration | Triggered by | Started at
Clicking a row navigates to Run Detail.

**Drift Overview Chart** (Recharts bar chart):
Last 7 days — daily bars showing "compliant" vs "drifted" nodes.

**Live Activity Feed** (right panel):
WebSocket-powered stream of recent events:
- Run started/completed
- Drift detected
- Node came online/went offline
- Format: `[timestamp] [icon] description`

---

### 2. Inventory — Nodes `/inventory/nodes`

**Purpose**: Manage all managed nodes.

**Top Bar**: Search input | Filter by status (All / Reachable / Unreachable / Unknown) | Filter by group | "Add Node" button

**Node Cards Grid** (or toggle to table):
Each card shows:
- Node name (large, monospace)
- Hostname
- OS type icon (Linux penguin / Windows logo)
- Status pill: `REACHABLE` (green) / `UNREACHABLE` (red) / `UNKNOWN` (gray)
- Group badges
- Last seen timestamp
- Actions: Edit | Ping | Delete

**Add/Edit Node Drawer** (slides in from right):
Form fields: Name, Hostname, Port, OS Type, Connection Type, Credential (select), Description, Tags (key-value pairs)

---

### 3. Inventory — Node Detail `/inventory/nodes/:id`

**Tabs**:
1. **Overview**: All node fields, ping button, connection test result
2. **Variables**: Table of key/value pairs, add/edit/delete inline
3. **Groups**: Chips of group memberships, add/remove
4. **Runs**: Runs that targeted this node (table, last 20)
5. **Drift**: Drift items for this node across all reports

---

### 4. Inventory — Groups `/inventory/groups`

**Purpose**: Manage node groups.

**Tree View** (left panel — nested groups):
- Expandable tree showing parent/child group hierarchy
- Click to select and view group detail

**Group Detail Panel** (right):
- Group name, description, parent
- Members: list of nodes in this group
- Variables: key/value table
- Forge bindings: which forges are bound to this group (policy mode)

**Add/Edit Group Modal**: Name, description, parent group selector

---

### 5. Forges `/forges`

**Purpose**: Manage ForgeSpec files.

**Top Bar**: Search | Filter by mode (All / Policy / Playbook / Mixed) | "New Forge" button

**Forge List** (table):
Columns: Name | Mode badge | Version | Status (Valid / Invalid) | Last modified | Actions

**New Forge** → navigates to Forge Editor.

---

### 6. Forge Editor `/forges/:id/edit` and `/forges/new`

**Purpose**: Full-featured ForgeSpec editor.

**Layout**:
```
┌─────────────────────────────────┬─────────────────────┐
│  Monaco Editor (ForgeSpec YAML) │  Validation Panel   │
│                                 │                     │
│  [syntax highlighted YAML]      │  ✅ Valid           │
│                                 │  or                 │
│                                 │  ❌ Errors:         │
│                                 │  Line 12: ...       │
│                                 │  Line 18: ...       │
│                                 │                     │
│                                 │  ⚠ Warnings:        │
│                                 │  ...                │
├─────────────────────────────────┴─────────────────────┤
│  [Save as Draft] [Validate] [Save & Activate] [Run ▸] │
└─────────────────────────────────────────────────────────┘
```

**Validation**: Real-time as user types (debounced 800ms) — calls `POST /forges/{id}/validate`

**Version History Panel** (toggleable sidebar): List of all versions with timestamps, diff view between versions.

**Run Modal** (triggered by "Run ▸" button):
- Select target nodes or groups (multi-select)
- Select tags to filter tasks (optional)
- Confirm → `POST /runs` → redirect to Run Detail

---

### 7. Forge Detail `/forges/:id`

**Tabs**:
1. **Overview**: Forge metadata, mode, current version, validation status
2. **Versions**: Version history table with diff viewer
3. **Bindings**: Policy bindings to groups (add/remove binding, toggle auto-remediate)
4. **Runs**: All runs of this forge

---

### 8. Runs `/runs`

**Purpose**: View and manage all runs.

**Filters**: Status pills (All / Running / Success / Failed / Partial / Cancelled) | Date range picker | Search by forge name

**Runs Table**:
Columns: Status | Forge | Nodes | Summary (ok/changed/failed) | Duration | Triggered by | Time

**Status Badges** (animated for running):
- `RUNNING` — pulsing blue
- `SUCCESS` — green
- `FAILED` — red
- `PARTIAL` — orange
- `CANCELLED` — gray

---

### 9. Run Detail `/runs/:id`

**Purpose**: Live streaming view of a run's execution.

**Header**: Run ID | Status | Forge name + version | Triggered by | Duration timer

**Node Tab Bar**: One tab per target node. Badge on each tab shows node status.

**Output Panel** (main area):
```
  ● Task 1/5: Install nginx              [OK]      2.1s
  ├─ stdout: Reading package lists...
  ├─ stdout: Building dependency tree...
  └─ stdout: nginx is already the newest version.

  ● Task 2/5: Deploy nginx config        [CHANGED] 0.8s
  └─ stdout: Template written to /etc/nginx/nginx.conf

  ⟳ Task 3/5: Restart nginx             [RUNNING]
  └─ stdout: Restarting nginx service...
```

**WebSocket**: Output lines stream in real time via `/topic/runs/{runId}/output`

**Summary Sidebar**:
- ok: 5 | changed: 2 | failed: 0 | skipped: 1
- Progress bar
- Cancel button (if running)

---

### 10. Drift `/drift`

**Purpose**: View drift detection results.

**Dashboard Cards**:
- Compliant nodes | Drifted nodes | Last check time

**Reports Table**:
Columns: Forge | Group | Drifted/Total nodes | Status | Created at

**New Drift Check Button** → Modal: select forge + group → POST /drift/reports

---

### 11. Drift Report Detail `/drift/reports/:id`

**Node Status Grid**: Cards for each node — green (compliant) or red (drifted count).

**Drift Items Table**:
```
Node        Resource Type   Resource Name   Expected State   Actual State     Drifted
web-01      service         nginx           running          stopped          ❌ YES
web-02      service         nginx           running          running          ✅ NO
web-01      file            /etc/nginx...   mode: 0644       mode: 0755       ❌ YES
```

**"Remediate All" button** — triggers POST /drift/reports/{id}/remediate → redirects to new Run.

---

### 12. Vault `/vault`

**Purpose**: Manage secrets and credentials.

**Secrets Table**:
Columns: Name | Type | Description | Created by | Created at | Actions (Edit/Delete)
Values are never displayed — only metadata.

**Add Secret Drawer**: Name, type (ssh_key / password / token / certificate), value textarea, description.

---

### 13. Audit Log `/audit`

**Purpose**: Full audit trail of all ForgeOps actions.

**Filters**: User | Resource type | Date range

**Audit Table**:
Columns: Time | User | Action | Resource | Resource ID | IP Address

**Expandable rows**: Click to see full payload JSON.

---

### 14. Settings `/settings`

**Tabs**:
1. **Profile**: Edit own name, email, password
2. **Users** (ADMIN): User management table — create, edit roles, deactivate
3. **System**: System info, version, check for updates

---

## Global Components

### Notification Toast
WebSocket-driven. Appears top-right. Types: info / success / warning / error.

### Command Palette (Ctrl+K)
Quick navigation: "Go to node X", "Run forge Y", "View run Z"

### Breadcrumb Navigation
All pages show breadcrumb: `ForgeOps > Inventory > Nodes > web-01`

### Loading States
Skeleton loaders for all tables and cards — no raw spinners.

### Empty States
Every list/table has a designed empty state with an action button.
