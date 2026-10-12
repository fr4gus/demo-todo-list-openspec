# Design

## Context

Builds on `add-platform-scaffold`: problem-details errors, CORS for `FRONTEND_ORIGIN`, Flyway, Redis, Mailpit and the base RTK Query slice already exist. Behavior is in the three specs of this change; this document covers how.

## Goals / Non-Goals

**Goals:**
- Stateless auth that downstream changes consume through a role claim and a current-user accessor.
- Password reset that is safe by default (hashed tokens, expiry, single use, rate limit).

**Non-Goals:**
- Refresh tokens, token revocation, multi-factor auth, social login, email verification at sign-up, account lockout.
- Admin management of users (the admin overview change is read-only).

## Decisions

**Data model (Flyway migration).**
- `users`: `id` UUID PK, `full_name`, `email` (stored lowercase, unique index), `password_hash`, `role` (`USER`/`ADMIN`, check constraint), audit columns.
- `password_reset_tokens`: `id` UUID PK, `user_id` FK -> `users` (indexed, `ON DELETE CASCADE`), `token_hash` (unique), `expires_at`, `used_at`, audit columns.
Emails are normalized to lowercase on write and lookup, which gives case-insensitive uniqueness without a functional index.

**JWT.** HS256 with `JWT_SECRET` (minimum 32 bytes, validated at startup), claims `sub` = user id, `role`, `iat`, `exp` = iat + 24h. A `OncePerRequestFilter` validates the token, loads the user id from `sub` and checks the account still exists, then sets the `SecurityContext` with authority `ROLE_<role>`. Alternative: asymmetric keys; rejected because only this service issues and verifies tokens.

**Distinct login errors.** The product spec requires different messages for unknown email and wrong password, so the service throws two exceptions with codes `EMAIL_NOT_FOUND` and `INVALID_PASSWORD`. This allows account enumeration at sign-in; accepted because the spec asks for it, and mitigated by keeping the reset-request response uniform. A constant-time compare is used for passwords either way.

**Authorization.** Method security (`@PreAuthorize("hasRole('ADMIN')")`) on controllers plus URL rules (`/api/v1/auth/**` permit-all, everything else authenticated). Feature changes declare their own role requirements.

**Password reset.** Token = 32 random bytes, URL-safe Base64; DB stores SHA-256 of it (the token is already high-entropy, so a fast hash is appropriate and enables lookup by hash). Requesting a new reset marks older unused tokens used. Confirm runs in one transaction: validate, set new BCrypt hash, set `used_at`. Rate limit: Redis counter keyed by lowercased email with a 1-hour TTL (`INCR` + `EXPIRE`), limit 5. Email through Spring Mail (`JavaMailSender`), link `FRONTEND_ORIGIN/reset-password?token=...`, sent after commit so a mail failure cannot leave a half-created state; failures are logged, and the API response stays `202`.

**Admin seeding.** An `ApplicationRunner` implementing the rules in the `user-accounts` spec: all-or-nothing validation of the three variables, skip when any admin exists, fail when the email belongs to a `USER`.

**Frontend.**
- `features/auth/`: `authSlice` (token, expiresAt, user), `authApi` injected into the base API (login, register, requestReset, confirmReset), screens `SignInPage`, `SignUpPage`, `ForgotPasswordPage`, `ResetPasswordPage`.
- Token persisted in `localStorage` with expiry; the slice hydrates on load and drops an expired token. A base-query wrapper dispatches `loggedOut` on any 401.
- `ProtectedRoute` takes an optional `role`; a post-login redirect sends `USER` to `/` and `ADMIN` to `/admin/users`, and each role is redirected away from the other's area.
- `localStorage` is readable by injected scripts; accepted for this scope and mitigated by a strict CSP in the nginx config being noted as a later hardening item.

## Risks / Trade-offs

- [Stolen 24h token cannot be revoked; reset does not invalidate tokens] → Accepted for v1; a `password_changed_at` claim check or refresh tokens can be added later without API breakage.
- [Sign-in messages enable email enumeration] → Required by product spec; rate limiting on sign-in is a possible follow-up.
- [Reset email delivery depends on SMTP] → Mailpit locally; production SMTP config documented; delivery failures logged.
- [Admin seed password lives in environment] → Not committed, `.env` ignored; rotate by changing the password in the app after first sign-in (not supported yet, noted).
