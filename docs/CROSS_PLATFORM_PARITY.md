# Cross-platform parity acceptance

Baseline: `android-native-rebuild` commit `89ee2b7fc5bcb1319f37861a67a613631b8f99b0` (2.0.9).

This is an implementation and verification record, not a production launch approval. There is no claimed completion percentage. A compiling client is not evidence of live account acceptance.

## Web implementation

- Firebase email/password, Google popup, verification/resend/refresh, password reset, explicit sign-out; in-memory auth persistence; fresh sign-in after reload/close and five-minute inactivity lock.
- PDF.js embedded extraction and self-hosted Tesseract worker/WASM/model. No original PDF or extracted raw text is uploaded. 50 MB / 200 text-page / 30 OCR-page limits, bounded text/fields/lines/accounts, no silent partial-report success.
- The Android text parser's supported patterns and comparison rules are ported, with original synthetic fixtures reused.
- Original-data review/editing, supported dispute reasons, per-bureau and CFPB walkthroughs, editable fields and reviewed copy chunks, actual receipts and response dates, bounded case-aware AI conversations, cited step explanations and response explanations, editable AI draft previews.
- Separate bureau letters, local sender fields, multipage PDF export, explicit paid LetterStream website handoff, mailing/delivery/response records.
- Goals and saved AI plans, dated manual scores, freeze checklist, terms/AI-sharing consent, account and local-data deletion controls, existing server-side subscription and scoped-dispute enforcement. New checkout stays disabled, matching the Android test candidate.
- AES-256-GCM local vault, PBKDF2-SHA256 with 310,000 iterations, random salt and IV, Firebase UID bound as authenticated additional data. Passphrase/key never persisted. Encrypted transfer is explicit; no automatic cloud case synchronization is claimed.
- Shared Android Convex agent, policies and billing copied from the pinned baseline. Exact allowlisted-origin CORS/preflight adds browser support without changing origin-less native request handling.

## Verification performed November 1, 2026

- 29 web tests passed: all 18 Android corpus variants, 2,400 seeded mutations, parser limits, identity/comparison, filing/CFPB guards, case size limits, schema validation before save, AES roundtrip/wrong owner/wrong passphrase/tampering and CORS.
- 41 shared agent and billing regressions passed, using synthetic/mocked provider responses rather than a live OpenAI call.
- Browser importer harness: six exact synthetic real-PDF checks passed (three bureaus × embedded PDF and image-only OCR); two non-PDF/truncated files rejected. Each supported PDF yielded two accounts, score 640, balances 1280/0 and suffix 1234.
- Full web/backend TypeScript checks and production build passed before publication of this candidate. Large bundle warnings remain informational; OCR assets are self-hosted and loaded on demand.

## Unverified acceptance gates

- Deploy the shared backend revision and set `CREDIT_WEB_ORIGINS` to each exact HTTPS web origin. An unset list deliberately rejects browser calls.
- Build the web app with the **same** `CREDIT_PRODUCTION_GATEWAY_URL` used by Android (workflow passes this as `VITE_CREDIT_AGENT_ENDPOINT`). Confirm Firebase authorized domains and public API restrictions permit that web origin.
- Live same-account email/Google sign-in, verification refresh, consent revocation, AI, entitlement restoration, deletion/recent-auth and account-switch races need staging acceptance. No live provider request or customer report was used in these tests.
- Validate supported real bureau layouts, accessibility and mobile Safari/iPad/Mac browser behavior; browser security cannot replicate Android's FLAG_SECURE or non-exportable Keystore exactly. The web vault uses a user-held passphrase instead.
- Apple native compilation, simulator/UI/device tests, Google client registration, signing and distribution have their own evidence and cannot be inferred from web tests.
- Android encrypted transfer requires the companion bridge change and Android regression acceptance before it can be described as available in an APK.

Keep release status HOLD until these applicable gates have evidence. Platform branch names alone do not establish parity or installed-app availability.

The follow-up portability change removes explicit Linux-only build-package requirements so npm can resolve the correct optional native packages on each host. A local-case clear control and a deletion/save fence prevent accidental stale-vault restoration.
