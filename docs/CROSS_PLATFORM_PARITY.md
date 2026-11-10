# Cross-platform parity acceptance

Baseline: Android `android-native-rebuild` commit `0b28929d016876abbc5b5ee173461844bb397bfa` (2.0.9 candidate), with web on `main` and native Apple clients on their dedicated branches.

This is an implementation and verification record, not a production launch approval. No completion percentage is claimed. A compiling client is not evidence of live account acceptance.

## Platform branches and current evidence

| Platform | Branch / commit | Automated evidence | Remaining acceptance |
| --- | --- | --- | --- |
| Web | `main` / PR #23 merged | Post-merge web validation passed: [run 42737833248](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/42737833248). | Live Firebase, CORS, AI, billing, deletion, and real-report acceptance. |
| Android | `android-native-rebuild` / `0b28929` | Post-transfer native device tests passed in [42737792061](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/42737792061), journeys in [42737792195](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/42737792195), measured parser in [42737792155](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/42737792155), and signed APK/AAB plus emulator install/update smoke in [42737792243](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/42737792243). | Fresh legal-source review before backend/admin gate can pass; live Firebase/AI/billing; real-device acceptance and Play closed testing. |
| iPhone / iOS | `ios-native` / `264aa16` | Previous base validation passed: [run 42736736736](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/42736736736). Latest email-verification gate, five-minute return flow, and Safari portal handoff are queued in [run 42740736757](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/42740736757). | Firebase app registration/config, Google sign-in, signing, real iPhone OCR/accessibility, and device acceptance. |
| iPad / iPadOS | `ipados-native` / `651499e` | Previous base validation passed: [run 42736740789](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/42736740789). Latest verification and Safari handoff changes are queued in [run 42740741237](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/42740741237). | Firebase app registration/config, signing, real iPad UI/accessibility and device acceptance. |
| Mac / macOS | `macos-native` / `e1bb1ef` | Previous base validation passed: [run 42736744999](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/42736744999). Latest verification and browser-handoff changes are queued in [run 42740746766](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/42740746766). | Firebase app registration/config, Developer ID signing, notarization, and real-Mac acceptance. |

Apple's CI uses the shared SwiftUI source and runs all three Xcode schemes: iPhone and iPad simulator builds plus the macOS regression suite. This is compile/test evidence, not a signed Apple distribution artifact.

## Shared functional contract

The goal is functional parity, not pixel-identical interfaces. Each platform should provide the same user-visible case workflow while using its native security and interaction model.

- **Identity and consent:** Firebase email/password and Google sign-in, verification/resend/refresh, explicit sign-out, fresh-session protections, current policy acceptance, and affirmative AI-sharing consent. Unverified Apple users see a verification screen before vault unlock or case tools; returning from Mail refreshes pending verification.
- **Report intake:** Equifax, Experian, and TransUnion PDF import; local embedded-text extraction and scanned-page OCR; bounded parsing; user review of extracted fields against the original report.
- **Analysis:** comparable account fields, bureau differences, evidence gaps, and supported findings. A parser/comparison flag is never treated as proof of an error.
- **AI guide:** same authenticated Convex legal-agent contract, bounded redacted case snapshot, case-scoped context, follow-up questions, bureau-step explanations, editable draft answers, and actual-response explanations. Original PDFs, SSNs, full account numbers, private letter sender fields, and mailing tracking numbers must not be sent.
- **Guided filing:** bureau-specific and CFPB preparation, editable factual fields, review gates, copyable chunks, official portal handoff, and recording of actual receipts and responses. Android uses Chrome Custom Tabs; iPhone/iPad use SFSafariViewController; Mac uses the system browser. The user controls credentials, attachments, and final submission.
- **Physical letters:** separate bureau letters, private sender details kept local, multipage PDF export, and mailing/delivery/response tracking. Android additionally supports native printing.
- **Planning and tracking:** goals, saved AI plans, dated manual scores, freeze-action checklist, dispute status, and follow-up check-ins.
- **Case portability:** versioned AES-GCM envelope, PBKDF2-SHA256 derivation, random salt/IV, and Firebase UID bound as authenticated additional data. Explicit export/import requires the same Firebase account and transfer passphrase. PR #22 merged the Android bridge; PR #23 merged web vault hardening.
- **Deletion and access:** owner-bound case storage, account-generation/session fences, explicit local clear/delete flows, backend ownership/verification, and existing subscription or case-scoped entitlement enforcement. New Apple checkout remains disabled in this candidate.

