# Frontend User Module Integration

## Base
- Admin user-management base path: `/api/admin/users`
- Mobile user-self base path: `/api/mobile/users`
- All endpoints require authenticated cookies (`sid`/`rid`) and `credentials: 'include'`.
- Admin endpoints are permission-protected (`403` if missing authority).

## Admin Authorities by Endpoint
- `POST /api/admin/users`: `users:create`
- `GET /api/admin/users/{id}`: `users:read`
- `GET /api/admin/users`: `users:read`
- `PATCH /api/admin/users/{id}`: `users:update`
- `PATCH /api/admin/users/{id}/status`: `users:manage`
- `PUT /api/admin/users/{id}/role`: `users:manage`
- `GET /api/admin/users/{id}/permissions`: `users:manage`
- `POST /api/admin/users/{id}/password/reset/request`: `users:manage`
- `DELETE /api/admin/users/{id}`: `users:delete`

## Admin Endpoints

### 1) Create User
- `POST /api/admin/users`
- Success: `201` with `Location: /api/admin/users/{id}` and user payload.

### 2) Get User By Id
- `GET /api/admin/users/{id}`

### 3) List Users (Paginated + Filtered)
- `GET /api/admin/users`
- Query params:
  - `page` (default `1`, min `1`)
  - `size` (default `20`, range `1..100`)
  - `sortBy` (`id|name|email|createdAt|updatedAt`, default `createdAt`)
  - `sortDir` (`asc|desc`, default `desc`)
  - `q` (optional text search over `name` and `email`)
  - `roleId` (optional)
  - `emailVerified` (optional `true|false`)

Example:
`/api/admin/users?page=1&size=20&sortBy=createdAt&sortDir=desc&roleId=2&emailVerified=true`

### 4) Update User Role
- `PUT /api/admin/users/{id}/role`

### 5) Update User (Patch)
- `PATCH /api/admin/users/{id}`
- Optional fields: `name`, `phone`, `image`, `roleId`, `emailVerified`, `phoneVerified`, `status`

### 6) Update User Status
- `PATCH /api/admin/users/{id}/status`
- Body:
```json
{
  "status": "LOCKED"
}
```

### 7) Get User Permissions
- `GET /api/admin/users/{id}/permissions`

### 8) Request User Password Reset
- `POST /api/admin/users/{id}/password/reset/request`
- Response:
```json
{
  "message": "password_reset_requested"
}
```

### 9) Delete User
- `DELETE /api/admin/users/{id}`
- Success: `204`

### 10) List Audit Logs
- `GET /api/admin/audit-logs`
- Permission: `users:manage`
- Query params:
  - `page` (default `1`, min `1`)
  - `size` (default `20`, range `1..100`)
  - `actorUserId` (optional UUID)
  - `action` (optional exact action key)
  - `resourceType` (optional)
  - `resourceId` (optional)
  - `result` (optional `SUCCESS|FAILURE`)
  - `from` / `to` (optional ISO datetime)

## Mobile User-Self Endpoint

### 1) Get My Permissions
- `GET /api/mobile/users/me/permissions`
- Returns effective permissions for current authenticated user.

### 2) Get My Audit Logs
- `GET /api/mobile/users/me/audit-logs`
- Returns only current user's audit events.
- Query params:
  - `page` (default `1`, min `1`)
  - `size` (default `20`, range `1..100`)
  - `action` (optional exact action key)
  - `result` (optional `SUCCESS|FAILURE`)
  - `from` / `to` (optional ISO datetime)

## Real Frontend Technique
1. Keep `adminUsersApi` separate from mobile self API clients.
2. Admin UI calls only `/api/admin/users/*`.
3. Mobile/self UI calls `/api/mobile/users/me/permissions` for route/feature guards.
4. For `401`, follow auth refresh strategy from auth integration doc.
5. For `403`, hide/disable admin actions in UI.

## Error Handling
- `400`: invalid query/body
- `401`: unauthenticated
- `403`: authenticated but no permission
- `404`: target user/role not found
- `409`: create conflict (email already in use)
