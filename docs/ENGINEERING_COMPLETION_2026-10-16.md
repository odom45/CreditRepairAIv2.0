# Funded AI and native authentication follow-up: 2.0.4

Application changes: `d0142cb241850aa0940b71d9641caedcfa9d4c9f`; test-fixture lint correction: `238129170fd4f0ced583e801cfb6de58a05dffc6`, branch `android-native-rebuild`. The correction collects test UI state reactively and changes no packaged application code. Follow-up `6d38b182966f39d21486231b222f68b8fa0f8fb4` also clears the authentication busy flag when a case is invalidated; an added device regression delays verification, clears the case, rejects the old reply and proves a new refresh can run.

## Completed code

- Login errors now use allowlisted, actionable categories for credentials, disabled providers, app authorization, network failures, throttling and expired sessions. Raw provider messages, tokens and account details are never rendered. Invalid-email/password/not-found responses share one credential message.
- The explicit Google sign-in button now uses Credential Manager's `GetSignInWithGoogleOption`, retaining the real configured web OAuth client. A canceled chooser is distinguished from a missing credential and an unavailable provider. This does not register a certificate in Firebase.
- Password recovery is available from the sign-in screen after the user enters an email and explicitly presses its button. Its completion message does not disclose account existence. No password is required to request recovery.
- Settings and the AI screen show email verification controls. Refresh reloads Firebase's current user and force-refreshes the ID token so the backend can observe an updated `email_verified` claim. Resending requires an explicit user action. An account that was created successfully is not reported as failed creation merely because verification-mail delivery failed.
- Unverified accounts receive a local explanation before an AI request; the backend's verified-email gate remains enforced. Local tools remain available to signed-in users.
- Authentication coroutine cancellation is propagated. Verification results are fenced to the same signed-in owner and case generation.
- Release defaults advance to 2.0.4 / versionCode 20261017, retaining the existing owner signing key. Five JVM error/privacy regressions and five offline device scenarios were added; no real mail is sent by the fixtures.

## Live AI check after the owner's funding update

The failed development job in [run 38838060702](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/38838060702) was rerun on October 16. It deployed checked backend source to the existing authorized development deployment. Its real fictional-case request passed: three provider responses were HTTP 200, eight legal-source retrieval results were accepted, case-tool checks completed, and the answer included the required educational label and FCRA reference. No provider error code was returned.

The test privately reads the existing deployment's server API key and legal store. It does not create a store, replace credentials, expose secrets, or use consumer reports. This confirms the configured agent/retrieval path works after funding. It does **not** prove a native Firebase-authenticated HTTP request from the owner's phone, production deployment, or remaining API balance.

## Firebase remains an account configuration blocker

At 2026-10-16T19:33:15Z, a fresh read-only public configuration check explicitly supplying `androidPackageName`, `sha1Cert` and `firebaseAppId` rejected the existing owner upload certificate with HTTP 400 `INVALID_CERT_HASH`. The comparison with the older registered certificate returned HTTP 200 and project number 287473718478. A generic reachable config endpoint without those query parameters is insufficient to establish certificate registration.

The Firebase plugin exists but remains unconnected. This session has not changed provider settings, created OAuth registrations or obtained genuine refreshed Firebase configs. Follow [FIREBASE_SIGN_IN_FIX.md](FIREBASE_SIGN_IN_FIX.md) in the existing project. Neither generating a new signing key nor editing a certificate string into JSON would correct the server registration. Email/password login on the owner's phone is still unverified; its exact failure must be distinguished by the new safe diagnostics.

## Verification record

- Final backend/configuration/output regressions: 44 passed locally; the focused [funded-agent recovery run](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/38958991401) checks and deploys the same backend source. Native application source `6d38b182966f39d21486231b222f68b8fa0f8fb4` passed 58 JVM tests, 23 device scenarios, lint and all protected configuration gates in run 38957441781. Backend-only follow-up `95cab13a7f9ca7d90a10c8afaa7b2ae4b020d95f` does not alter the packaged APK/AAB.
- Frontend and Convex typecheck, production web build, generated policy synchronization, legal registry check and current-source credential pattern scan passed.
- The initial native run verified 58 JVM tests and 22 device tests with zero failures/errors/skips. Lint then found one composition-state-read error in the newly added test fixture; it was corrected rather than suppressed. [Corrected native checks](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/38957441781) and [final signed release plus install/update acceptance](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/38957441952) provide the final check results.
- The first 2.0.4 signed release already passed signatures, Android 16 installation, launch and an in-place update from the previous owner-signed release in [run 38955785291](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/38955785291). No phone-specific sign-in or Play purchase acceptance is implied.
- The measured 60-case parser batch passed for unchanged parser source in [run 38838060712](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/38838060712). This authentication follow-up does not change the parser or turn synthetic cases into real-report evidence.

