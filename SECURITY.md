# CreditRepairAI security and privacy boundary

"Bank-level security" is not a certification. Production readiness requires implemented controls, documented operations, and independent verification. This file records the current technical boundary and launch blockers.

## Implemented in the native branch

- Raw PDF text and OCR bitmaps are used transiently on-device and are not persisted by the app.
- The document-picker URI grant is not persisted.
- Stored structured case state is encrypted with AES-GCM using a non-exportable Android Keystore key.
- Android cloud backup/device transfer is disabled for the app, and screenshots/screen recording are blocked with `FLAG_SECURE`.
- Delete-all clears local case state and destroys the Keystore key.
- The AI client accepts HTTPS endpoints only, requires a short-lived bearer token, redacts high-risk identifiers, and sends structured fields rather than the PDF or full report text.
- The server endpoint requires an authenticated identity, limits request size, rejects full account numbers/high-risk identifiers, does not persist the submitted snapshot, and returns `no-store` responses.
- OpenAI and vector-store credentials exist only in server environment variables.
- Release signing material is accepted only through protected CI secrets and is ignored by Git.

## Required before handling real consumer data in production

- Connect native OIDC sign-in (recommended: Auth0 with MFA/session revocation, or another Convex-supported OIDC provider) and wire sign-out/account deletion to local key destruction and server-side deletion.
- Add server-side per-user rate limits, abuse controls, audit events containing no report data, session-expiry tests, and authorization tests.
- Complete mobile/backend threat modeling, dependency/SAST/secret scanning, penetration testing, and remediation.
- Establish incident response, breach notification, access control, key rotation, environment separation, backup/restore, vendor review, and deletion/retention procedures.
- Publish an attorney-reviewed privacy policy, terms, Play Data Safety disclosure, consent flow, and support/deletion process. Determine applicable federal/state privacy, credit-repair, consumer-protection, accessibility, and record-retention obligations with qualified counsel.
- Execute appropriate data-processing terms with infrastructure/model vendors and confirm model-provider data controls for the selected account tier.
- Validate parsing against a representative, permissioned, fully redacted test corpus from all three bureaus. Add regression fixtures without real consumer identifiers.

Do not market the app as certified, "bank-level," guaranteed to remove items, or guaranteed to improve a score unless an authorized independent review supports the exact claim.
