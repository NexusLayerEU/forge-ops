# ForgeOps — Product Overview

## Vision

ForgeOps is an infrastructure automation platform that unifies two paradigms:

| Paradigm | Origin | ForgeOps Implementation |
|---|---|---|
| **Push / Agentless** | Ansible | SSH/WinRM execution on-demand, no daemon on targets |
| **Declarative Desired State** | Puppet | ForgeSpec policies define what nodes *should* be, engine enforces convergence |

The result: engineers write **ForgeSpec** files (YAML-based DSL) that describe desired infrastructure state. ForgeOps pushes enforcement tasks directly to nodes without requiring any agent installation, while continuously tracking drift between actual and desired state.

---

## Core Concepts

### ForgeSpec (Policy/Playbook DSL)
A YAML file that declares desired state. It has two modes:

- **Policy mode** (Puppet-style): "These nodes must always be in this state. Drift triggers auto-remediation."
- **Playbook mode** (Ansible-style): "Run these tasks once, in order, against these targets."

A single ForgeSpec file can mix both modes in different blocks.

### Forge (Compiled Policy Unit)
A parsed, validated ForgeSpec file stored in the database. A Forge has a version history, can be pinned, and can be associated with node groups.

### Node
A managed host (Linux or Windows). Defined in the Inventory with connection parameters. Can belong to multiple Groups.

### Group
A logical collection of Nodes. Groups can be nested. ForgeSpec targets can reference Groups or individual Nodes.

### Run
A single execution event — applying a Forge (or a subset of tasks) against a set of Nodes. Runs are streamed in real time and stored with full output logs.

### Drift Report
A periodic or on-demand comparison between the declared state in applied Forges and the actual state observed on Nodes. Drift items can trigger automatic remediation Runs.

### Task
The atomic unit of work in a ForgeSpec. A task maps to a ForgeOps **module** (e.g., `package`, `service`, `file`, `user`, `command`, `template`).

### Module
A built-in executor that knows how to enforce a particular resource type on Linux or Windows. Similar to Ansible modules / Puppet resource types.

---

## Key Differentiators vs Ansible

| Feature | Ansible | ForgeOps |
|---|---|---|
| Desired state tracking | ❌ No | ✅ Yes — drift detection |
| Auto-remediation | ❌ No | ✅ Yes — configurable per policy |
| Real-time Web UI | Limited | ✅ Full streaming dashboard |
| State history | ❌ No | ✅ Full audit log in PostgreSQL |
| Mixed push+declarative in one file | ❌ No | ✅ Yes |

## Key Differentiators vs Puppet

| Feature | Puppet | ForgeOps |
|---|---|---|
| Agent required | ✅ Yes (puppet agent) | ❌ No — agentless |
| SSH-based execution | ❌ No | ✅ Yes |
| Playbook-style ordered runs | Limited | ✅ Yes |
| Modern Web UI | Legacy | ✅ React 18 |
| Self-hosted, Docker Compose | Complex | ✅ Simple `docker compose up` |

---

## User Roles

| Role | Capabilities |
|---|---|
| **Admin** | Full access — manage users, credentials, all resources |
| **Operator** | Create/edit Forges, run against any node, view all logs |
| **Viewer** | Read-only access to all resources and logs |
| **Node Manager** | Manage inventory (nodes/groups) only, no Forge execution |

---

## Terminology Glossary

| Term | Definition |
|---|---|
| ForgeSpec | The YAML DSL file format |
| Forge | A stored, versioned ForgeSpec in the database |
| Node | A managed host |
| Group | A named collection of Nodes |
| Run | An execution of a Forge against Nodes |
| Task | Atomic unit of work within a ForgeSpec |
| Module | Built-in executor for a resource type |
| Drift | Difference between declared and actual node state |
| Remediation | An auto-triggered Run to fix drift |
| Vault | Encrypted credential/secret store |
| Policy | A Forge in "declarative/desired-state" mode |
| Playbook | A Forge in "imperative/ordered-tasks" mode |
