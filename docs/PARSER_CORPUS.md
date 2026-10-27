# Expanded synthetic report corpus

All text is invented test data. No actual consumer records, account names, account numbers or bureau PDFs are included. Bureau names identify the header under test; these layouts do not claim to reproduce current official bureau formats.

`native-android/app/src/test/resources/parser/expanded/manifest.tsv` enumerates 18 fixtures: three bureaus × six layouts. `ParserCorpusTest` holds full independent account expectations, including null/empty fields, so test output cannot become its own expected value. The original three fixture files in `native-android/app/src/test/resources/parser/` remain unchanged.

Generated device PDFs live in `ParserRobustnessInstrumentedTest`: three bureaus × four layouts × embedded/scanned = 24 PDFs. The original measured 60-case corpus remains in `SyntheticParserBatchTest`. Randomized tests use fixed seeds recorded in the test source and assessment. A new failing seed must be reproduced and corrected, never ignored by widening the acceptable exception set.

Current verified source, seeds, CI run and artifact digests are recorded in [the parser result record](PARSER_ROBUSTNESS_RESULTS.json) and [the assessment](PRODUCTION_ASSESSMENT.md).
