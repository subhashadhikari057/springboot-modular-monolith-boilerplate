# Frontend Auth Module Integration

## Base
- Base path: `/api/auth`
- Auth model: cookie-based (`sid`, `rid`)
- Client requirement: send credentials on every auth-protected request (`credentials: 'include'`)

## Cookie Behavior
- `sid`: short-lived access session cookie (default 15 minutes)
- `rid`: refresh cookie (default 7 days)
- Cookies are set/rotated by backend on:
  - `POST /api/auth/login`
  - `POST /api/auth/register`
  - `POST /api/auth/refresh`
- Cookies are cleared by backend on:
  - `POST /api/auth/logout`
  - `POST /api/auth/password/change`

## Endpoint Integration

### 1) Register
- `POST /api/auth/register`
- Request:
```json
{
  "name": "Jane Doe",
  "email": "jane@example.com",
  "password": "StrongPass123!"
}
```
- Success: `201`, sets `sid` + `rid`, returns authenticated user payload.

### 2) Login
- `POST /api/auth/login`
- Request:
```json
{
  "email": "jane@example.com",
  "password": "StrongPass123!"
}
```
- Success: `200`, sets `sid` + `rid`.

### 3) Current User
- `GET /api/auth/me`
- Uses `sid` cookie automatically.
- Success: `200` user profile.
- If unauthenticated: `401`.

### 4) Update My Profile
- `PATCH /api/auth/me`
- Send changed profile fields only.
- Requires valid session.

### 5) Refresh Session
- `POST /api/auth/refresh`
- Uses `rid` cookie.
- Success: rotates session and re-sets both cookies.
- Frontend use: call when protected request fails with `401` once, then retry original request.

Brief frontend implementation:
1. User calls any protected API with `credentials: 'include'`.
2. If response is `401`, frontend calls `POST /api/auth/refresh` (also with credentials).
3. If refresh succeeds (`200`), backend sends new cookies automatically.
4. Frontend retries the original protected request once.
5. If refresh fails (`401`), clear auth state and redirect user to login.


## Real Implementation Technique (Frontend)
Use one centralized HTTP client wrapper (or interceptor), not refresh logic in every page.

Example approach:
1. Always send requests with credentials enabled.
2. In response interceptor/wrapper:
   - if status is not `401`, return normally.
   - if status is `401` and request has not retried yet:
     - call `POST /api/auth/refresh`
     - mark original request as retried
     - replay original request once
   - if refresh fails, redirect to login.
3. Prevent parallel refresh storms:
   - keep one shared `refreshPromise`
   - if multiple requests get `401` together, all wait for same refresh call.
4. Keep auth state in memory/store (`isAuthenticated`, `me`), but never store `sid`/`rid` in JS storage.

### 6) Logout
- `POST /api/auth/logout`
- Backend invalidates session and clears cookies.
- Frontend should clear local user state and redirect to login.

### 7) Change Password (Logged-in)
- `POST /api/auth/password/change`
- Request:
```json
{
  "currentPassword": "OldPass123!",
  "newPassword": "NewStrongPass123!"
}
```
- Success: backend revokes sessions and clears cookies.
- Frontend: force re-login.

### 8) Request Verification
- `POST /api/auth/verify/request`
- Request:
```json
{
  "purpose": "EMAIL_VERIFICATION",
  "channel": "EMAIL"
}
```
- Backend sends verification email.
- Response includes verification metadata.
- Token field behavior:
  - Dev/local: may include plain token (testing)
  - Prod: token is hidden (`null`)

### 9) Confirm Verification
- `POST /api/auth/verify/confirm`
- Request:
```json
{
  "identifier": "<identifier-from-link>",
  "purpose": "EMAIL_VERIFICATION",
  "channel": "EMAIL",
  "token": "<token-from-link>"
}
```
- Used by verify-link page in frontend.

### 10) Forgot Password
- `POST /api/auth/password/forgot`
- Request:
```json
{
  "email": "jane@example.com"
}
```
- Backend sends reset email if account exists.
- Always returns generic success message.
- Token field behavior:
  - Dev/local: may include token
  - Prod: token is hidden (`null`)

### 11) Reset Password
- `POST /api/auth/password/reset`
- Request:
```json
{
  "identifier": "<identifier-from-link>",
  "token": "<token-from-link>",
  "newPassword": "NewStrongPass123!"
}
```
- Frontend flow:
  1. User clicks reset link from email.
  2. Reset page reads `identifier` and `token` from query params.
  3. User enters new password.
  4. Frontend calls this endpoint.

## Recommended Frontend Flow
1. App start: call `GET /api/auth/me`.
2. If `401`: show logged-out state.
3. For protected API `401`: call `POST /api/auth/refresh` once, retry original request once.
4. If refresh fails: redirect to login.

## Error Handling
- Validation issues: `400`
- Unauthorized/expired session: `401`
- Conflict (for example duplicate email): `409`
- Show generic user-safe messages for auth failures; avoid leaking sensitive details.
