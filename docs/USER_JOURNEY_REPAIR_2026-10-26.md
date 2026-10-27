# Account, dispute, letter and conversation repair

The owner tested signed 2.0.7 and reported no visible sign-in/verification or sign-out, no offered bureau walkthrough, and an assistant blocked on verification with weak interaction. Physical mailed dispute letters are a required addition. These findings supersede the previous 100% backlog estimate.

## Implemented and CI-verified in merged PR #18

- A restored unverified Firebase account now sees a verification screen before report/dispute tools. The active email, resend/check controls and confirmed account switch are visible. Returning to the app refreshes verification and the ID token. Import handlers also require verification. Existing encrypted reports are retained during verification; no key or Firebase configuration is replaced.
- Every saved dispute offers a walkthrough and print/mail action for each relevant known bureau. Creating or reopening the draft from Analysis opens its own guide. The guide retains separate bureau progress and does not silently default to an unrelated bureau.
- Paper letters contain one bureau recipient, local-only sender fields, editable facts/resolution/evidence and signature space. Export and print require an explicit review. Native PDF pagination preserves all lines and supports print page ranges. The user prints, signs, adds bureau-required forms/identity records/supporting copies and mails the letter personally. Generating/saving/printing does not record submission.
- Mailing, confirmed delivery and response receipts are encrypted on device. 30/45-day check-ins use confirmed delivery; no response date or deletion is promised. Postal CFPB readiness considers an actual response or confirmed delivery plus 45 days; the user must still check the actual complaint requirements.
- AI requests include bounded, redacted recent conversation only for the selected scope, under the existing 4,000-character server contract. Earlier turns are labeled untrusted context. Error replies and other-case chat are excluded. Mail status dates/bureau can inform case history; raw tracking numbers, sender names/addresses and exported letter text are excluded from the agent snapshot. Existing authenticated server and separate AI-sharing consent remain required.
- The server outcome guard analyzes Markdown emphasis and narrowly qualified negation without allowing actual promises. A blocked draft gets at most one revision attempt using the same retrieved context and original deadline; a second failing draft remains withheld. Mail metadata is separately allowlisted by bureau/date; sender and tracking fields are stripped.
- The assistant scrolls to new messages, offers follow-up questions and exposes a stop-waiting action. Stopping the UI does not claim to retract a request already received by the provider.

## Automated verification for 2.0.8

The new secret-free native-experience workflow compiles the app/test APK, runs all native JVM tests, lint and the full emulator instrumentation suite. Added tests reproduce restored unverified sessions, account/sign-out discovery and confirmation, correct bureau guide entry, letter-export review requirements, multi-page PDF first/last-line integrity, print page ranges, encrypted postal/chat persistence and exclusion of private tracking from agent JSON. Conversation properties check case isolation, redaction, bounded history and preservation of the current question.

No actual owner Firebase account, consumer report, postal mailing, printer or live OpenAI response is used in these offline CI fixtures. Those results must not be presented as owner-phone acceptance. Existing protected credentials are reused by the established build process; this patch neither creates nor changes keys.

## Official mail sources checked October 26, 2026

