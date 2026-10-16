# CreditRepairAI contractor hand-off

Start here for the native Android product. A prepared contractor can reproduce a staging build without hunting for settings; working owner-controlled Firebase/Convex/OpenAI/Play accounts are prerequisites for authenticated integration. An afternoon setup is a target, not a proven delivery time or a promise of Play approval.

## 1. Establish the exact product and source

Use branch `android-native-rebuild` of `odom45/CreditRepairAIv2.0`. Record the commit SHA and workflow run used for every APK/AAB. The product is native Kotlin/Jetpack Compose in `native-android/`. Root Vite/React code contains a separate companion prototype and admin UI; sample web report comparisons are not evidence of native parser behavior.

Read `CONVEX_SCHEMA.md`, the exported `convex-schema.json`, `CONFIGURATION.example.json`, `../FIREBASE_SIGN_IN_FIX.md`, `../PARSER_BATCH_RESULTS.md` and `../CLOSED_TESTING_PLAN.md`. Keep accounts, domain, data, code/IP, upload signing identity, Play listing, Firebase apps, Convex deployments, OpenAI project/vector store and support mailbox on the transaction's explicit transfer checklist. A source-code license alone does not transfer those services.

## 2. Reproduce local checks

Requirements: Git, Node 22+ with npm, JDK 17, Android SDK 36/build tools, and an API-35 disposable emulator for instrumentation.

```bash
npm ci --ignore-scripts
npm run typecheck
npm run build
npm run test:launch
npm run policies:check
npm run legal:check
npm run audit:source-secrets
node scripts/export-convex-schema.mjs
node scripts/firebase-signing-readiness.mjs
cd native-android
./gradlew testDebugUnitTest assembleDebug
bash ../scripts/run-parser-batch.sh current true
```

The last command requires a running disposable emulator/device; the measured parser workflow provisions one automatically. Parse metrics and per-case expected/actual rows are artifacts, not a claim about every current bureau PDF. Unknown report layouts and missing fields still require verification; monetary storage currently uses whole-dollar integers, not cent precision.

## 3. Wire staging without copying a seller's secrets

1. Decide whether services will transfer or be recreated. Store new owner credentials only in protected environment; never put API keys/vector-store IDs in the APK. Acquire the existing upload key securely for an existing Play listing rather than generating a replacement.
2. Download two genuine Firebase Android configs after registering package/certificate pairs. Enable the intended providers and run the signing readiness check. If using a new Firebase project, update `convex/auth.config.ts` and all three HTTP issuer checks together.
3. Select the intended Convex **development** deployment. Install/typecheck and deploy using `npx convex dev --once` for an interactive fresh-project setup, or an owner-provided deployment key with `npx convex deploy --yes`. The existing unattended `deploy-development.mjs` intentionally accepts only the current owner's named development key; update that explicit allowlist after an authorized transfer, not to bypass it.
4. Set the server variables in the example map using `npx convex env set NAME VALUE` in a secure operator terminal. Do not echo secret values into logs. Keep paid access off until Play verification and billing lifecycle testing are complete. `CREDIT_SUPPORT_EMAIL` must be monitored by the new operator if ownership changes.
5. Provision an owned OpenAI project with funds and permitted model access, plus an owned legal vector store. `scripts/create-legal-vector-store.mjs` creates billable resources and requires source-review judgment; an existing store does not automatically become accessible to a buyer's key. Preserve/document official-source metadata and permissions.
6. Set root web `VITE_CONVEX_URL` to the matching cloud URL. Set Android Gradle `CREDIT_AI_GATEWAY_URL` to the matching HTTPS **site** origin or `/credit-agent` endpoint. Native HTTP routes use the site URL; the web Convex client uses the cloud URL.

## 4. OpenAI configuration is explicit and live

`convex/creditAgentConfig.ts` exports the versioned system prompt, default model, 60-second deadline, six tool rounds and output-token budget. `creditAgentCore.ts` imports it for actual Responses API calls; it is not a sample disconnected from runtime. `CREDIT_AGENT_MODEL` can override the model. Case-tool/source-retrieval/consent enforcement remains in server code even if prompt wording changes. Existing regression tests cover forced case summary, usable retrieved legal sources, truncated output, deadlines, consent and billing ownership/deletion races.

After any prompt change, run backend regressions/typecheck and a funded fictional native case. Record prompt version, source commit and retrieval evidence. Do not trade away source grounding, user-only filing or privacy safeguards to make a demo appear successful.

## 5. Native authenticated acceptance before buyer demo

Install a certificate-correct APK; verify Google and email sign-in; accept current policies; opt into AI; import only synthetic fixtures; verify the native authenticated response with source citations; test sign-out/account isolation and disposable-account deletion. Test the Play-delivered build separately because Play uses its app-signing certificate. A passing mock test or valid signing certificate does not prove these integrations.

## 6. Signed packaging and Play handover

Use `.github/workflows/signed-android-release.yml` with the existing owner upload key and a version code above all prior Play uploads. Workflow edits on `android-native-rebuild` trigger a signed build; manual dispatch requires its definition on the default branch. APK is for direct installation; AAB is for Play Console. Current approved contact is `admin@synergyfundai.com`, with repository overrides supported. Both native/server paid modes must agree.

Complete the closed-test record and obtain Google's production-access decision before claiming that operational hurdle is cleared. Keep public production rollout unpublished if the buyer wants a draft. Lawyer review, independent security review, real devices/current permitted bureau layouts and billing account permissions remain separate acceptance work.

## Troubleshooting map

| Symptom | First check |
| --- | --- |
| Google chooser fails or returns no usable credential | Firebase Android package/signing registration, genuine downloaded configs, web OAuth client and Play-services account; see exact fingerprint checklist. |
| AI 401 | Wrong Firebase project/issuer or missing/expired Firebase ID token. |
| AI 403 | Email not verified or current affirmative terms/AI sharing missing. Verify email and re-sign in to refresh claims. |
| AI 402 | Paid mode enabled without an active verified Play entitlement. |
| AI 429 | Distinguish account rate limit from provider billing credit exhaustion; inspect protected operator logs without exposing tokens. |
| AI legal-source unavailable | Vector-store ownership/metadata/retrieval results; do not remove the grounding gate. |
| Release build fails preflight | Missing signing/gateway/contact/version inputs; do not generate a new signing key for an existing listing. |
| Empty/incorrect import | Inspect per-case metrics, extraction method and current bureau layout; do not invent missing account fields. |

Secrets are handed over out of band through an owner-approved secure mechanism. This package contains configuration templates/schema/test evidence only, no consumer database export or credentials.

## October 15 source update

See [the engineering completion record](../ENGINEERING_COMPLETION_2026-10-15.md) for the new editable AI field-draft flow, annual subscriptions, verified case-specific one-time purchases, policy version 2026-10-15.1 and signed-release installation/update acceptance. The schema export now has 15 tables. Historical test/build results above apply only to their recorded commits; use the latest verification follow-up for the current release.

## 2.0.3 follow-up

See [the follow-up record](../ENGINEERING_FOLLOWUP_2026-10-16.md) for parser/account-comparison corrections and protected production deployment configuration. The production workflow requires its own matching production key and existing server/vector-store settings; it does not reuse the development key or provision paid resources. A tester release is not proof of production/Firebase/Play acceptance.