**Automatic cloud case synchronization is not implemented.** A shared Firebase login does not move a case between devices; the user initiates an encrypted transfer.

## Platform-specific security

| Platform | Local security model |
| --- | --- |
| Android | Android Keystore AES-GCM, `FLAG_SECURE`, disabled backups, redaction before network calls, and a five-minute return window for temporary background handoffs. Explicit app close locks the session; process recreation requires fresh sign-in. |
| Apple | CryptoKit encrypted vault, Firebase-UID-bound envelope, key material held in memory, iOS file protection and backup exclusion, restricted macOS file permissions, and the same five-minute background-return grace with verification refresh. |
| Web | Passphrase-encrypted AES-256-GCM vault with PBKDF2-SHA256, random salt/IV, Firebase UID bound as authenticated additional data, and no persisted passphrase/key. Exact allowlisted-origin CORS is required. |

The Android portal handoff uses secure Chrome Custom Tabs, not a WebView that reads or controls bureau pages. Guided instructions remain in the app; the user handles portal credentials, attachments, and submission.

## Verification performed November 9, 2026

- Android pre-transfer baseline: native device tests, account/dispute/physical-letter journeys, and measured synthetic parser batch passed. The signed APK and AAB passed signature verification; emulator previous-install, in-place update, and release launch checks passed. Those artifacts were built from commit `c48d77b`, before PR #22 was merged.
- Android post-transfer branch: native device tests, journeys, parser batch, staging preflight, backend regressions, signed APK/AAB, and emulator install/update smoke passed against merge commit `0b28929`. The separate backend/admin legal freshness gate remains failed.
- Apple base branches passed web checks, iPhone/iPad simulator compilation, and macOS regression tests. The latest email-verification screen, background-return, and Safari portal-handoff changes are queued for revalidation.
- Web: PR #23 merged into `main`; post-merge web validation passed.
- Synthetic fixtures and mocked provider tests do not establish accuracy for every live bureau layout, successful production AI, or customer account acceptance.

## Release blockers and unverified gates

- The Android backend/admin validation currently fails because the federal legal-library review is 55 days old while its configured freshness limit is 45 days. The date must not be falsified and the freshness threshold must not be weakened. Refresh and document the actual official-source review before treating this gate as passed.
- Live same-account email/Google sign-in, verification refresh after opening an email link, consent revocation, AI requests, entitlement restoration, account switching, recent-auth deletion, and encrypted transfer need staging acceptance.
- Test on real Android, iPhone, iPad, and Mac hardware. Verify OCR, accessibility, app background/return, file protection, and account lifecycle behavior.
- Apple bundle registration, Google client configuration, signing, Keychain/file access, Developer ID notarization, and distribution remain owner-controlled gates. The current latest Swift changes must also complete their queued CI runs.
- Android Play closed testing, live billing, legal/security review, and real-report validation remain release-owner gates.
- No automatic cloud case sync, live legal-agent acceptance, or app-store approval is claimed.

Keep release status **HOLD** until all applicable owner and live-device gates have evidence. Branch existence, source completeness, simulator compilation, and signed test artifacts are not production launch approval.

## Change history

- PR #22 merged into `android-native-rebuild` at `0b28929`: encrypted Android/web/Apple case-transfer bridge.
- PR #23 merged into `main` at `dd553a6`: vault validation, deletion/save fencing, explicit local-case clear, and host-portable package setup.
- Apple build fixes: corrected the missing guided-filing view closure, bridged Firebase callback APIs with Swift concurrency, and assigned a unique Swift module name to the macOS test target. Follow-up parity fixes gate vault access on email verification, allow a five-minute return from email/portal handoffs, and use SFSafariViewController on iPhone/iPad.
