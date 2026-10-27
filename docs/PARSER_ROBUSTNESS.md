# Native parser robustness coverage

All test documents are invented. No bureau-provided templates or real consumer data are used.

## Corpus

- Checked-in text: `native-android/app/src/test/resources/parser/corpus/manifest.tsv` lists 18 fixtures and independent expected bureau, score, account suffixes and balances. Each of Equifax, Experian and TransUnion has inline, split-value, alias, unicode-space, multi-page/form-feed and empty-account variants. The three original fictional fixtures remain.
- Generated PDFs: `SyntheticParserBatchTest` produces 72 supported cases: each bureau has 17 embedded layouts, five image-only OCR layouts, one encrypted PDF with an empty user password, and one three-page embedded PDF. Six additional guards cover invalid bytes, blank, password-required, 201 embedded pages, 31 scanned pages, and an unsafe tall OCR page: 78 total.
- `PdfParserFuzzTest` adds 24 PDFs containing mutated report text, 48 seeded byte-mutation/truncation cases, three seeded one-to-three-page image-only OCR documents, and a >50 MB rejection case. Existing `PdfParserInstrumentedTest` still checks embedded, scanned and encrypted inputs. The 48 mutations allow either a valid bounded parse or an expected invalid-input rejection; they do not claim extraction accuracy for corrupted inputs.

## Properties and limits

`ParserPropertyTest` uses seeds 7, 42 and 20261026 with 800 mutations each. Mutations include truncation, deletion through truncation, inserted control/unicode/surrogate characters, shuffled fields, duplicated sections and arbitrary character substitutions. Properties require bounded fields/accounts/money, valid optional scores and deterministic extracted fields (excluding deliberately random IDs). Generated valid reports assert exact account boundaries at 0, 1, 2, 20 and 150 accounts across all bureaus; the existing regression rejects 151.

Explicit boundaries cover character, line/field, field-count and extraction-writer limits. Limits reject rather than silently truncate reports. Resource-limit errors, cancellation, VM errors and arbitrary test failures are not treated as successful fuzz outcomes. PDF structural/argument errors may be expected rejections for corrupted byte inputs.

JVM test heap: 256 MB. JUnit timeouts: 15–60 seconds for text tests, 30–180 seconds for individual PDF tests, 600 seconds for the measured corpus. Each mutated PDF must finish in under 10 seconds; the workflow job is bounded to 40 minutes. Timeouts are test enforcement, not a production process-isolation guarantee. PDFBox parsing still executes in the app process, and mixed scratch memory does not cap the whole dependency heap.

## Reproduce

From the repository root:

```sh
python3 scripts/check-parser-evidence.py
cd native-android
bash ./gradlew testDebugUnitTest --stacktrace
# With an API 35 Google APIs emulator/device connected:
bash ../scripts/run-parser-batch.sh current true
node ../scripts/summarize-parser-batch.mjs parser-validation-current.json
```

The parser workflow runs on native-branch pushes, native-branch pull requests and manual dispatch. The PR workflow needs no account credentials or production deployment. It uploads JVM XML/results and Android instrumentation reports plus measured JSON. The JSON must contain all 78 cases; exact-field, bureau, score and extraction-method expectations must pass for all 72 supported PDFs, and all six guards must reject. Instrumentation/process crashes or failed tests fail CI even if corpus metrics were already emitted.

## Evidence interpretation

The current status and exact CI run are in `PARSER_ROBUSTNESS_EVIDENCE.json`. Both active assessment documents must have the same status marker. Historical release-readiness scores are archived. A passing synthetic run closes this scoped implementation/test follow-up; legal review, penetration testing, Play submission/closed testing, real-report validation and device/staging acceptance remain separate owner gates.

Verified on October 26, 2026 (Pacific) in [CI run 40434452161](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/40434452161), source `8b3284f91f322a9b9419f6b5e4b736d530cb8ac4`: JVM suite passed; 8/8 Android parser tests passed; 72/72 supported PDFs exact; 6/6 guards safely rejected; 1,950/1,950 fields matched. Corpus runtime: 126,186 ms.
