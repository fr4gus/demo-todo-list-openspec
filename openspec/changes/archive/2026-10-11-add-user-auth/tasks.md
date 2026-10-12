# Tasks

## 1. Accounts backend

- [x] 1.1 Add the Flyway migration for `users` (unique lowercase email, role check) and the `User` entity and repository; verify a Testcontainers repository test enforces email uniqueness
- [x] 1.2 Implement registration service and `POST /api/v1/auth/register` with validation, lowercase normalization, BCrypt hashing and `EMAIL_IN_USE`; verify Mockito service tests (success, duplicate, case variant, hash differs from input) and a MockMvc test for 201/400/409
- [x] 1.3 Implement the admin seeding runner with the all-or-nothing rules; verify tests for first start, existing admin, absent variables, partial variables and email clash with a user

## 2. Authentication backend

- [x] 2.1 Implement JWT creation and parsing with `JWT_SECRET` validation; verify unit tests for claims, 24h expiry, tampering and expiry rejection
- [x] 2.2 Implement the JWT filter, security configuration and role rules; verify `@SpringBootTest` + MockMvc tests: no token 401, bad token 401, valid token 200, wrong role 403
- [x] 2.3 Implement `POST /api/v1/auth/login` with `EMAIL_NOT_FOUND` and `INVALID_PASSWORD`; verify MockMvc tests asserting codes, the exact detail messages and token expiry
- [x] 2.4 Confirm no password, hash or token is logged during login and registration; verify with a log-capture test

## 3. Password recovery backend

- [x] 3.1 Add the `password_reset_tokens` migration, entity and repository; verify a Testcontainers test for the unique hash and cascade on user delete
- [x] 3.2 Implement request-reset (token generation, hashing, supersede, mail after commit, uniform response) with a Redis rate limit; verify Mockito tests for known/unknown email and a Redis Testcontainer test for the 6th request returning 429
- [x] 3.3 Implement confirm-reset (validation, expiry, single use, password policy); verify tests for success, expired, reused, superseded and weak password leaving the token usable
- [x] 3.4 Verify the reset email reaches Mailpit locally and its link opens the frontend reset route (manual check documented in README)

## 4. Frontend authentication

- [x] 4.1 Implement `authSlice`, localStorage persistence with expiry, and the 401 handler in the base query; verify unit tests for hydrate, expired token drop and logout on 401
- [x] 4.2 Implement `authApi` endpoints; verify MSW-backed tests for login, register and reset calls including error payloads
- [x] 4.3 Build `SignInPage` and `SignUpPage` with the exact messages; verify RTL + MSW tests for "Email doesn't exist", "invalid password", email-in-use and redirect to sign-in after sign-up
- [x] 4.4 Build `ForgotPasswordPage` and `ResetPasswordPage` including the invalid-link state; verify RTL + MSW tests for neutral confirmation, success redirect and invalid token
- [x] 4.5 Make `ProtectedRoute` role-aware and add post-login redirects; verify tests for signed-out, user-on-admin-route and admin-on-user-route
- [x] 4.6 Add a sign-out control reachable from authenticated screens; verify a test that it clears the token and shows sign-in

## 5. Integration

- [x] 5.1 Run the full flow in Compose (register, sign in, reset password via Mailpit, sign in again, seeded admin sign-in); verify each step manually and record the result in the change notes
