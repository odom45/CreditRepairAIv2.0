# CreditRepairAI release owner checklist

Review snapshot: 2026-10-12. Code implemented is not equivalent to production verified. Do not mark the application 100% launch-ready until every required gate below has evidence.

## October 18 production-first release gate

The signed-release job now requires repository variable `CREDIT_RELEASE_READY=true`; it is skipped while that variable is absent/false, including manual dispatch. Before any Android release assembly it requires paid mode, genuine Firebase Play-signing configuration and matching production server settings via the existing production configuration checker. Its gateway comes from `CREDIT_PRODUCTION_GATEWAY_URL` and deployment credential from `CREDIT_PRODUCTION_DEPLOY_KEY`, rather than the development gateway secret. Leave the release gate closed until production integration and license billing acceptance have evidence. Publishing source may run ordinary development checks but must not produce a premature release candidate.

## October 17 introductory pricing and release-candidate update

The [current pricing and release-candidate record](docs/LAUNCH_PRICING_AND_RELEASE_CANDIDATE_2026-10-17.md) supersedes the older standard-price-only offer selection below. The app now supports eligible `launch-intro` offers: monthly for three P1M cycles and yearly for one P1Y cycle, followed by an infinite recurring base plan. Prices, calculated discounts, eligibility and renewal disclosures come from Play. Intended US prices are recorded in `config/launch-pricing.json`; that file is an operator plan, not evidence of saved Play settings. Candidate version defaults are 2.0.7 / 20261020. No new signed artifact or production acceptance is established by this source change.

## October 17 live integration preparation

The direct-APK Firebase registration repair and signed 2.0.6 verification are recorded in [Firebase repair](docs/FIREBASE_REPAIR_2026-10-17.md). The later [integration acceptance record](docs/INTEGRATION_ACCEPTANCE_2026-10-17.md) records production settings and actual Play draft/test setup. Older missing-credit and upload-certificate findings below are historical. Production deployment, native authenticated acceptance and real Play billing acceptance remain open.

Before distributing through Play, run `node scripts/firebase-signing-readiness.mjs --require-play-ready`. The actual Play signing certificate is now recorded, but its registration is absent from the downloaded Firebase configuration. Upload-certificate readiness alone does not establish Play sign-in readiness. Download genuine Firebase configuration after registering the Play certificate; never manufacture OAuth entries locally.

## October 16 funded AI and authentication update

The funded live agent/retrieval check now passes, including the output-filter/citation corrections verified in recovery run 38958991401; the previous exhausted-credit result below is historical. Native 2.0.4 adds actionable login errors, password recovery, the explicit Google button flow and email verification/token refresh. Current Firebase certificate checks still reject the owner's upload certificate. See [the current completion and acceptance record](docs/ENGINEERING_COMPLETION_2026-10-16.md) for exact evidence and remaining account gates.

## October 15 engineering update

The current source adds AI-generated editable portal answers, monthly/yearly subscriptions and receipt-bound one-dispute access. Review [the completion record](docs/ENGINEERING_COMPLETION_2026-10-15.md) for implementation, Play product setup and remaining account gates. These additions supersede the older monthly-only implementation scope below; they do not mark live account integration complete. The signed release workflow now also triggers on native/backend source changes and verifies an in-place update from 2.0.1 before exporting artifacts.

## Implemented in the native-branch work

- [x] Versioned full privacy/terms/educational disclaimer drafts; explicit acceptance and adult confirmation; separate affirmative AI-sharing choice with local/server enforcement and stale-choice protection. Qualified review still required; no blanket liability transfer is claimed.
- [x] Draft public policy/deletion pages hosted on the authorized development deployment and monitored-contact build injection implemented; configure/test the mailbox and obtain qualified approval before treating the commercial external deletion pathway as operational.
- [x] GitHub emulator instrumentation/lint, read-only protected-configuration checks, owner-key signing probe, locked dependency audit and fictional live-agent smoke jobs added. Results and limits are recorded in `docs/VERIFICATION_RESULTS.md`.
- [x] Reviewed authentication dependency updates and platform-binary cleanup applied; current npm audit reports zero known advisories. Android Bouncy Castle family upgraded from inherited 1.72 to aligned 1.86; resolved Maven inventory/OSV and encrypted-PDF gates added.
- [x] Agent requires completed non-empty legal retrieval and a case-summary tool, rejects incomplete outputs, and clears an abort timer under one 60-second request budget. Eight additional backend regressions cover these cases.

