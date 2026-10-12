# Parser validation — no real data in this repository

## Automated fictional fixtures

`native-android/app/src/test/resources/parser/` contains three deliberately fictional text fixtures. `ReportTextParserTest` runs the same pure text parser used by Android import. These fixtures check bureau/score recognition, account suffixes, balances, field boundaries and missing values. They are not current bureau PDFs and do not establish accuracy across actual layouts or OCR.

Run `cd native-android` then `./gradlew testDebugUnitTest`. Never alter expectations just to match a parser error: compare the expected account records with the fixture text.

The API-35 GitHub emulator also passed three generated PDF scenarios on source `2472e382`: embedded text, scanned on-device OCR and AES-encrypted fictional PDF with an empty document-user password. Run `./gradlew connectedDebugAndroidTest lintDebug` only on a disposable debug device containing fictional records. These fixtures contain no bureau login credential or consumer data; their success does not validate all current bureau formats.

## Permissioned real-layout validation — owner/testers

1. Obtain explicit permission for each report. Keep originals out of Git, public issues, chat, screenshots and CI artifacts.
2. Redact locally using an actual redaction/export tool, not a rectangle drawn over text. Remove name, address, SSN, date of birth, phones, email, full account numbers, barcodes, report IDs and document metadata. Extract the PDF text again to confirm removed values cannot be recovered. Keep only fictional replacements and account last-four values necessary for the test.
3. Prepare one embedded-text PDF and one scanned PDF per bureau, plus files with no score, no accounts, masked identifiers, repeated account headings, closed/paid accounts, and different reporting dates. Reports without scores are normal: expect no invented score.
4. Make a local expected-results sheet: bureau; score or null; account count; each creditor alias, suffix, balance/status, reported date, limit and remarks. A second person compares it with the redacted PDF.
5. Install the debug APK on a supported device; select each PDF through the document picker and through Share/Open with. Compare every extracted field. Record extraction method and omissions. Do not upload test PDFs to the production agent.
6. Verify unknown-bureau handling, OCR errors, concurrent import queue, interrupted import, rotation, leaving/returning, sign-out during import, and deletion during import/AI requests. No old result may return after deletion or account switching.
7. Confirm unsupported size/page layouts produce an explanation rather than partial hidden results. Current limits: 50 MB PDF, 200 embedded-text pages, 30 scanned OCR pages, and 4096-pixel maximum OCR page height. These are resource limits, not format-accuracy guarantees.
8. Report mismatches privately with the fixture alias and expected versus observed fields. Do not disclose the original consumer identity. An analyzer flag is always a review prompt; differences in dates or parsing can explain mismatches.

## Device walkthrough checklist

- Fresh install/sign-in → explicit unchecked agreement/adult confirmation → all five onboarding steps → dashboard. First AI use requires a separate optional data-sharing choice; declining leaves local tools available.
- Acquire reports with Custom Tabs; sign into official sites yourself; PDF sharing or Print → Save as PDF → import; inspect captured/missing bureaus.
- Finding explanation and citation explanation → AI response or clear configuration/auth error.
- Draft review checkbox → copy/share; guided portal fields → adjustable chunks → leave/return → saved receipt → recorded response/check-ins.
- Goal input → saved goal → prioritized local checklist → AI plan or clear error.
- Monthly plan → unavailable/disabled configuration state; Play license-tester purchase/pending/cancel/restore when owner setup exists.
- Clear case → import new fictional case → restart preserves that new case. Account switch must not show another account's case.
- Privacy/support/account deletion: recent sign-in failure must not say deletion completed. Cancel subscription separately in Play.
- TalkBack, large font/display size, keyboard open, narrow screens, dark theme; all actions remain reachable.

## Established automation and remaining manual acceptance

[Final source run 38241196324](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/38241196324) passed all 15 API-35 device tests and lint, including the three PDF scenarios, encrypted storage, agreement/onboarding, acquisition, filing receipts/responses and demo/navigation. Lint has zero errors and 29 warnings; see `VERIFICATION_RESULTS.md` for limits.

No complete human walkthrough or real-user feedback was obtained. TalkBack/device diversity, actual permissioned bureau layouts, native Firebase/Convex/AI, Google Play license lifecycle and release AAB/R8 remain unverified. The live model gate is blocked by exhausted API credit. Unit/device fixtures and APK assembly do not substitute for these checks.
