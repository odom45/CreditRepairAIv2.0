# Integration preparation and remaining acceptance — October 17, 2026

Application baseline: owner-signed 2.0.6 / 20261019, source `fd354880383f1cafede6d409cf12e481fb042173`. Repository baseline for this follow-up: `28e9076ce553e29a102930d369a3d4baab26ca58`. No native application code or packaged binary changed during this follow-up.

## Production configuration

The owner-authenticated Convex dashboard identified the existing production deployment `efficient-porpoise-478`. It displayed **Never deployed**, with no deployment tokens and no environment variables before this work.

Saved in that production deployment:

- The existing `OPENAI_API_KEY` and `CREDIT_LEGAL_VECTOR_STORE_ID`, reused from the same project's working development configuration. No API key or legal store was created. Copied values were not printed or committed; only their format booleans were checked.
- `CREDIT_AGENT_MODEL=gpt-5.6-terra` and the owner-approved support contact.
- `CREDIT_PAID_ACCESS_ENABLED=false`, `CREDIT_ENABLE_TEST_AUTH=false`, `CREDIT_ENABLE_OPERATOR_TOOLS=false`.

These are saved settings, **not a production deployment or a successful production AI request**. Creating the production deploy credential remains a security approval boundary. The existing `scripts/production-configuration.mjs` and production workflow require a matching production key, gateway and protected settings before deployment. Do not reuse the development deploy key as a production key.

After deployment, verify production authentication, policy acceptance, consented fictional AI retrieval, unauthenticated rejection and deletion failure paths. Build a new signed version targeting the production gateway only after those checks pass. The 2.0.6 tester binary still targets the working development deployment.

## Play preparation

- Google processed the signed 2.0.6 / 20261019 AAB in the existing app draft, with package `com.creditrepairai.v2`, minimum API 26 and target API 36.
- Saved internal release name: **2.0.6 - Firebase repair and guided AI testing**. Release notes identify it as an internal test, with purchases disabled.
- The release preview reported two warnings and no blocking errors: no selected testers at that time, and missing native crash symbols. The owner-only acceptance email list was subsequently created and selected for this track; unrelated tester lists were not selected.
- Saved the same owner-only list for Play license testing, preserving the normal license response. This setup is not evidence of an actual purchase or license-test lifecycle.
- Created inactive subscription products `creditrepairai_monthly` and `creditrepairai_yearly`. The owner subsequently confirmed $30/month, matching the current Financial Projections document. Saved base plan `monthly` with auto-renewing P1M billing, United States availability and exactly USD 30.00. Play initially suggested USD 29.99; the per-country override was corrected and the saved form verified at USD 30.00. The base plan remains inactive. Yearly and per-dispute prices are not specified in that document and remain owner inputs. No base plan or offer was activated.
- Prepared `creditrepairai_dispute` with a standard buy option, but did not create a saved product: Play requires a price for an available region, and rejects a draft with no available regions. No placeholder price was entered. The unsaved form is not an active or completed product.
- The developer account's user list contained the owner only; no server service-account user was present. Server verification credentials and app-level Play permissions remain prerequisites for real receipt validation. Do not enable paid access until those are configured and license-test purchase/restore/expiry/refund checks pass.

The internal track was subsequently published to the selected owner-only acceptance list. Play confirms **Active**, **Available to internal testers**, and the 2.0.6 release. The actual invitation page at `https://play.google.com/apps/internaltest/4700570090398776927` was opened and confirmed to invite the owner to test `com.creditrepairai.v2 (unreviewed)`. Accepting that invitation and installing/signing into the phone app remain owner steps. No invitation agreement was accepted on the owner's behalf.

No public production rollout, customer charge, refund, complaint submission or tester invitation message was performed. Native phone acceptance remains open.

## Play signing versus upload signing

The owner-authenticated Play app-signing page supplied the actual distribution certificate, which differs from the direct-upload certificate:

- Play SHA-1: `6A:17:AC:C0:2F:CD:94:50:0A:B2:1A:0A:A0:AA:DC:13:C9:28:4B:F1`.
- Play SHA-256: `7A:86:78:AC:A5:0F:BF:B9:7D:AC:E1:12:7F:C0:F9:D1:C9:EC:1B:BD:85:50:83:7F:1D:94:FD:E5:F7:81:DA:EE`.

Only these public fingerprints were added to `config/firebase-signing.json`. No Firebase OAuth entry was fabricated. `scripts/firebase-signing-readiness.mjs` now reports `playConfigReady` separately and supports `--require-play-ready`. It requires a matching Android OAuth certificate on the release package in genuine downloaded configuration; a debug/other-package match cannot satisfy it. Neither local readiness flag establishes console-provider status or a live native user session.

## Verification in this follow-up

An isolated checkout restored dependencies with `npm ci --ignore-scripts --no-audit --no-fund`. The recovered shared checkout had incomplete dependencies; that initial local failure was environmental, and no passing result was assumed from it.

- Frontend/Convex typecheck passed.
- All 50 backend/configuration regressions passed, including three new Play-versus-upload certificate cases.
- Policy synchronization and source credential-pattern checks passed on the application baseline.
- Current genuine Firebase configs pass `--require-ready` for the owner-upload certificate, while `--require-play-ready` correctly exits nonzero. Play-certificate registration/download remains required.

This session has no connected Android device. Browser console login and source tests do not establish the signed APK's Firebase → Convex → AI chain. Complete that check with an owned verified-email account and a fictional case on the actual signed app. Real bureau layouts, accessibility/device diversity, independent reviews and the required real-person testing period remain outside these automated results.