## Remaining acceptance requirements

1. Correct actual Firebase registrations/provider configuration and verify Google and email/password on the exact signed APK, including email verification and authenticated AI consent/gateway requests.
2. Provide the existing production deployment's protected credentials/configuration and perform the separate production deployment/acceptance checks. The tester gateway is still development.
3. Configure Play products and service-account permissions and run license-tester purchase, restore, expiry and refund cases before enabling paid access. Purchases remain off.
4. Verify the support/deletion mailbox, the owner's phone installation, permissioned real reports and account deletion/re-authentication behavior.
5. Complete independent legal/security review, remediate findings and finish the required real-person closed-test period and owner Play submission. Those external outcomes cannot be replaced by these automated checks.

Implementation references: [Firebase user management](https://firebase.google.com/docs/auth/android/manage-users), [Google button flow](https://developer.android.com/identity/sign-in/credential-manager-siwg-implementation), [public certificate validation](https://docs.cloud.google.com/identity-platform/docs/reference/rest/v1/TopLevel/getProjects).

## AI output handling corrected after funded testing

A subsequent live request completed its case tools and eight source retrievals but failed the final FCRA-reference check. Its raw answer was not retained, so that historical failure alone does not identify the exact offending sentence. Source review and deterministic regressions exposed two real output-handling defects: the outcome filter rejected explicitly negated statements such as “does not prove an error,” and answer redaction silently cut everything after 4,000 characters, including late citations.

The filter now scopes a narrow explicit negation to the individual claim and still rejects a later affirmative promise, an unrelated negation or “not only guaranteed…” wording. It also rejects “guarantees approval.” Answer redaction retains the full bounded answer through 16,000 characters; oversized answers produce an explicit retry message rather than a partial legal answer. Request/snapshot text keeps the original 4,000-character bound and all identifier redaction remains in force. Five additional backend regressions cover these cases. The smoke report now records only safe answer-length/filter/reference metadata to distinguish future failure categories; it never logs a consumer answer.

Final binary SHA-256 (native source 6d38b18):

- APK: `1ec2ddc6633ef0fd7d47064bc70a501ef8c5f316dc10f61d726f8679d64b0f8d`
- AAB: `4e7c82933f23cbfa880e4065d4e0355f144b070b11709781ba7921508da82055`

These binaries passed signature checks and Android 16 installation, launch and in-place update at versionCode 20261017 in run 38957441952. The unchanged parser retains its previously measured synthetic evidence; native sign-in, real reports, Play purchases and a consumer phone remain separate gates.

## Final funded-agent acceptance

Focused recovery run `38958991401` passed all 44 backend regressions, frontend/Convex typecheck, policy synchronization, legal registry and source credential scan, then deployed backend `95cab13a7f9ca7d90a10c8afaa7b2ae4b020d95f` to the existing development deployment. Its final live fictional request passed: all provider responses were HTTP 200, eight source results were retrieved, the raw answer was 4,044 characters, and the displayed answer was 4,111 characters including its educational label. The FCRA reference was retained and the outcome filter did not reject the answer. This remains a server-side fictional request rather than native authenticated acceptance.

The final native run `38957441781` passed every job, including all 23 device tests and lint, before this backend-only recovery deployed. The signed binary source remains `6d38b182966f39d21486231b222f68b8fa0f8fb4`. Purchases were explicitly false in that release's configuration. No pending repeat build for backend-only changes is needed to establish those native results; it does not change this binary's code or certificate.

The two signed downloads were saved as `CreditRepairAI-2.0.4.apk` and `CreditRepairAI-2.0.4.aab`. Account configuration and the external acceptance requirements listed above remain open. No production-ready or fully working phone-login claim is made.
