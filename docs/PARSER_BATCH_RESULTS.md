# Measured Android parser batch — 2026-10-12

The strict final run passed all 60 expected outcomes: all 54 supported synthetic PDFs were fully correct, all 1,620 evaluated core account fields matched, and all six unsupported inputs were safely rejected. This is emulator evidence for the controlled synthetic corpus, not an accuracy estimate for all current bureau files.

| Measure | Unchanged parser | Final parser |
| --- | --- | --- |
| Supported PDFs opened | 54/54 (100%) | 54/54 (100%) |
| Fully correct supported reports, including bureau/score/count | 27/54 (50%) | 54/54 (100%) |
| Evaluated core fields | 1,176/1,620 (72.59%) | 1,620/1,620 (100%) |
| Unsupported inputs safely rejected | 6/6 | 6/6 |

## Reproducible source and raw evidence

Baseline source: `04e530c66ce5856c3ff1f24d395c857a881411ea`; [baseline run 38290903242](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/38290903242); [complete JSON/HTML artifact](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/38290903242/artifacts/11766955946).

Final parser source: `ab72ce6dbc21ea61d85bd792d40040814401cea1`; [strict run 38294994560](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/38294994560); [complete per-case JSON/HTML artifact](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/38294994560/artifacts/11768032492).

Artifact ZIP digests:
- Baseline: `314245477fcd925d2f17a87ad6cd669b5691615ed8be7a36f0ce8ca218d50dbc`.
- Final: `b758f566df0a20ea55a94a704fe5c3fbf884ac34ab87dab01bd9511b3e9dd844`.

The first fixture/provider/export harness attempt was invalid and is excluded. The valid baseline deliberately measured faults without requiring all cases to pass. Intermediate strict runs failed, with 42/54 and then 53/54 complete reports; their failures drove corrections. No expected values were lowered or altered to obtain the final pass. JUnit's success percentage is not the parser-accuracy metric.

## Corpus and acceptance

API-35 Android emulator, `sdk_gphone64_x86_64`; production `CreditReportParser`, Android content URIs/PdfRenderer, PDFBox Android and bundled ML Kit.

- Three bureau headers, 12 embedded-text variants each: upper/labeled/mixed-case creditors, split fields, whitespace, account-status label, no score, missing balance, inline fields and account-number aliases.
- Four scanned variants per bureau: plain/labeled/no-score/split values (12 total).
- Three AES-encrypted files with empty document-user password and three multi-page files.
- Six guards: non-PDF, blank PDF, password required, over 200 text pages, over 30 scanned pages and unsafe tall scan.
- Three expected accounts per supported file. Ten evaluated fields per account: creditor, suffix, balance, status, payment status, opened date, reported date, limit, type, responsibility. Bureau, score/null and exact account count are also required for a complete pass.

Fixtures and independent expected values: `native-android/app/src/androidTest/java/com/creditrepairai/v2/analysis/SyntheticParserBatchTest.kt`. Re-run `bash ../scripts/run-parser-batch.sh current true` from `native-android/` with a disposable running emulator/device, or use `.github/workflows/parser-validation-batch.yml`. Complete synthetic expected/actual rows are exported and validated as 60 cases before upload.

## Corrections supported by the evidence

Canonical label/value handling, inline field boundaries, title-case headings, account-status extraction, current versus high balance, no invented score from credit limits, no invented suffix from an empty number label, observed-digit/date/separator normalization, geometric OCR reading order/fragment spacing and whitespace-tolerant bureau logos. The OCR resolution and memory/page guards remain bounded. Production does not log consumer report text; diagnostic text is confined to generated test fixtures.

## Limits and remaining proof

No current real consumer/bureau PDFs or physical-device acceptance were completed. These generated layouts use a controlled font and records; they do not validate every bureau version, arbitrary multi-column/rotated/noisy scan, complex wrapped creditor, long account list or cent precision. Money is stored as whole-dollar integers, and original report fields must be verified before any filing. OCR label/separator heuristics require permissioned real-layout testing.

Firebase Console registration/live login, funded native AI, Play license lifecycle, real-user closed testing, qualified legal/security review and Google's production-access/app review remain separate gates. A signed build or this synthetic benchmark does not prove launch readiness.

## Buyer hand-off

- [Contractor blueprint](handoff/BUYER_HANDOFF.md)
- [Actual Convex schema and ownership map](handoff/CONVEX_SCHEMA.md), plus `handoff/convex-schema.json` (14 tables; no records)
- [Configuration input map](handoff/CONFIGURATION.example.json)
- Live OpenAI system prompt/request configuration: `convex/creditAgentConfig.ts`, imported by the actual agent engine; unchanged prompt and 20 backend regressions verified
- [Exact Firebase certificate/config correction](FIREBASE_SIGN_IN_FIX.md)
- [Closed-testing plan and private evidence template](CLOSED_TESTING_PLAN.md)

The native case remains encrypted on device. The companion web sample-report functions are distinct from the native product and are identified in the blueprint.
