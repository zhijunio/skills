# Spring Boot 4 MFA demo

A runnable, intentionally in-memory sample of two-step session authentication with Spring Security 7. It demonstrates password or passkey as the first factor, followed by TOTP, an email one-time code, an EIP-191 wallet signature, or a one-time app recovery phrase. Application resources stay blocked until both steps succeed. It is for learning, not production deployment.

## Run

```bash
./mvnw spring-boot:run
```

The demo user is `demo` with password `changeit`. Open `http://localhost:8080/` to start the first step. The first login can use the demo email factor (`demo@example.test`; its code is printed to the application log), then enroll TOTP, wallet, recovery phrase, or passkeys after completing MFA. User records, TOTP secrets, email challenges, wallet credentials, recovery phrase hashes, WebAuthn credentials, and sessions are in memory and are lost on restart. Spring Security's CSRF protection remains enabled; clients first fetch `GET /api/csrf`, then send the returned token in the named header for state-changing requests.

The Chinese demo page separates the first authentication step from the second-factor challenge. Before MFA is complete, it only exposes factor verification and logout; factor enrollment and all application APIs require a completed MFA session. Password login and passkey login each establish the first step, then the user must verify one configured second factor. The browser must have an injected Ethereum wallet for the wallet-signature flow. Passkey registration and assertion use Spring Security's WebAuthn endpoints with browser ceremonies initiated by the demo page.

## Factor flows

Ordinary application routes, including `GET /api/users/me`, require the login's primary factor plus one verified TOTP, email, wallet, or app recovery phrase. A primary factor is the password or a passkey authentication. Factor completion is stored in the HTTP session's `SecurityContext`. The filter chain rejects password-only sessions with `403`; MFA challenge endpoints remain available so the user can finish the second step. Sensitive account-security reads and factor enrollment require MFA to have been verified within the previous five minutes. Re-verifying a factor refreshes that window; ordinary profile reads do not prompt again.

### TOTP

1. `POST /api/mfa/totp/enroll` returns a development enrollment secret and `otpauth` URI.
2. Add it to an authenticator app.
3. `POST /api/mfa/totp/confirm` with `{"code":"123456"}` confirms enrollment.
4. `POST /api/mfa/totp/verify` with a fresh code adds the TOTP factor to the session and completes login MFA.

The verifier follows a 30-second HMAC-SHA1 TOTP step, accepts a narrow ±1 step window, and atomically rejects replay of an accepted time step.

### Email one-time code

`POST /api/mfa/email/request` sends to the demo account address `demo@example.test`. The sample prints the code to the application log instead of connecting to an email provider. `POST /api/mfa/email/verify` with `{"code":"123456"}` consumes the short-lived code. Replace the development delivery with a mail provider and bind it to a verified account email before deployment.

### Wallet signature

1. `POST /api/mfa/wallet/enroll/challenge` with `{"address":"0x..."}` returns an EIP-4361-style sign-in message.
2. Sign the exact returned message using the wallet's EIP-191 `personal_sign` flow.
3. `POST /api/mfa/wallet/enroll/verify` with `{"signature":"0x..."}` verifies and binds the recovered address.
4. On a later login, `POST /api/mfa/wallet/challenge` returns a fresh, one-time message; sign it and send the signature to `/api/mfa/wallet/verify` to complete the second step.

The sample checks the challenge expiry, exact message signature, recovered address, and one-time consumption. Before production, use a maintained SIWE implementation and validate the actual deployment domain, URI, chain ID, nonce format, and origin.

### One-time app recovery phrase

After completing MFA recently, `POST /api/mfa/recovery-phrase/enroll` creates a random, valid BIP-39 12-word phrase for this application and returns it once. Save it offline. The server stores only its SHA-256 hash. `POST /api/mfa/recovery-phrase` with `{"phrase":"word1 ... word12"}` consumes the phrase and completes the second step; five invalid attempts invalidate it. Generating a replacement requires a recent MFA verification and invalidates the previous phrase.

This phrase is an app-specific, single-use recovery credential. It is not a wallet seed phrase and must never be used to create or restore a wallet. The app does not ask for or store wallet seed phrases. Wallet ownership continues to use the signature flow above. Because a recovery phrase is a bearer secret, production systems should treat it as a recovery code, rate-limit attempts, provide secure replacement and revocation flows, and avoid treating it as a phishing-resistant factor.

### WebAuthn / Passkeys

The `.webAuthn(...)` configuration enables Spring Security's WebAuthn registration and assertion endpoints, with an in-memory credential repository for local demonstration. The Chinese demo page performs the browser `navigator.credentials.create/get` ceremonies against Spring's endpoints. Registration requires a completed MFA session. A passkey can be the primary factor; the user must still complete a second factor for this sample's two-step policy. Use `http://localhost:8080` for the configured relying-party origin and ID. A production policy should require and verify user verification when relying on a passkey as a phishing-resistant factor.

## Wallet recovery phrase boundary

The app recovery phrase is generated specifically for this demo and is never used as a wallet seed. Never enter a wallet's recovery phrase into this application. Wallet ownership is proved with a signed, expiring, single-use challenge; never request, log, or store a user's wallet recovery phrase.

## Production gaps

- Replace in-memory user/factor/credential stores with durable repositories and schema migrations; encrypt TOTP secrets at rest.
- Replace console email delivery, bind email to the account's verified address, and add resend limits and account-level abuse controls.
- Require recent authentication for factor enrollment, replacement, removal, and recovery; add audit events and user notifications.
- Configure HTTPS, real RP ID/origins, session cookie flags, rate limiting, challenge attempt limits, and monitored delivery.
- Persist WebAuthn credentials for deployments. The UI uses Spring Security's built-in WebAuthn registration and login pages; verify browser behavior against the real RP ID and origin before deployment.
- A production wallet flow should use a complete SIWE parser/verifier and enforce deployment-specific domain, URI, chain, nonce, and time policy.
- Replace the in-memory recovery phrase hash with durable, protected storage and define account recovery, phrase rotation, revocation, notifications, and abuse-response policies before deployment.

See the skill's [MFA reference](../../spring-boot-developer/references/security/mfa.md) for the policy model and Spring Security version notes.
