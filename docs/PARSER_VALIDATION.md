# Parser validation — no real data in this repository

## Current automated synthetic coverage — October 26, 2026

PR #16 is merged into `android-native-rebuild`. [Final PR CI](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/40435571642) passed the JVM suite and all eight API 35 Android parser tests. The 78-case measured PDF corpus has 72 exact supported outcomes, six safe rejections and 1,950 expected fields matched. All three bureaus are covered by multiple embedded/OCR layouts.

The text corpus contains the three original fixtures and 18 new fixtures (six layouts per bureau) with independent expected values. The suites add 2,400 seeded text mutations, generated account properties, 24 PDFs with mutated text, 48 malformed/truncated PDF byte mutations, three seeded image-only/multi-page OCR inputs and oversized-input rejection. [PARSER_ROBUSTNESS.md](PARSER_ROBUSTNESS.md) gives exact coverage, seeds, bounds and commands; [PARSER_ROBUSTNESS_EVIDENCE.json](PARSER_ROBUSTNESS_EVIDENCE.json) ties results to tested commits and runs.

Every fixture is synthetic. No current real-bureau-layout accuracy or complete human walkthrough is established by these tests. Never alter independent expectations merely to match a parser error.

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

## Automation and remaining manual acceptance

The current parser evidence is summarized above. Historical full-device, lint, signed-build and live-agent results remain in [VERIFICATION_RESULTS.md](VERIFICATION_RESULTS.md) and its dated records. Those earlier counts and account-state observations are not new verification of the merged snapshot.

Complete human walkthroughs, TalkBack/device diversity, permissioned real layouts, native Firebase/Convex/AI and Play license lifecycle acceptance remain in the [owner checklist](../RELEASE_OWNER_CHECKLIST.md). Synthetic tests and assembly do not substitute for those checks. Earlier exhausted-credit statements are historical; this document does not recheck current account balances.
