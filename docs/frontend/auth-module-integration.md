# Frontend Auth Module Integration

## Base
- Base path: `/api/mobile/auth`
- Auth model: cookie-based (`sid`, `rid`)
- Client requirement: send credentials on every auth-protected request (`credentials: 'include'`)

## Cookie Behavior
- `sid`: short-lived access session cookie (default 15 minutes)
- `rid`: refresh cookie (default 7 days)
- Cookies are set/rotated by backend on:
  - `POST /api/mobile/auth/login`
  - `POST /api/mobile/auth/register`
  - `POST /api/mobile/auth/refresh`
- Cookies are cleared by backend on:
  - `POST /api/mobile/auth/logout`
  - `POST /api/mobile/auth/password/change`

## Endpoint Integration

### 1) Register
- `POST /api/mobile/auth/register`
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
- `POST /api/mobile/auth/login`
- Request:
```json
{
  "email": "jane@example.com",
  "password": "StrongPass123!"
}
```
- Success: `200`, sets `sid` + `rid`.

### 3) Current User
- `GET /api/mobile/auth/me`
- Uses `sid` cookie automatically.
- Success: `200` user profile.
- If unauthenticated: `401`.

### 4) Update My Profile
- `PATCH /api/mobile/auth/me`
- Send changed profile fields only.
- Requires valid session.

### 5) Refresh Session
- `POST /api/mobile/auth/refresh`
- Uses `rid` cookie.
- Success: rotates session and re-sets both cookies.
- Frontend use: call when protected request fails with `401` once, then retry original request.

Brief frontend implementation:
1. User calls any protected API with `credentials: 'include'`.
2. If response is `401`, frontend calls `POST /api/mobile/auth/refresh` (also with credentials).
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
     - call `POST /api/mobile/auth/refresh`
     - mark original request as retried
     - replay original request once
   - if refresh fails, redirect to login.
3. Prevent parallel refresh storms:
   - keep one shared `refreshPromise`
   - if multiple requests get `401` together, all wait for same refresh call.
4. Keep auth state in memory/store (`isAuthenticated`, `me`), but never store `sid`/`rid` in JS storage.

### 6) Logout
- `POST /api/mobile/auth/logout`
- Backend invalidates session and clears cookies.
- Frontend should clear local user state and redirect to login.

### 6.1) List Active Sessions
- `GET /api/mobile/auth/sessions`
- Returns all active sessions/devices for current user.
- Use this in "Logged in devices" UI.

### 6.2) Revoke One Session
- `DELETE /api/mobile/auth/sessions/{sessionId}`
- Revokes selected device/session.
- If user revokes current session, backend also clears cookies and frontend should redirect to login.

### 6.3) Logout All Sessions
- `POST /api/mobile/auth/logout-all`
- Revokes all sessions for current user and clears cookies.
- Frontend should force logged-out state.

### 7) Change Password (Logged-in)
- `POST /api/mobile/auth/password/change`
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
- `POST /api/mobile/auth/verify/request`
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

### 8.1) Resend Verification
- `POST /api/mobile/auth/verify/resend`
- Same request body as `/verify/request`.
- Has cooldown (default 1 minute). If called too quickly, backend returns `400`.

### 9) Confirm Verification
- `POST /api/mobile/auth/verify/confirm`
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
- `POST /api/mobile/auth/password/forgot`
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
- `POST /api/mobile/auth/password/reset`
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

### 12) Re-authenticate (Sensitive Action Guard)
- `POST /api/mobile/auth/reauth`
- Request:
```json
{
  "password": "CurrentPassword123!"
}
```
- Use before sensitive actions (for example email change, account deletion, critical profile updates).

### 13) Request Account Deletion Verification
- `POST /api/mobile/auth/account/delete/request`
- Sends deletion verification email to current user.
- Cooldown applies (default 1 minute).

### 14) Delete My Account (Verified)
- `POST /api/mobile/auth/account/delete`
- Request:
```json
{
  "token": "<token-from-deletion-email>"
}
```
- On success, backend deletes user account and clears cookies.
- Frontend should clear app state and redirect to public/login page.

## Recommended Frontend Flow
1. App start: call `GET /api/mobile/auth/me`.
2. If `401`: show logged-out state.
3. For protected API `401`: call `POST /api/mobile/auth/refresh` once, retry original request once.
4. If refresh fails: redirect to login.

## Error Handling
- Validation issues: `400`
- Unauthorized/expired session: `401`
- Conflict (for example duplicate email): `409`
- Show generic user-safe messages for auth failures; avoid leaking sensitive details.