- Experian: [official dispute help](https://www.experian.com/help/dispute-credit/), Experian / Dispute by Mail / P.O. Box 4500 / Allen, TX 75013.
- Equifax: [official dispute request form](https://assets.equifax.com/assets/personal/Dispute.pdf), Equifax Information Services LLC / P.O. Box 740256 / Atlanta, GA 30374.
- TransUnion: [official mail/phone dispute instructions](https://www.transunion.com/credit-disputes/dispute-your-credit/mail-or-phone), TransUnion Consumer Solutions / P.O. Box 2000 / Chester, PA 19016-2000.

The app links these source pages and asks the user to recheck current address, form and identity-document requirements. For example, Equifax's official form requires identification/address information beyond the letter; the app does not imply that a name/address-only letter satisfies every requirement. Required sensitive identifiers can be added by the user directly to the bureau form without sending them to AI.

## Regression investigation notes

The new small-screen journey tests exposed a transient long draft-created snackbar covering the actual bureau buttons. The redundant creation snackbar is removed; opening the guide is the success feedback. Guide step/progress now precede selector rows, and routes have explicit state keys. An older AI-draft fixture selected Equifax for a Cedar dispute that only listed TransUnion/Experian; that fixture now selects a genuinely Equifax-relevant finding. The bureau restriction was retained.

Raw PDF byte mutations now use one generated page, so the unchanged 10-second per-mutation budget measures container robustness without requiring three OCR pages inside that budget. All 48 mutations and the 180-second whole-test deadline remain. Separate seeded scans, text mutation PDFs and the measured corpus retain multi-page coverage. No production parser limit or independent field expectation is relaxed.

## Current verification — October 27, 2026

PR #18 merged at `36e33860768d44863ca237527ba5eabeea8aa5f7`, preserving the tested source `f9fce9d2ee625d4b93092141be8b65c4f9b75266`. [Native/backend run 40465737885](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/40465737885) passed compile, JVM tests, lint, all 37 API-35 device tests and backend typecheck/regressions. [Parser run 40465737742](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/40465737742) passed all eight parser tests, 72 exact supported PDFs, six safe rejections and 1,950/1,950 fields in 146,489 ms. Malformed-PDF mutations and real bundled OCR also passed.

Code readiness: **98%** — engineering estimate for the agreed native scope, not a measured percentage or exhaustive audit. Residual integration/edge-case defects remain possible; human legal/security/Play/real-report/device/staging gates are tracked separately and are not code-completion deductions. Signed-build and backend deployment evidence is recorded below; owner-phone acceptance remains open.

## Prior feature-list cross-check

The earlier buyer-brief list also named a pre-send evidence check and an explicit Explain my result experience. Letter export/printing requires evidence details and reviewed facts/mail requirements; editable portal answers retain evidence/placeholder review before copying or submission tracking. Every dispute now offers Explain my result: select a relevant bureau, enter its actual response (optional supporting-record summary), remove identifiers and explicitly review before invoking the existing consent-gated/redacting assistant. Switching bureaus clears the prior response and review. The prompt asks for one missing fact/next action and official sources without changing dispute status or presuming a correction. This extra UI entry passed [native/backend CI 40467835812](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/40467835812), including all 38 device tests; [parser CI 40467835785](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/40467835785) also passed. It is merged in PR #19; no mailing-service partnership, postage purchase or automatic submission is claimed.

## Backend deployment and live synthetic check

[Production deployment 40467129591](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/40467129591) succeeded using the existing configured production target and protected credentials. [Native/development run 40467129617](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/40467129617) passed build, device/lint, backend/admin, preflight, development deployment and real-provider synthetic smoke. The smoke used fictional data and the configured server key, received three HTTP 200 responses, found eight retrieval results and returned a sourced FCRA-referencing answer that passed the outcome guard. It explicitly did not exercise native Firebase authentication. No credentials were created or rotated.

The pending PDF export snapshots its original dispute ID as well as owner; deleting that dispute while the Android save picker is open cancels writing. Final source with this additional guard is checked again by native/release CI.

## Signed APK 2.0.8 / 20261026

[Signed release 40469209159](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/40469209159) passed for source `1e108d61841d630a5f80fb6c6b728e55a9e650e5`. APK v2/v3 and AAB signature verification passed with the existing owner certificate. API-36 installation of the prior signed release, in-place update and process launch passed; no actual Firebase account or Play purchase was exercised. Purchases remain disabled for this production-test candidate.

The downloaded APK archive matched its published GitHub artifact digest; extracted APK ZIP integrity passed and its signing-block certificate matched SHA-256 `1ec8aaa5a3b259ac8d0ec16f66b805ddf82e32a73fc5f486dce320844e8d09ad`. APK: `CreditRepairAI-2.0.8-Updated-Signed.apk`, 48210706 bytes, SHA-256 `f13d5d1890a835d069fcd409a609592477380fa26de373867c695368946c3a2c`. [GitHub artifact](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/40469209159/artifacts/12718077669). Install over the existing signed app to retain its encrypted local reports; uninstalling or confirming sign-out clears local data.

## Final source regression result

Source `1e108d61841d630a5f80fb6c6b728e55a9e650e5` passed [native/backend run 40469209409](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/40469209409): 79 unit tests, all 38 device tests, 55 backend tests, zero failures and zero lint errors (29 warnings). [Parser run 40469209153](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/40469209153) passed all eight importer tests, all 78 corpus outcomes and all 1,950 fields; corpus elapsed time 144,226 ms. This includes the final pending-export ID guard. The published evidence update changes documentation only and preserves this tested/signed application source. Owner-phone, actual authenticated account, printer/postal and launch acceptance remain open.
