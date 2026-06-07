# ForgeOps — API Specification

Base URL: `http://localhost:8080/api/v1`
Auth: Bearer JWT in `Authorization` header (all endpoints except `/auth/**`)

---

## Auth

### POST /auth/login
```json
Request:  { "username": "admin", "password": "..." }
Response: { "token": "eyJ...", "expiresIn": 86400, "user": { "id": "...", "username": "...", "roles": ["ADMIN"] } }
```

### POST /auth/refresh
```json
Request:  { "token": "eyJ..." }
Response: { "token": "eyJ...", "expiresIn": 86400 }
```

### GET /auth/me
```json
Response: { "id": "...", "username": "...", "email": "...", "roles": [...] }
```

---

## Nodes

### GET /nodes
Query params: `page`, `size`, `search`, `status`, `groupId`
```json
Response: {
  "content": [ { "id": "...", "name": "...", "hostname": "...", "status": "reachable", "osType": "linux", ... } ],
  "totalElements": 42, "page": 0, "size": 20
}
```

### POST /nodes
```json
Request: {
  "name": "web-01", "hostname": "192.168.1.10", "port": 22,
  "osType": "linux", "connectionType": "ssh",
  "credentialId": "uuid", "description": "...", "tags": {}
}
Response: Node object
```

### GET /nodes/{id}
### PUT /nodes/{id}
### DELETE /nodes/{id}

### POST /nodes/{id}/ping
Test connectivity to node.
```json
Response: { "reachable": true, "latencyMs": 12 }
```

### GET /nodes/{id}/variables
### POST /nodes/{id}/variables
```json
Request: { "key": "app_version", "value": "2.1.0", "isSecret": false }
```
### DELETE /nodes/{id}/variables/{variableId}

---

## Groups

### GET /groups
### POST /groups
```json
Request: { "name": "web-servers", "description": "...", "parentId": null }
```
### GET /groups/{id}
### PUT /groups/{id}
### DELETE /groups/{id}

### POST /groups/{id}/members
```json
Request: { "nodeIds": ["uuid1", "uuid2"] }
```
### DELETE /groups/{id}/members/{nodeId}

### GET /groups/{id}/variables
### POST /groups/{id}/variables
### DELETE /groups/{id}/variables/{variableId}

---

## Forges

### GET /forges
Query: `page`, `size`, `search`, `mode`
### POST /forges
```json
Request: {
  "name": "web-server-setup",
  "description": "...",
  "mode": "mixed",
  "content": "forgespec: '1.0'\nname: ..."
}
Response: { "id": "...", "name": "...", "latestVersion": 1, "isValid": true, "errors": [] }
```
### GET /forges/{id}
### PUT /forges/{id}  — creates a new version
### DELETE /forges/{id}

### GET /forges/{id}/versions
### GET /forges/{id}/versions/{version}

### POST /forges/{id}/validate
```json
Request: { "content": "forgespec: ..." }
Response: { "valid": true, "errors": [], "warnings": [] }
```

### GET /forges/{id}/bindings
### POST /forges/{id}/bindings
```json
Request: { "groupId": "uuid", "autoRemediate": true, "checkInterval": 3600 }
```
### DELETE /forges/{id}/bindings/{bindingId}

---

## Runs

### GET /runs
Query: `page`, `size`, `status`, `forgeId`, `triggeredBy`, `from`, `to`

### POST /runs
Trigger a new run.
```json
Request: {
  "forgeId": "uuid",
  "version": 3,           // optional, defaults to pinned or latest
  "targetNodes": ["uuid1", "uuid2"],  // optional, overrides forge targets
  "targetGroups": ["uuid"],
  "tags": ["web"]         // optional — run only tasks with these tags
}
Response: { "runId": "uuid", "status": "pending" }
```

### GET /runs/{id}
```json
Response: {
  "id": "...", "status": "running", "forge": {...},
  "targetNodes": [...], "tasks": [...], "startedAt": "...", "completedAt": null,
  "summary": { "ok": 5, "changed": 2, "failed": 0, "skipped": 1 }
}
```

### DELETE /runs/{id}   — cancel a running run

### GET /runs/{id}/tasks
### GET /runs/{id}/tasks/{taskId}
### GET /runs/{id}/tasks/{taskId}/logs

### GET /runs/{id}/output
Full merged output as plain text (for download/export).

---

## Drift

### GET /drift/reports
Query: `page`, `size`, `forgeId`, `groupId`

### POST /drift/reports
Trigger a drift check.
```json
Request: { "forgeId": "uuid", "groupId": "uuid" }
Response: { "reportId": "uuid", "status": "pending" }
```

### GET /drift/reports/{id}
### GET /drift/reports/{id}/items
```json
Response: [
  {
    "nodeId": "...", "nodeName": "...",
    "resourceType": "service", "resourceName": "nginx",
    "expectedState": { "state": "running" },
    "actualState": { "state": "stopped" },
    "isDrifted": true, "remediated": false
  }
]
```

### POST /drift/reports/{id}/remediate
Trigger remediation run for all drifted items.
```json
Response: { "runId": "uuid" }
```

---

## Secrets (Vault)

### GET /vault/secrets
```json
Response: [ { "id": "...", "name": "...", "secretType": "ssh_key", "createdAt": "..." } ]
// Note: encrypted values never returned
```

### POST /vault/secrets
```json
Request: { "name": "prod-ssh-key", "secretType": "ssh_key", "value": "-----BEGIN..." , "description": "..." }
```

### PUT /vault/secrets/{id}
### DELETE /vault/secrets/{id}

---

## Users

### GET /users  (ADMIN only)
### POST /users
```json
Request: { "username": "...", "email": "...", "password": "...", "fullName": "...", "roles": ["OPERATOR"] }
```
### GET /users/{id}
### PUT /users/{id}
### DELETE /users/{id}

---

## Audit

### GET /audit
Query: `page`, `size`, `userId`, `resource`, `from`, `to`
```json
Response: {
  "content": [
    { "id": 1, "user": "admin", "action": "RUN_CREATED", "resource": "run", "resourceId": "uuid", "occurredAt": "..." }
  ]
}
```

---

## WebSocket (STOMP)

Connect to: `ws://localhost:8080/ws`

### Subscribe to run output
```javascript
client.subscribe('/topic/runs/{runId}/output', (message) => {
  const line = JSON.parse(message.body);
  // { taskId, nodeId, nodeName, lineNo, level, message, loggedAt }
});
```

### Subscribe to run status
```javascript
client.subscribe('/topic/runs/{runId}/status', (message) => {
  const status = JSON.parse(message.body);
  // { runId, status, summary }
});
```

### Subscribe to drift results
```javascript
client.subscribe('/topic/drift/{reportId}', (message) => {
  const item = JSON.parse(message.body);
  // drift item as the check progresses node by node
});
```

### Subscribe to notifications
```javascript
client.subscribe('/topic/notifications', (message) => {
  const notification = JSON.parse(message.body);
  // { type, severity, title, message, resourceId }
});
```

---

## Error Responses

All error responses follow:
```json
{
  "error": "NOT_FOUND",
  "message": "Node with id '...' not found",
  "timestamp": "2026-01-01T00:00:00Z",
  "path": "/api/v1/nodes/..."
}
```

HTTP status codes:
- `400` Bad Request (validation error)
- `401` Unauthorized
- `403` Forbidden (RBAC)
- `404` Not Found
- `409` Conflict (duplicate name)
- `422` Unprocessable Entity (ForgeSpec validation failure)
- `500` Internal Server Error
