# CreditRepairAI 2.0.9 — verified test candidate

Review date: October 29, 2026 (Pacific). Version 2.0.9 / 20261029. [PR #20](https://github.com/odom45/CreditRepairAIv2.0/pull/20) merged into `android-native-rebuild` at `a4e6580b8df17a1abc0f8ddb7d0242eeaa4ccb6c`.

## Resulting behavior

- A new app session and ordinary background/close require an explicit sign-in. Cached Firebase identity alone cannot reveal the case. Locking hides case data, fences pending work and blocks secure service tokens while retaining the encrypted same-account reports. Explicit sign-out retains its existing confirmed local-deletion behavior. Android document-picker continuations, rotation and an explicit Google sign-in can finish.
- The assistant adds report review, next-step coaching, evidence checks, factual letter coaching, bureau-response interpretation, mailing guidance, simpler explanations and copying. It uses bounded, redacted case-scoped conversation. Mobile answers use plain text; ordinary account terminology and blank answer labels no longer get corrupted by account-ID redaction. Incoming labeled opaque identifiers remain conservatively masked.
- Physical letters add a paid LetterStream website handoff after successful current-PDF export, review and separate-purchase acknowledgment. Users upload the completed packet, verify the proof, choose mail service and pay the provider. Actual confirmed mailing can be recorded after reauthentication without re-entering sender details.

## Automated and live evidence

| Check | Result and evidence |
| --- | --- |
| Native JVM tests | 84 passed, zero failures/errors/skips; [final-source run 41001575262](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/41001575262) |
| API 35 device tests | 43 passed, zero failures/errors/skips in the same run |
| Android lint | Zero errors/fatal issues; 29 existing warnings |
| Backend typecheck/regressions | 57 tests passed, zero failed; [final-source run 41001575262](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/41001575262) |
| Synthetic PDF/OCR corpus | All 78 outcomes passed; [run 41001179066](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/41001179066) |
| Real-provider interactive AI | Two-turn fictional dialogue passed; five HTTP 200 responses, 24 retrieval results, no API errors; document context, a continuing question and readable account terminology verified; [run 41001570920](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/41001570920) |
| Production AI deployment | Passed; [run 41002517089](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/41002517089) |
| Signed APK / update verification | Passed: APK v2/v3 and AAB verification, previous install, in-place update and launch on API 36; [run 41002516889](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/41002516889) |

Final PR source `c7f66a940a240a9735a1ce51c799edadc2165ac8` passed the complete native/backend workflow. The Android sources and tests are identical to the earlier verified source `e043cf8196ce002ef8419d541e5bcee6a9881742`, whose archived XML reports were independently counted: 84 JVM and 43 device tests, zero failures/errors/skips. Final-source backend and live-provider checks also passed. Merge reruns are additional evidence for the same implementation.

Live tests used fictional data and the existing protected server configuration. They did not authenticate a real native Firebase user or mail a real letter. The dialogue was reviewed; this is evidence for the exercised scenarios, not proof of universal legal correctness or every future response.

## Mailing and purchase scope

The mailing feature is external provider checkout. CreditRepairAI does not collect its own mailing markup, transmit an API mail order, automatically upload identity/evidence documents or synchronize postal status. LetterStream quotes and charges the service separately. App purchases remain disabled in this production-test candidate. [Mailing workflow and remaining integration work](PAID_MAILING.md).

Prior agreed app purchase plans remain $24.99/month for three months then $30/month; $199.99 for the first year then $249.99/year; $9.99 for one saved dispute. These are planned prices, not proof of live Play activation.

## Phone acceptance

Install the signed update over the existing signed app to retain encrypted local reports. Do not uninstall as part of an ordinary update. Check fresh sign-in, restoration of the same-account case, background/reopen lock, one contextual AI follow-up and the paid-mail handoff on the owner's phone. No owner-phone acceptance is inferred from emulator or provider tests. [Demo brief](STARTUPS_DEMO_2.0.9.md).

## Signed APK identity

- Filename: `CreditRepairAI-2.0.9-Updated-Signed.apk`
- Package: `com.creditrepairai.v2`; version code `20261029`.
- Bytes: 48210700.
- APK SHA-256: `c0dbc442cac4b8618e3d131ef9225ffbb6fca469b0ba9a3abf9b8815e0ada4e3`.
- Signing certificate SHA-256: `1ec8aaa5a3b259ac8d0ec16f66b805ddf82e32a73fc5f486dce320844e8d09ad`; matches the prior owner's signed APK.
- [Build artifact](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/41002516889/artifacts/12954661087).

The downloaded archive matched its published digest; APK ZIP integrity and the packaged new-feature strings were checked. The signed test APK was made available. This does not establish actual owner-phone authentication, a real postal purchase, live app purchases or a public store release.