- [x] Native Kotlin/Compose application, Firebase email/Google sign-in contract, short-lived user-bound tokens.
- [x] On-device PDF/OCR parsing, automatic high-risk-identifier redaction, AES-GCM/Keystore case storage, FLAG_SECURE and backups disabled.
- [x] Persisted report-acquisition checklist, isolated Custom Tabs, explicit PDF Open/Share import and document-picker fallback.
- [x] Guided bureau/CFPB filing: editable fields, chunk limits, review before copying, user-only submission, receipts, response records, private check-ins and encrypted inline AI step explanations. Answers are invalidated after relevant draft/step changes and account deletion/switch.
- [x] Goals, local prioritized action checklist, saved AI planning output, explanation affordances and educational labels.
- [x] Play monthly client, server purchase ownership/expiry/state verification, acknowledgment, restoration, hourly lifecycle refresh, admin authorization/cancel/refund-and-revoke.
- [x] Five-page onboarding, in-app privacy/terms/support and local/account deletion paths.
- [x] Session/generation fences prevent stale AI/import/billing completions after account switch or deletion; storage ownership marker survives local clearing.
- [x] Serializable server deletion guard prevents in-flight billing refresh from recreating deleted purchase records; a minimal hashed marker is disclosed in the privacy draft and requires retention review.
- [x] Fictional parser, chunking, state-machine, entitlement and session regression tests; backend policy tests.
- [x] CI definitions cover native tests/build plus backend/admin typecheck, tests, legal registry and source-token scan. Signed release checks owner configuration and signatures.

## Verification completed and still required

- [x] Current application source `2472e38210ac525616c014a9b9c4b65f45a03476`: native/backend jobs, 37 JVM tests, 20 backend tests and owner-signed debug APK verification passed. [Final source run `38241196324`](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/38241196324). All 15 device tests and lint also passed; Android OSV completed across 213 modules with zero reported advisories. Development deployment succeeded, but the real AI smoke failed on exhausted API credit, so the overall run is not green.
- [x] All 15 device scenarios, Android lint, resolved Android dependency scan and protected development deployment completed.
- [x] Private comparison confirmed the GitHub OpenAI key already matches the server key; [key-check run `38241914828`](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/38241914828) passed.
- [x] Real retrieval smoke: after funding, the October 16 rerun of run `38838060702` passed with three HTTP 200 responses and eight legal-source retrieval results. This was a fictional server-side agent check; native authenticated login/AI remains a separate acceptance requirement.
- [ ] Follow `docs/PARSER_VALIDATION.md` on actual devices using permissioned, fully redacted reports. Full UI/device/large-font/TalkBack checks are not covered by pure unit tests.
- [ ] Stage and verify Firebase → Convex → OpenAI with a fictional case and a verified-email account.
- [ ] Stage Play license-tester scenarios: purchase, pending, canceled checkout, restore, expiry, grace, hold, account switch, replacement and administrator refund. Do not use a real charge for testing.
- [ ] Verify deletion/re-authentication failures and vendor retention. The native account flow is a multi-service operation: it deletes Convex app records, then Firebase login. If Firebase fails, it reports incomplete deletion and requires re-sign-in/retry.
- [x] Build signed release with the existing owner upload key and verify APK/AAB signatures and release AAB/R8 assembly. Completed in run 38269615544; installation/authenticated release-device checks remain pending. No upload key was generated.
- [ ] Re-run official-link freshness checks. A passing registry-format/review-age check does not prove every external source link is available or each legal synopsis is current.

## Owner actions, in order

### 1. Configure the missing contact and Firebase registrations

The preflight verified the existing OpenAI/Convex/gateway and four signing Secrets; these are not assumed missing. It also verified the key/alias/password combination. The owner approved `admin@synergyfundai.com` as the support/deletion contact (Gmail fallback: `benjaminjodom45@gmail.com`). Workflows use the admin address when no repository override is configured. Play service-account environment and billing-admin allowlist remain missing. The existing signing certificate does not match either committed Firebase Android OAuth certificate.

1. Restore API billing credit for the existing server OpenAI project/key: live Responses returned `credit_balance_exhausted`. A successful key/model-list read does not prove billable calls can run. Rerun the existing fictional live-agent smoke after funding; no key regeneration is required by this result.
2. Verify delivery and monitoring of the owner-approved `admin@synergyfundai.com` support/deletion mailbox. The release app embeds that address; the native development workflow sets `CREDIT_SUPPORT_EMAIL` in the authorized development environment. Test Settings contact and the external deletion contact. Mail delivery has not been tested.
3. Use the public SHA-1/SHA-256 in `docs/VERIFICATION_RESULTS.md` to register the stable tester/upload certificate with the appropriate Firebase Android apps. Download fresh matching configs to the existing main/debug file paths. Verify email and Google sign-in and native Firebase → Convex → AI using an owned staging account and fictional case.
4. After counsel revisions, regenerate policies, deploy the approved documents and confirm public privacy/deletion URLs and mailbox delivery. Draft page hosting alone is not compliance approval.

