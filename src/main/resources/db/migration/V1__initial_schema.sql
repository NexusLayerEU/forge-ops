-- ForgeOps Initial Schema
-- Tables ordered to satisfy foreign key constraints

-- Users (no dependencies)
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username        VARCHAR(64) UNIQUE NOT NULL,
    email           VARCHAR(255) UNIQUE NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(255),
    is_active       BOOLEAN DEFAULT TRUE,
    must_change_password BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE user_roles (
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    role    VARCHAR(32) NOT NULL CHECK (role IN ('ADMIN','OPERATOR','VIEWER','NODE_MANAGER')),
    PRIMARY KEY (user_id, role)
);

-- Secrets (no dependencies — nodes reference this)
CREATE TABLE secrets (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) UNIQUE NOT NULL,
    secret_type     VARCHAR(32) CHECK (secret_type IN ('ssh_key','password','token','certificate')),
    encrypted_value BYTEA NOT NULL,
    description     TEXT,
    created_by      UUID REFERENCES users(id),
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

-- Nodes (references secrets)
CREATE TABLE nodes (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) UNIQUE NOT NULL,
    hostname        VARCHAR(255) NOT NULL,
    port            INT DEFAULT 22,
    os_type         VARCHAR(16) CHECK (os_type IN ('linux','windows')) DEFAULT 'linux',
    connection_type VARCHAR(8) CHECK (connection_type IN ('ssh','winrm')) DEFAULT 'ssh',
    credential_id   UUID REFERENCES secrets(id),
    description     TEXT,
    tags            JSONB DEFAULT '{}',
    last_seen_at    TIMESTAMPTZ,
    status          VARCHAR(16) DEFAULT 'unknown'
                      CHECK (status IN ('reachable','unreachable','unknown')),
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE node_variables (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    node_id     UUID REFERENCES nodes(id) ON DELETE CASCADE,
    key         VARCHAR(255) NOT NULL,
    value       TEXT NOT NULL,
    is_secret   BOOLEAN DEFAULT FALSE,
    UNIQUE (node_id, key)
);

-- Groups (self-referencing for parent/child hierarchy)
CREATE TABLE groups (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) UNIQUE NOT NULL,
    description TEXT,
    parent_id   UUID REFERENCES groups(id),
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    updated_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE group_variables (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id  UUID REFERENCES groups(id) ON DELETE CASCADE,
    key       VARCHAR(255) NOT NULL,
    value     TEXT NOT NULL,
    is_secret BOOLEAN DEFAULT FALSE,
    UNIQUE (group_id, key)
);

CREATE TABLE node_group_memberships (
    node_id   UUID REFERENCES nodes(id) ON DELETE CASCADE,
    group_id  UUID REFERENCES groups(id) ON DELETE CASCADE,
    PRIMARY KEY (node_id, group_id)
);

-- Forges
CREATE TABLE forges (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) UNIQUE NOT NULL,
    description     TEXT,
    mode            VARCHAR(16) CHECK (mode IN ('policy','playbook','mixed')) DEFAULT 'mixed',
    pinned_version  INT,
    created_by      UUID REFERENCES users(id),
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE forge_versions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    forge_id    UUID REFERENCES forges(id) ON DELETE CASCADE,
    version     INT NOT NULL,
    content     TEXT NOT NULL,
    checksum    VARCHAR(64) NOT NULL,
    is_valid    BOOLEAN DEFAULT FALSE,
    errors      JSONB DEFAULT '[]',
    created_by  UUID REFERENCES users(id),
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (forge_id, version)
);

CREATE TABLE forge_group_bindings (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    forge_id        UUID REFERENCES forges(id) ON DELETE CASCADE,
    group_id        UUID REFERENCES groups(id) ON DELETE CASCADE,
    auto_remediate  BOOLEAN DEFAULT FALSE,
    check_interval  INT DEFAULT 3600,
    last_checked_at TIMESTAMPTZ,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (forge_id, group_id)
);

-- Runs
CREATE TABLE runs (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    forge_version_id UUID REFERENCES forge_versions(id),
    triggered_by     UUID REFERENCES users(id),
    trigger_type     VARCHAR(16) CHECK (trigger_type IN ('manual','scheduled','remediation','drift')),
    status           VARCHAR(16) DEFAULT 'pending'
                       CHECK (status IN ('pending','running','success','failed','partial','cancelled')),
    target_nodes     UUID[] NOT NULL,
    started_at       TIMESTAMPTZ,
    completed_at     TIMESTAMPTZ,
    summary          JSONB DEFAULT '{}',
    created_at       TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE run_tasks (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_id       UUID REFERENCES runs(id) ON DELETE CASCADE,
    node_id      UUID REFERENCES nodes(id),
    task_index   INT NOT NULL,
    task_name    VARCHAR(255) NOT NULL,
    module       VARCHAR(64) NOT NULL,
    status       VARCHAR(16) DEFAULT 'pending'
                   CHECK (status IN ('pending','running','ok','changed','failed','skipped')),
    started_at   TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    exit_code    INT,
    UNIQUE (run_id, node_id, task_index)
);

CREATE TABLE run_logs (
    id          BIGSERIAL PRIMARY KEY,
    run_task_id UUID REFERENCES run_tasks(id) ON DELETE CASCADE,
    line_no     INT NOT NULL,
    level       VARCHAR(8) CHECK (level IN ('info','warn','error','debug')),
    message     TEXT NOT NULL,
    logged_at   TIMESTAMPTZ DEFAULT NOW()
);

-- Drift
CREATE TABLE drift_reports (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    forge_version_id UUID REFERENCES forge_versions(id),
    group_id         UUID REFERENCES groups(id),
    status           VARCHAR(16) CHECK (status IN ('pending','running','complete','error')),
    total_nodes      INT DEFAULT 0,
    drifted_nodes    INT DEFAULT 0,
    created_at       TIMESTAMPTZ DEFAULT NOW(),
    completed_at     TIMESTAMPTZ
);

CREATE TABLE drift_items (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    drift_report_id    UUID REFERENCES drift_reports(id) ON DELETE CASCADE,
    node_id            UUID REFERENCES nodes(id),
    resource_type      VARCHAR(64) NOT NULL,
    resource_name      VARCHAR(255) NOT NULL,
    expected_state     JSONB NOT NULL,
    actual_state       JSONB NOT NULL,
    is_drifted         BOOLEAN NOT NULL,
    remediated         BOOLEAN DEFAULT FALSE,
    remediation_run_id UUID REFERENCES runs(id)
);

-- Audit
CREATE TABLE audit_events (
    id          BIGSERIAL PRIMARY KEY,
    user_id     UUID REFERENCES users(id),
    action      VARCHAR(64) NOT NULL,
    resource    VARCHAR(64) NOT NULL,
    resource_id VARCHAR(255),
    payload     JSONB DEFAULT '{}',
    ip_address  VARCHAR(64),
    occurred_at TIMESTAMPTZ DEFAULT NOW()
);
