# CreditRepairAI 2.0.5 — verified engineering follow-up

Native application source: `6440464cff5bfe364e16b4721649989c32dc6f29`. Source-maintenance/backend workflow follow-up: `8cc790499a1a517b76c70ae3648f0496d2cd6a05`. Branch: `android-native-rebuild`.

## Completed in this update

- AI serialization no longer silently takes the first 200 accounts, 100 findings or 100 disputes. An oversized whole-case request is refused with an explanation and a route to focused guided-filing assistance. Case selection is applied before checking the limits; every imported record remains saved. A missing/deleted selected dispute cannot fall back to the whole case.
- A failed AI/service request remains visible in chat but cannot overwrite a previously saved goal plan. Successful replacement still requires the same goal description and target period.
- Filing edits preserve saved receipts, submission/response dates and reminders. Old reviewed UI records cannot record submission after facts change; deleted disputes cannot be resurrected by a delayed filing event. The model requires the final reviewed step, blocks repeat receipts, and permits response recording only for an actually submitted saved filing.
- CFPB preparation and the AI snapshot use the same prior-bureau-response test. A CFPB response, future timestamp, or unsent filing cannot stand in for a response to a prior bureau dispute. This is a saved-record check, not a legal eligibility determination or observation of a portal.
- Renewed expired source-index reviews after checking all 51 state/DC portal URLs and retrieving all eight official opinion PDFs. Corrected observed portal redirects and broken entry points. All state coverage remains registry-only; attorney treatment/citator review remains pending. Per-source review scope and access limits are recorded in `legal-library/reviews/`.
- Corrected Gorman's source from the original January opinion to the October amended opinion matching the existing citation/date. The live development vector store reused its identity. Both corrected attachments completed indexing before the two old store attachments were detached. Original global uploaded files were retained. Maintenance is retryable and does not create a store.

## Verified evidence

| Check | Result and scope |
| --- | --- |
| Android JVM | 65 tests; zero failures or ignored tests. Actual downloaded Gradle report inspected. |
| Android device | 26 API-35 scenarios; zero failures, errors or skips. Includes stale receipt/deleted-filing and saved-plan failure regressions. Offline fixture identities do not prove Firebase sign-in. |
| Parser | 54/54 supported synthetic reports exactly correct; 1,620/1,620 evaluated core fields; 6/6 unsupported files rejected. Reconstructed the complete batch metrics from the actual device log. No real consumer reports were used. |
| Android lint | Zero errors/fatal issues; 29 existing warnings retained. |
| Backend/configuration/maintenance | All 47 tests passed, including both-source indexing before replacement, retaining originals on failure and reusing completed sources on retry. |
| Build and configuration | Frontend build, frontend/Convex typecheck, policy synchronization, official-source check including links, source credential-pattern scan and workflow YAML parsing passed. Scans do not replace an independent security review. |
| Signed release | APK v2/v3 checks and AAB JAR verification passed on the release runner. Prior-release install, in-place update to 2.0.5 and release launch passed on Android API 36. |
| Downloaded binaries | GitHub ZIP SHA-256 digests matched before extraction. APK/AAB ZIP integrity and the existing owner certificate SHA-256 were independently checked locally. |
| Live development AI | Successful fictional request after source refresh: three HTTP-200 provider responses, 16 accepted source results, case-tool checks, educational label and retained FCRA reference. Not a native Firebase-authenticated phone request. |

Runs:

- [Signed release and API-36 update acceptance](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/39035948297).
- [Native application validation and device/lint reports](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/39035948305). Unit, build, dependency/preflight and device/lint jobs passed. Its initial backend validation stopped at the newly expired review-date check; the overall run is failed and deployment was skipped. This failure is not hidden or presented as green.
- [Source refresh, 47 regressions, development deployment and live AI acceptance](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/39036384149) passed after the reviewed source maintenance.
- [Official-source freshness and link checks](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/39036384184) passed. Restricted/unavailable registry-only portal retrievals remain documented and do not approve state-law text.
- [Full follow-up native/backend workflow](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/39036384244) covers the maintenance commit; inspect its final status separately. No packaged native source changed between these two native runs.

## Deliverables

Both files use package `com.creditrepairai.v2`, version `2.0.5`, version code `20261018`. Paid access was explicitly false in the signed release. Existing owner certificate SHA-256: `1ec8aaa5a3b259ac8d0ec16f66b805ddf82e32a73fc5f486dce320844e8d09ad`.

| File | Bytes | SHA-256 |
| --- | ---: | --- |
| CreditRepairAI-2.0.5.apk | 48,177,947 | `0139518cd157bca8ce0d3c4045cd3894b172ac5fe6411a0fcdb83fbd969c21c0` |
| CreditRepairAI-2.0.5.aab | 28,305,912 | `05a83b06414f1c1c162aae71de94ce3100fb3de16bfe8f53c649f6a2772041ec` |

## Remaining account and external acceptance

1. Genuine Firebase package/certificate registrations and refreshed configurations, correct providers, and Google/email sign-in on the exact signed APK. The existing downloaded configs still lack the owner signing certificate and incorrectly share one Firebase app ID. The last live certificate check returned `INVALID_CERT_HASH`; this update cannot register a certificate by editing local JSON.
2. Verified native sign-in → email verification → separate AI consent → Convex → AI, including owner-phone and deletion/re-authentication failure paths. Emulator offline identities and server-side fictional AI checks do not establish this chain.
3. Explicit production deployment credentials/configuration and protected production acceptance. The working tester gateway remains the authorized development deployment.
4. Play products, server-account permissions and real license-test purchase/restore/expiry/refund behavior before enabling purchases. No charge or refund was performed here.
5. Monitored support/deletion contact, permissioned current bureau layouts and wider device/accessibility acceptance, independent legal/security reviews and remediation, real closed-test participants and the elapsed testing period, then owner Play submission.

No code, testing, review or deployment in this record establishes complete commercial launch readiness or predicts dispute outcomes. No real complaint/dispute was submitted, no identity provider was bypassed, no new signing key was generated, and no production deployment was selected.
