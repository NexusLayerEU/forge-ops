# ForgeOps — ForgeSpec DSL Specification

ForgeSpec is the YAML-based domain-specific language used to define tasks and policies in ForgeOps.

---

## File Structure

```yaml
forgespec: "1.0"           # required — DSL version
name: "my-forge"           # required — unique forge name
description: "..."         # optional
mode: policy|playbook|mixed  # default: mixed
vars:                      # optional global variables
  key: value
targets:                   # optional default targets (overridable at run time)
  groups:
    - web-servers
  nodes:
    - db-node-01
tasks:                     # required — list of tasks
  - name: "..."
    ...
policies:                  # optional — desired-state declarations (Puppet-style)
  - name: "..."
    ...
handlers:                  # optional — tasks triggered by 'notify'
  - name: "..."
    ...
```

---

## Task Definition

```yaml
tasks:
  - name: "Install nginx"           # required — human-readable name
    module: package                  # required — module name
    params:                          # module-specific parameters
      name: nginx
      state: present
    when: "{{ os_type == 'linux' }}" # optional condition (Jinja2-like)
    loop:                            # optional loop
      - nginx
      - curl
    loop_var: pkg_name              # variable name when looping
    notify:                          # trigger handler by name
      - "Restart nginx"
    tags:                            # optional tags for selective run
      - web
      - packages
    timeout: 60                      # seconds, default 30
    ignore_errors: false             # continue on failure
    become: true                     # sudo/elevation
    become_user: root
    register: result_var            # capture output to variable
```

---

## Policy Definition (Desired State)

```yaml
policies:
  - name: "Ensure nginx is running"
    resource: service
    params:
      name: nginx
      state: running
      enabled: true
    on_drift: remediate             # remediate | alert | ignore
    check_interval: 3600            # seconds between checks
    targets:
      groups:
        - web-servers
```

---

## Built-in Modules

### `package`
Manage system packages (apt, yum, dnf, chocolatey auto-detected).

```yaml
module: package
params:
  name: nginx           # required (or list)
  state: present        # present | absent | latest
  version: "1.24.0"    # optional pinned version
```

### `service`
Manage system services (systemd, init, Windows Services).

```yaml
module: service
params:
  name: nginx
  state: started        # started | stopped | restarted | reloaded
  enabled: true         # enabled at boot
```

### `file`
Manage files and directories.

```yaml
module: file
params:
  path: /etc/myapp/config.conf
  state: present        # present | absent | directory | link
  content: |            # inline content
    key=value
  src: /path/to/link    # for state: link
  owner: www-data
  group: www-data
  mode: "0644"
```

### `template`
Render a Jinja2 template to a file on the target.

```yaml
module: template
params:
  src: templates/nginx.conf.j2   # relative to forge file
  dest: /etc/nginx/nginx.conf
  owner: root
  mode: "0644"
  vars:
    server_name: "{{ hostname }}"
```

### `command`
Run a raw shell command.

```yaml
module: command
params:
  cmd: "systemctl daemon-reload"
  creates: /path/to/file     # skip if this file exists (idempotency)
  removes: /path/to/file     # skip unless this file exists
  chdir: /opt/myapp
```

### `user`
Manage OS users.

```yaml
module: user
params:
  name: deploy
  state: present          # present | absent
  groups:
    - sudo
    - docker
  shell: /bin/bash
  home: /home/deploy
  password_hash: "..."    # SHA-512 hash
  ssh_authorized_keys:
    - "ssh-rsa AAAA..."
```

### `copy`
Copy a file from the controller to the target.

```yaml
module: copy
params:
  src: files/myapp.jar       # relative path
  dest: /opt/myapp/myapp.jar
  owner: myapp
  mode: "0755"
```

### `cron`
Manage cron jobs.

```yaml
module: cron
params:
  name: "backup job"
  state: present
  minute: "0"
  hour: "3"
  job: "/opt/scripts/backup.sh"
  user: root
```

### `mount`
Manage filesystem mounts.

```yaml
module: mount
params:
  path: /mnt/data
  src: /dev/sdb1
  fstype: ext4
  opts: defaults
  state: mounted          # mounted | unmounted | present | absent
```

### `wait_for`
Wait for a condition before continuing.

```yaml
module: wait_for
params:
  host: localhost
  port: 8080
  timeout: 120
  state: started          # started | stopped | present | absent | drained
```

---

## Variable Interpolation

ForgeSpec supports `{{ variable }}` syntax throughout, resolved in this precedence order:

1. Task-level `vars`
2. Forge-level `vars`
3. Node variables (from inventory)
4. Group variables (from inventory)
5. Built-in variables

### Built-in Variables

| Variable | Description |
|---|---|
| `{{ hostname }}` | Node hostname |
| `{{ node_name }}` | Node name in ForgeOps inventory |
| `{{ os_type }}` | `linux` or `windows` |
| `{{ run_id }}` | Current run UUID |
| `{{ forge_name }}` | Current forge name |
| `{{ forge_version }}` | Current forge version number |
| `{{ env.VAR }}` | Environment variable from ForgeOps server |

---

## Conditionals

```yaml
when: "{{ os_type == 'linux' }}"
when: "{{ ansible_distribution == 'Ubuntu' and version >= '22.04' }}"
when: "{{ result_var.exit_code == 0 }}"
```

Supported operators: `==`, `!=`, `>`, `<`, `>=`, `<=`, `and`, `or`, `not`, `in`

---

## Handlers

Handlers run once at the end of a task block, triggered by `notify`. They run only if at least one task that notified them reported `changed`.

```yaml
handlers:
  - name: "Restart nginx"
    module: service
    params:
      name: nginx
      state: restarted
```

---

## Full Example — Mixed Mode

```yaml
forgespec: "1.0"
name: "web-server-setup"
description: "Provision and maintain web servers"
mode: mixed
vars:
  app_port: 8080

targets:
  groups:
    - web-servers

tasks:
  - name: "Install packages"
    module: package
    params:
      name: "{{ item }}"
      state: present
    loop:
      - nginx
      - curl
      - ufw

  - name: "Deploy nginx config"
    module: template
    params:
      src: templates/nginx.conf.j2
      dest: /etc/nginx/nginx.conf
    notify:
      - "Restart nginx"

  - name: "Open port 80"
    module: command
    params:
      cmd: ufw allow 80/tcp

handlers:
  - name: "Restart nginx"
    module: service
    params:
      name: nginx
      state: restarted

policies:
  - name: "nginx must be running"
    resource: service
    params:
      name: nginx
      state: running
      enabled: true
    on_drift: remediate
    check_interval: 300
```

---

## Validation Rules

The ForgeSpec parser must enforce:

1. `forgespec` version field present and supported
2. `name` is unique across all forges
3. All `module` values reference known built-in modules
4. All `{{ variable }}` references are resolvable at parse time or flagged as runtime-only
5. `targets` reference groups/nodes that exist in inventory (warning, not error)
6. `notify` references must resolve to a handler name in `handlers`
7. `when` expressions must be syntactically valid
8. `loop` must be a list; `loop_var` must be a valid identifier
9. `timeout` must be a positive integer
10. Policy `on_drift` must be one of `remediate | alert | ignore`