### 2. Stage cloud configuration

Use the intended staging deployment first. The supplied `dev:` Convex deployment credential targets a development deployment; the word "production" in a workflow does not turn that into a production deployment. Select the actual production deployment only when ready and store its deployment credential in the protected configuration.

Firebase project: `credit-repair-ai-v2`; package: `com.creditrepairai.v2`; debug package: `com.creditrepairai.v2.debug`.

1. Firebase Console → Authentication → Sign-in method: enable Email/Password and Google; set the owner support email.
2. Firebase Console → Project settings → Your apps: verify both Android package registrations. Register debug signing SHA-1/SHA-256 for test builds, the upload certificate for direct signed APK tests, and the Play App Signing certificate for Play-delivered builds. Download fresh `google-services.json` only after registrations are correct.
3. Google Cloud Console for this project → APIs & Services → Credentials: locate the key referenced by `current_key` in each Firebase config. Do not paste its value into source or the checklist.
4. Apply API restrictions to Firebase-required services only (for the current native auth use, Identity Toolkit API and Token Service API; preserve other Firebase-required APIs actually used). Do not allow unrelated billable APIs or Generative Language API on this client key. Test restrictions before changing a deployed app's key.
5. Apply Android application restrictions using package names and the corresponding signing SHA-1 fingerprints where supported by the Firebase/auth configuration. Do not apply browser-referrer restrictions to Android. Use separate staging/production client registrations as needed, and test Google/email sign-in after restrictions.
6. Review Identity Toolkit quotas and abuse protection; if Firebase database/storage products are enabled, deny unused access with Security Rules and configure supported App Check. An API-key restriction is not user authorization and this app's Keystore encryption is not a penetration-test certificate.

Official reference: https://firebase.google.com/docs/projects/api-keys and https://firebase.google.com/docs/android/troubleshooting-faq

### 3. Configure protected agent/billing environment

GitHub repository **Secrets** used by workflows:

| Name | Purpose |
|---|---|
| `OPENAI_API_KEY` | Server-only OpenAI project key |
| `CONVEX_DEPLOY_KEY` | Key for the intended deployment |
| `CREDIT_AI_GATEWAY_URL` | HTTPS site origin or full `/credit-agent` route |
| `ANDROID_KEYSTORE_BASE64` | Existing owner upload keystore, base64 |
| `ANDROID_KEYSTORE_PASSWORD` | Existing keystore password |
| `ANDROID_KEY_ALIAS` | Existing upload-key alias |
| `ANDROID_KEY_PASSWORD` | Existing key password |
| `VITE_CONVEX_URL` | Correct cloud URL for the web/admin app |

GitHub repository **Variables** used by signed release:

- `CREDIT_SUPPORT_EMAIL`: a monitored owner mailbox; no invented default. Release fails if empty/invalid.
- `CREDIT_PAID_ACCESS_ENABLED`: `false` until legal/configuration/Play tests are approved; set to `true` only for the reviewed paid-software release.

Convex protected environment:

- `OPENAI_API_KEY`, `CREDIT_LEGAL_VECTOR_STORE_ID`, `CREDIT_AGENT_MODEL`: configure via the legal-agent workflow (model currently `gpt-5.6-terra`; confirm project access and actual tool response in staging).
- `CREDIT_SUPPORT_EMAIL`: the same monitored mailbox configured for the native release; public Convex policy/deletion pages read this server setting.
- `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`: existing authorized Google service-account credentials with least-privilege app permissions for Play purchase verification and, if needed, refund/cancel management. Keep server-only. Owner/account administrators must supply this; code cannot authorize itself to a Play account.
- `BILLING_ADMIN_IDENTITIES`: exact authenticated `tokenIdentifier` values for trusted administrators. Sign into `/admin` to view the caller's identity, then configure the allowlist. Email addresses and client code do not grant administrator access.
- `CREDIT_PAID_ACCESS_ENABLED`: match the APK's paid-access mode. A paid APK with a free backend mode is a release configuration error. A free staging mode still requires auth/verified email and has a per-account AI rate limit.
- `CREDIT_ENABLE_TEST_AUTH` and `CREDIT_ENABLE_OPERATOR_TOOLS`: unset/false in production. Do not enable the retired test provider on a public production deployment.

Do not commit service-account JSON, private keys, upload keys, purchase tokens, passwords or API secrets. Firebase `google-services.json` is public client configuration and is committed; its restrictions are an owner-console check, not proven by a source scan. Known historical test passwords are retired and the test provider is disabled by default. The pattern scan is not an exhaustive credential audit.

