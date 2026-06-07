-- Performance indexes

-- Nodes
CREATE INDEX idx_nodes_status ON nodes(status);
CREATE INDEX idx_nodes_hostname ON nodes(hostname);
CREATE INDEX idx_nodes_name ON nodes(name);

-- Node variables
CREATE INDEX idx_node_variables_node ON node_variables(node_id);

-- Groups
CREATE INDEX idx_groups_parent ON groups(parent_id);
CREATE INDEX idx_groups_name ON groups(name);

-- Group variables
CREATE INDEX idx_group_variables_group ON group_variables(group_id);

-- Node group memberships
CREATE INDEX idx_node_group_memberships_node ON node_group_memberships(node_id);
CREATE INDEX idx_node_group_memberships_group ON node_group_memberships(group_id);

-- Forges
CREATE INDEX idx_forges_name ON forges(name);
CREATE INDEX idx_forges_mode ON forges(mode);
CREATE INDEX idx_forges_created_by ON forges(created_by);

-- Forge versions
CREATE INDEX idx_forge_versions_forge ON forge_versions(forge_id);
CREATE INDEX idx_forge_versions_valid ON forge_versions(is_valid);

-- Forge group bindings
CREATE INDEX idx_forge_group_bindings_forge ON forge_group_bindings(forge_id);
CREATE INDEX idx_forge_group_bindings_group ON forge_group_bindings(group_id);
CREATE INDEX idx_forge_group_bindings_last_checked ON forge_group_bindings(last_checked_at);

-- Runs
CREATE INDEX idx_runs_status ON runs(status);
CREATE INDEX idx_runs_forge_version ON runs(forge_version_id);
CREATE INDEX idx_runs_triggered_by ON runs(triggered_by);
CREATE INDEX idx_runs_created_at ON runs(created_at DESC);

-- Run tasks
CREATE INDEX idx_run_tasks_run ON run_tasks(run_id);
CREATE INDEX idx_run_tasks_node ON run_tasks(node_id);
CREATE INDEX idx_run_tasks_status ON run_tasks(status);

-- Run logs
CREATE INDEX idx_run_logs_task ON run_logs(run_task_id);
CREATE INDEX idx_run_logs_logged_at ON run_logs(logged_at DESC);

-- Drift reports
CREATE INDEX idx_drift_reports_forge_version ON drift_reports(forge_version_id);
CREATE INDEX idx_drift_reports_group ON drift_reports(group_id);
CREATE INDEX idx_drift_reports_status ON drift_reports(status);
CREATE INDEX idx_drift_reports_created_at ON drift_reports(created_at DESC);

-- Drift items
CREATE INDEX idx_drift_items_report ON drift_items(drift_report_id);
CREATE INDEX idx_drift_items_node ON drift_items(node_id);
CREATE INDEX idx_drift_items_drifted ON drift_items(is_drifted) WHERE is_drifted = TRUE;

-- Audit events
CREATE INDEX idx_audit_events_user ON audit_events(user_id);
CREATE INDEX idx_audit_events_resource ON audit_events(resource, resource_id);
CREATE INDEX idx_audit_events_time ON audit_events(occurred_at DESC);
CREATE INDEX idx_audit_events_action ON audit_events(action);

-- Secrets
CREATE INDEX idx_secrets_name ON secrets(name);
CREATE INDEX idx_secrets_type ON secrets(secret_type);
