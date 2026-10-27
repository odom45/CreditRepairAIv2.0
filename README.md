# CreditRepairAI v2.0

> **Device feedback update — October 26, 2026:** The previous 100% coding-backlog estimate is superseded by user-reported account/verification, walkthrough-discovery and assistant usability gaps, plus the required physical-letter feature. [PR #18](https://github.com/odom45/CreditRepairAIv2.0/pull/18) implements the repair; user-journey CI and a new signed 2.0.8 build are pending. Parser-only evidence below remains valid and does not establish acceptance of these new flows. Current work: [user-journey repair](docs/USER_JOURNEY_REPAIR_2026-10-26.md).


Native Android tools for on-device three-bureau report analysis, guided disputes and user-reviewed filing, with an authenticated legal-source AI assistant.

## Current Android status — October 26, 2026

**Current overall code-readiness rating reopened pending the user-journey repair.**

This is an engineering estimate based on the previously implemented native scope, closure of all three stated remaining code gaps, and passing automated checks. It is not an exhaustive new audit, a probability of correctness, or production-launch approval. The separate React web prototype is outside this rating.

[PR #16](https://github.com/odom45/CreditRepairAIv2.0/pull/16) is merged into **`android-native-rebuild`** at `ffe4e3750b49ee2db77d5b9cc36cea11a3eb0519`. Source version defaults remain **2.0.7 / 20261020**. The parser follow-up and all three remaining code-backlog items are complete. This documentation update does not create or claim a newly verified APK/AAB.

| Current evidence | Result |
|---|---|
| Final PR CI | [Run 40435571642](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/40435571642) passed on `c6eb63f85a18a6ab6ecc6c57b94eaa955b87469d` |
| Native JVM regression suite | Passed, including 2,400 seeded text mutations, account-boundary properties and limit tests |
| API 35 Android parser tests | 8 passed, including 24 mutated-report PDFs, 48 malformed/truncated byte mutations, three seeded image-only/multi-page OCR inputs and oversized-input rejection |
| Measured generated PDF corpus | 72/72 supported reports exact, 6/6 guards safely rejected, 1,950/1,950 fields matched |
| Expanded checked-in corpus | 18 new synthetic text fixtures: six layouts per bureau, plus the three original fixtures |
| Assessment/checklist consistency | Automated evidence check passed; historical numerical scores are archived |

Detailed status: [current code assessment](docs/PRODUCTION_ASSESSMENT.md), [release-owner checklist](RELEASE_OWNER_CHECKLIST.md), [machine-readable evidence](docs/PARSER_ROBUSTNESS_EVIDENCE.json), and [parser reproduction guide](docs/PARSER_ROBUSTNESS.md). The original measured-field results are tied to [run 40434452161](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/40434452161); the later final PR run repeated the full suites successfully.

## Native implementation

- Kotlin/Jetpack Compose app with Firebase email/Google sign-in integration and authenticated Convex access.
- On-device PDFBox extraction and bundled ML Kit OCR, structured account/score parsing, and three-bureau comparisons.
- High-risk identifier redaction before AI calls; Keystore AES-GCM case storage, FLAG_SECURE and backups disabled.
- Tool-enabled legal-source AI, editable dispute/portal answers, filing checklists, official-portal handoff, receipts and responses. Users review, copy and submit themselves.
- Goals and saved plans, score/action tracking, onboarding, privacy/terms/disclosure and deletion flows.
- Monthly/yearly subscriptions and case-bound dispute access in source, with server-side ownership/verification and account-generation protections. Release mode and paid access remain controlled by the existing configuration gates.

Implementation presence does not establish live account acceptance. Legal/security review, Play submission/closed testing, real-report validation and device/staging integration acceptance remain in the owner checklist. Synthetic fixtures do not establish accuracy for every actual bureau layout.

## Development

```sh
git clone --branch android-native-rebuild https://github.com/odom45/CreditRepairAIv2.0.git
cd CreditRepairAIv2.0
python3 scripts/check-parser-evidence.py
cd native-android
bash ./gradlew testDebugUnitTest assembleDebug
```

Use JDK 17 and Android SDK 36. Emulator/parser instructions: [PARSER_ROBUSTNESS.md](docs/PARSER_ROBUSTNESS.md). Native build/signing configuration: [native-android/README.md](native-android/README.md). Current release workflow: [.github/workflows/signed-android-release.yml](.github/workflows/signed-android-release.yml).

## Web/admin scope and historical records

The `src/` React report/analysis website is a separate legacy prototype with sample data, plus the admin interface. Its simulated report pages are outside this native code-readiness rating. Root `npm ci --ignore-scripts`, `npm run typecheck` and `npm run build` apply to that web/admin/backend toolchain.

Historical test counts, signing/Firebase work and integration observations remain in [VERIFICATION_RESULTS.md](docs/VERIFICATION_RESULTS.md) and the dated engineering records it links. Historical API-credit/certificate findings are not current account-state claims. The [active backlog](todo.md) and [owner checklist](RELEASE_OWNER_CHECKLIST.md) are the current status entry points.

Author: **Benjamin Odom** · [odom45](https://github.com/odom45)
