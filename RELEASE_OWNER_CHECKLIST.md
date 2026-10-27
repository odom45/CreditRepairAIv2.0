# CreditRepairAI release-owner checklist — October 26, 2026

Scope and status match [the current production assessment](docs/PRODUCTION_ASSESSMENT.md). Baseline: `302428f1ab977ffa17169b7d3398b3956d44124b` on `android-native-rebuild`; candidate: `parser-robustness-completion`.

**Agreed code follow-up: 3/3 complete (100% of this scope). Production acceptance: pending external gates.** No overall product-completion percentage is certified by this scoped change. The historical production-evidence score does not measure feature code completeness.

## Three-item engineering follow-up

- [x] Parser fuzz/property tests pass in CI, including resource boundaries, seeded text/PDF mutation, Unicode, empty/single/multi-page and real scanned-PDF OCR paths.
- [x] Expanded synthetic fixture corpus passes independent expected values for all three bureaus and multiple layouts: 18 new text fixtures and 24 generated embedded/scanned PDFs, preserving the original corpus.
- [x] Assessment and this checklist are synchronized to the verified candidate, inspected JUnit XML, artifact digests and configuration code.

Candidate source: `fcb2c4f531e70ba292696e318acc7c889092fb56`. [Parser robustness run 40434707769](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/40434707769). Both jobs passed. Inspected results: 79 JVM tests across 16 suites; seven API-35 parser test methods covering 137 PDF cases; zero failures, errors or skips. Android lint passed with zero errors/fatal issues and 29 retained warnings. [Machine-readable results](docs/PARSER_ROBUSTNESS_RESULTS.json) include verified artifact ZIP digests. This is the tested candidate, not a merged or newly packaged release.

Evidence to inspect: `parser-properties-junit` and `parser-pdf-ocr-junit` artifacts. Exact fixtures and expectations are in `native-android/app/src/test/resources/parser/expanded/manifest.tsv`, `ParserCorpusTest.kt`, `ParserPropertyTest.kt`, `ParserLimitsTest.kt` and `ParserRobustnessInstrumentedTest.kt`. The original 60-case Android batch also remains required. Resource limits and the limits of finite fuzz testing are documented in the assessment.

## External acceptance — kept separate from code completion

These boxes remain open until the owner has evidence. They are not erased or marked complete by the parser work.

- [ ] Qualified legal/commercial review of the product, pricing, advertising, contracts/cancellation, privacy, terms and jurisdiction requirements; apply approved revisions.
- [ ] Independent mobile/backend penetration test and any resulting remediation.
- [ ] Permissioned, redacted real-report validation across current bureau layouts; no real consumer reports belong in the committed synthetic corpus.
- [ ] Actual signed-app device and staging acceptance: email/Google sign-in, verified email, separate AI consent, Firebase → Convex → AI, account switching, deletion/re-authentication failure and offline/recovery flows.
- [ ] Accessibility, TalkBack, larger fonts, supported OEM/Android versions and measured performance acceptance.
- [ ] Play license-test purchase, pending/cancel/restore/expiry/grace/hold/refund/repeat-case behavior for monthly, yearly and per-dispute products; configure required server permissions first.
- [ ] Support/deletion mailbox delivery, approved public privacy/deletion URLs and accurate store/Data Safety/app-access materials.
- [ ] Actual closed-test participation and elapsed period required by the developer account, followed by owner Play submission and review.

A failed acceptance check can reveal a new code defect; remediate it and rerun the applicable automated checks. No real customer charge, refund, report submission or public rollout is authorized by checking off the engineering section.

## Current configuration and release procedure

1. Review the evidence for the exact application source being packaged. Parser validation uses no signing key or service credentials and performs no deployment. Merge the candidate only after its required CI checks pass; subsequent native changes need applicable checks again.
2. Firebase project: `credit-repair-ai-v2`; release package: `com.creditrepairai.v2`; debug package: `com.creditrepairai.v2.debug`. Preserve genuine downloaded configs and existing upload/Play signing registrations. Run `node scripts/firebase-signing-readiness.mjs --require-play-ready` before a Play-delivered build. A passing source/config check does not establish native sign-in.
3. Preserve the existing owner signing key. Never generate a replacement to resolve an installation/update failure. Use a version code higher than every previous Play upload when packaging the next release.
4. The signed-release workflow runs only when `CREDIT_RELEASE_READY=true` or `CREDIT_BUILD_PRODUCTION_TEST_CANDIDATE=true`. It uses protected `CREDIT_PRODUCTION_DEPLOY_KEY`, `CREDIT_PRODUCTION_DEPLOYMENT`, `CREDIT_PRODUCTION_GATEWAY_URL`, signing secrets and the support contact. Run the existing production-configuration and Firebase checks; do not substitute development credentials.
5. A deliberate **production-test** candidate requires purchases **disabled**. A public paid **production** build requires matching paid-access configuration and completed acceptance. The `CREDIT_BUILD_PRODUCTION_TEST_CANDIDATE` flag selects production-test mode when set. These are different modes; do not enable purchases merely to assemble an owner test build.
6. Intended pricing and introductory offer terms are in `config/launch-pricing.json` and the [pricing record](docs/LAUNCH_PRICING_AND_RELEASE_CANDIDATE_2026-10-17.md). The app displays actual Play price/eligibility/renewal data. A repository plan is not proof that products or offers are activated in Play. Complete license testing before paid access is enabled.
7. Verify release APK/AAB signatures, install/update/launch behavior and native acceptance for the packaged build. Upload to the intended test track only when approved; passing parser CI does not submit to Play or satisfy the human testing period.

Owner-approved support contact: `admin@synergyfundai.com` (fallback previously provided: `benjaminjodom45@gmail.com`). Delivery/monitoring remains an acceptance check, not an assumed result. Keep OpenAI keys, Convex deploy keys, service-account JSON, passwords, keystores and purchase tokens out of source and documents.

## Dated records

The [October 17 engineering record](docs/ENGINEERING_COMPLETION_2026-10-17.md), [Firebase repair](docs/FIREBASE_REPAIR_2026-10-17.md), [integration preparation](docs/INTEGRATION_ACCEPTANCE_2026-10-17.md), [verification history](docs/VERIFICATION_RESULTS.md) and [original parser measurements](docs/PARSER_BATCH_RESULTS.md) preserve their source-specific results and limits. Current service availability and live account state were not revalidated by this parser task.

The superseded [owner checklist](docs/history/RELEASE_OWNER_CHECKLIST_2026-10-12.md) and [assessment](docs/history/PRODUCTION_ASSESSMENT_2026-10-12.md) are historical snapshots. Follow this checklist for current status; do not execute older exhausted-credit, certificate-mismatch or paid-mode instructions as if they describe the current branch.