### 4. Configure Play subscriptions and test them

Play Console for `com.creditrepairai.v2` → Monetize → Products → Subscriptions:

1. Configure `creditrepairai_monthly` with base plan ID `monthly` (`P1M`, $30 renewal), and `creditrepairai_yearly` with base plan ID `yearly` (`P1Y`, $249.99 renewal), US availability. Add offer ID `launch-intro` to each: $24.99 per month for three monthly cycles, and $199.99 for one annual cycle, respectively. Use Play-evaluated new-customer eligibility (never subscribed to any subscription in the app). The app prefers that exact eligible offer and otherwise displays the regular base plan. Configure `creditrepairai_dispute` at $9.99 with one standard buy option (no multi-quantity, rental or preorder options). One-time purchase consumption occurs only after the verified account/case binding is saved. These are intended settings; consult the current record for actual saved state.
2. Description: monthly software access to guided tools, AI assistance and education; results are not guaranteed. Cancel anytime through Google Play. Do not describe this as fee-for-deletion, representation or a promise to improve a score.
3. Configure license testers and install an appropriately signed internal-test build. The `.debug` package is separate and is expected to report billing unavailable unless separately configured; local parsing/education remains available.
4. Give the existing server service account the required app-level Play permissions and enable the Android Publisher API. Run the documented license-test lifecycle for both subscription periods and one-time case access, including pending, refund, repeat purchase for a different case, restore, account switch and deletion/retry; never test actual customer refunds or real disputes here.
5. Admin refunds use Play's subscription revoke/full-refund API. They require explicit confirmation and server authorization. If the UI reports pending verification, inspect Play Console and refresh before retrying a financial operation.
6. The server re-verifies on foreground/AI use and hourly cron. It does not claim RTDN delivery is configured; add a Pub/Sub integration later if immediate background lifecycle propagation is required. No access is granted for pending/hold/paused/expired states; canceled subscriptions can retain access until verified expiry.

References: https://developer.android.com/google/play/billing/integrate and https://developers.google.com/android-publisher/api-ref/rest/v3/purchases.subscriptionsv2/revoke

### 5. Human reviews and actual devices

- Fintech/CROA and applicable state credit-services review of software/access model, fee timing, advertising, contracts/cancellation, privacy and terms. Calling a payment "software access" does not establish legal exemption. In-app documents are implementation drafts pending qualified review.
- Independent mobile/backend penetration test and remediation.
- Real-user/device testing and permissioned layout validation; code tests cannot establish actual bureau behavior or consumer outcomes.
- Publish approved privacy and external account-deletion-request URLs, a monitored support contact, store materials and accurate Data Safety/app-access answers.

### 6. Release and owner submission

1. Use the source/run and completed gate results in `docs/VERIFICATION_RESULTS.md`. Download the matching owner-signed debug APK artifact for staging. Any later application change must pass the applicable checks again; a documentation-only head does not change the tested application source.
2. Signed release run 38269615544 completed from `android-native-rebuild`. Editing the signed-release workflow on that branch now triggers a signed build; routine application commits do not trigger release builds. Manual dispatch still requires the workflow definition on the default branch. No Play publishing or change to the `main` website occurred.
3. The native workflow deploys only to the authorized development deployment, preserving the existing keys/vector store. Select/configure production explicitly after review. The separate legal-library workflow can create billable resources; do not run it merely to recheck existing staging. Verify a fictional-case authenticated native answer and source citations before release.
4. Run **Signed Android release** with a version code above every previous Play upload and an appropriate version name. Existing owner upload key only. Gradle refuses missing gateway/support/signing configuration; workflow verifies APK/AAB signatures and removes its ephemeral decoded key file.
5. Install the signed tester APK and complete release-mode checks. Owner uploads the signed `.aab` to Play, enrolls testers/fulfills account-specific testing requirements, completes review questions and submits. This task does not publish to Play.

## Remaining engineering proof and account-dependent work

Automated device/lint results are specific to the tested source and fictional scenarios. Complete manual/TalkBack/device diversity checks, native authenticated staging, actual Play license lifecycles, permissioned bureau layouts, vendor deletion/re-auth failures and release AAB/R8 validation still need evidence. Owner-controlled Firebase/Play/contact inputs are prerequisites for several checks; code cannot grant its own Play permissions or establish that a mailbox is monitored. These account gates are additional to lawyer review, penetration testing, real-user feedback and Play submission. Do not declare 100% acceptance until they are complete.

Custom Tabs require an explicit PDF Open/Share/import handoff; silent download interception is unavailable through that browser API. The app never observes portal credentials or auto-submits.
