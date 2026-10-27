# CreditRepairAI — Native Android

> **Device feedback update — October 26, 2026:** The previous 100% coding-backlog estimate is superseded by user-reported account/verification, walkthrough-discovery and assistant usability gaps, plus the required physical-letter feature. [PR #18](https://github.com/odom45/CreditRepairAIv2.0/pull/18) implements the repair; user-journey CI and a new signed 2.0.8 build are pending. Parser-only evidence below remains valid and does not establish acceptance of these new flows. Current work: [user-journey repair](../docs/USER_JOURNEY_REPAIR_2026-10-26.md).


This directory contains the native Android rebuild. It uses Kotlin and Jetpack Compose; it does not use Capacitor, a WebView, or a wrapped website for the application interface.

## Current branch status — October 26, 2026

**Current overall code-readiness rating reopened pending the user-journey repair.**

PR #16 is merged into `android-native-rebuild` at `ffe4e3750b49ee2db77d5b9cc36cea11a3eb0519`. [Final PR CI](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/40435571642) passed the JVM suite, document consistency and all eight Android parser tests. The expanded corpus has 78 PDF outcomes (72 exact supported reports plus six safe rejections) and 18 new text fixtures across all three bureaus. See [the assessment](../docs/PRODUCTION_ASSESSMENT.md) and [current owner checklist](../RELEASE_OWNER_CHECKLIST.md) for scope and remaining launch acceptance. Version defaults remain 2.0.7 / 20261020; this record does not claim a new APK/AAB.

## Included in the native implementation

- Dark navy/blue CreditRepairAI visual identity and native navigation
- PDF import through Android's secure document picker
- Embedded PDF text extraction with on-device OCR fallback for scanned reports
- Bureau and score detection plus structured account extraction
- Three-bureau comparisons for balance, status, missing-reporting, negative-item review, and possible duplicate patterns
- Fact-specific FCRA dispute drafts, clipboard/share actions, and tracking statuses
- CFPB walkthrough with a reviewable complaint narrative and official-portal handoff
- Secondary consumer-report freeze links and a private confirmation checklist
- Authenticated, tool-enabled report-aware agent contract plus educational action checklists without predicted outcomes
- Manual score history, action progress, safe fictional demo data, and complete local-data deletion
- Persisted Custom Tab report acquisition and explicit PDF share/open imports; browser credentials and page contents are never inspected
- Persisted editable portal filing guides, encrypted inline AI step explanations, user-only submission, receipts, response records and private 30/45-day check-ins. Step/draft edits invalidate outdated answers; credentials and actual portal submission remain entirely user controlled.
- Goal planning, saved AI plans, finding/draft/citation explanation actions and educational labels
- Play monthly/yearly and case-bound dispute billing clients with server verification/admin management; purchases disabled by default until owner setup/review
- Five-step onboarding, privacy/terms/support screens and account-deletion flow
- Session-generation regression checks prevent stale work from restoring deleted case data or crossing accounts

## Important boundaries

- A finding is a review prompt, not proof that reported information is wrong.
- Users must review every statement, replace placeholders, attach evidence, and submit disputes or CFPB complaints themselves.
- The app does not guarantee deletion, approval, a score increase, or timing.
- Report parsing and deterministic review flags run on-device. The interactive assistant only calls an HTTPS, authenticated server-side agent using a redacted structured case snapshot. Set `CREDIT_AI_GATEWAY_URL` only for that gateway. API keys and legal-vector-store IDs must never be embedded in the Android app.

## Signed Play release

The debug APK is for testing only. Google Play requires the release AAB to be signed with the app's upload key. Run the **Signed Android release** GitHub workflow after configuring these repository secrets:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`
- `CREDIT_PRODUCTION_DEPLOY_KEY` (the intended production deployment credential)

Repository variables `CREDIT_PRODUCTION_DEPLOYMENT`, `CREDIT_PRODUCTION_GATEWAY_URL` (HTTPS), `CREDIT_SUPPORT_EMAIL` and the release-mode settings must match the intended deployment. The workflow maps the production gateway variable to the native build property `CREDIT_AI_GATEWAY_URL`. Public production mode requires `CREDIT_RELEASE_READY=true` and configured paid access; the separate `CREDIT_BUILD_PRODUCTION_TEST_CANDIDATE=true` mode requires purchases disabled. `CREDIT_PAID_ACCESS_ENABLED` defaults to `false`. See the root `RELEASE_OWNER_CHECKLIST.md` for exact server configuration and mandatory testing. These in-app policy documents are drafts pending qualified review, not approved legal advice.

For an existing Play listing, use the upload key already registered with that app. For a new listing, create the owner-controlled upload key once, back it up securely, and never commit it. The workflow emits a signed AAB for Play and a signed release APK for testers, then verifies both signatures.

When starting the workflow, enter a Play `version_code` greater than every previously uploaded build. The current candidate defaults are version 2.0.7 / code 20261020; verify the highest uploaded code before using them. Introductory offer setup and release gates are recorded in `../docs/LAUNCH_PRICING_AND_RELEASE_CANDIDATE_2026-10-17.md`.

## Build

Requirements: JDK 17 and Android SDK 36.

```bash
./gradlew testDebugUnitTest assembleDebug
```

Backend/admin checks: `npm ci --ignore-scripts`, `npm run typecheck`, `npm run build`, `npm run test:launch`, `npm run legal:check`, `npm run audit:source-secrets` from the repository root.

Device regression tests: `./gradlew connectedDebugAndroidTest` on a disposable debug installation/emulator with fictional records only. `AppRepositoryInstrumentedTest` verifies actual Keystore storage and case preservation after local clearing; `OnboardingInstrumentedTest` verifies the five-step flow. Historical broader device-suite results are in [verification records](../docs/VERIFICATION_RESULTS.md); the current parser-specific emulator evidence is linked above. Follow [PARSER_VALIDATION.md](../docs/PARSER_VALIDATION.md) for manual and permissioned-layout checks.

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```
