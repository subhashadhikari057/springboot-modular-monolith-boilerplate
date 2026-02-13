# Frontend User Module Integration

## Base
- Base path: `/api/users`
- All endpoints require authenticated cookies (`sid`/`rid`) and `credentials: 'include'`.
- Access is permission-based; if user lacks authority, backend returns `403`.

Required authorities by endpoint:
- `POST /api/users`: `user:create`
- `GET /api/users/{id}`: `user:read`
- `GET /api/users`: `user:read`
- `PATCH /api/users/{id}`: `user:update`
- `PATCH /api/users/{id}/status`: `user:update-status`
- `PUT /api/users/{id}/role`: `user:update-role`
- `GET /api/users/{id}/permissions`: `user:read-permissions`
- `POST /api/users/{id}/password/reset/request`: `user:reset-password-request`
- `DELETE /api/users/{id}`: `user:delete`

## Endpoints

### 1) Create User
- `POST /api/users`
- Request:
```json
{
  "name": "Jane Doe",
  "email": "jane@example.com",
  "password": "Str0ngP@ssword",
  "phone": "+1-415-555-0132",
  "image": "https://cdn.example.com/avatars/jane.png",
  "roleId": 3
}
```
- Success: `201` with `Location: /api/users/{id}` and user payload.
- If `roleId` omitted, backend assigns default `USER` role.

### 2) Get User By Id
- `GET /api/users/{id}`
- Success: `200` with `UserResponse`.
- Not found: `404`.

### 3) List Users (Paginated + Filtered)
- `GET /api/users`
- Query params:
  - `page` (default `1`, min `1`)
  - `size` (default `20`, range `1..100`)
  - `sortBy` (`id|name|email|createdAt|updatedAt`, default `createdAt`)
  - `sortDir` (`asc|desc`, default `desc`)
  - `q` (optional text search over `name` and `email`)
  - `roleId` (optional)
  - `emailVerified` (optional `true|false`)

Example:
`/api/users?page=1&size=20&sortBy=createdAt&sortDir=desc&roleId=2&emailVerified=true`

Response shape:
```json
{
  "items": [
    {
      "id": "6cfb19a7-71a3-46a8-b1d8-3de77bcd9b61",
      "name": "Jane Doe",
      "email": "jane@example.com",
      "emailVerified": true,
      "phone": "+1-415-555-0132",
      "phoneVerified": false,
      "image": "https://cdn.example.com/avatars/jane.png",
      "role": {
        "id": 2,
        "name": "ADMIN"
      },
      "createdAt": "2026-02-12T10:00:00Z",
      "updatedAt": "2026-02-12T10:00:00Z"
    }
  ],
  "page": {
    "page": 1,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

### 4) Update User Role
- `PUT /api/users/{id}/role`
- Request:
```json
{
  "roleId": 2
}
```
- Success: `200` with updated `UserResponse`.
- Side effect: active sessions for that user are revoked.

### 5) Delete User
- `DELETE /api/users/{id}`
- Success: `204` (no body).

### 6) Update User (Patch)
- `PATCH /api/users/{id}`
- Request fields are optional:
```json
{
  "name": "Updated Name",
  "phone": "+1-415-555-9999",
  "image": "https://cdn.example.com/avatars/new.png",
  "roleId": 2,
  "emailVerified": true,
  "phoneVerified": false,
  "status": "ACTIVE"
}
```
- If `roleId` changes, backend revokes active sessions for that user.

### 7) Update User Status
- `PATCH /api/users/{id}/status`
```json
{
  "status": "LOCKED"
}
```
- On non-`ACTIVE` status, backend revokes active sessions.

### 8) Get User Permissions
- `GET /api/users/{id}/permissions`
- Returns effective permissions from user role.

### 9) Get My Permissions
- `GET /api/users/me/permissions`
- Returns effective permissions for currently logged-in user.

### 10) Admin Request Password Reset
- `POST /api/users/{id}/password/reset/request`
- Triggers password reset email for that user.
- Response:
```json
{
  "message": "password_reset_requested"
}
```

## Real Frontend Technique
1. Build one typed `usersApi` client with methods: `create`, `getById`, `list`, `updateRole`, `remove`.
2. Keep list state as: `items + pageMeta + filters + loading + error`.
3. On table filter/sort/page change, call `GET /api/users` with query params (debounce text filters if added later).
4. If `401`, follow shared auth refresh strategy from auth integration doc and retry once.
5. Handle `403` by disabling hidden actions (create/update-role/delete) based on frontend permission model.

## Error Handling
- `400`: invalid query/body (show inline validation message)
- `401`: unauthenticated (refresh or redirect login)
- `403`: authenticated but no permission
- `404`: target user/role not found
- `409`: create conflict (email already in use)
