# CreditRepairAI release-owner checklist

Current review: **October 26, 2026**. Native 2.0.7 source; [PR #16](https://github.com/odom45/CreditRepairAIv2.0/pull/16) covers the three Muse/Viktor parser follow-up items. This checklist agrees with [the current assessment](docs/PRODUCTION_ASSESSMENT.md) and [the machine-readable evidence](docs/PARSER_ROBUSTNESS_EVIDENCE.json).

<!-- parser-followup: implemented-awaiting-ci -->

**Status: implementation complete for the three requested items; CI verification pending.** This is not a whole-application completion claim or production-launch approval. The old numerical production score is retired.

## Code follow-up acceptance

- [x] Add seeded text fuzz/property tests: 2,400 mutations plus deterministic, account-boundary, unicode, empty/single-account, and resource-limit properties.
- [x] Add actual Android mutated-report PDF tests (24), malformed/truncated PDF tests (48 mutations), three seeded real image-only OCR imports, and oversized-input rejection.
- [x] Expand synthetic reports: 18 checked-in text fixtures (six layouts per bureau), and 78 generated PDF cases (72 supported plus six safe-rejection cases).
- [x] Enforce extracted-text/field bounds, retain byte/page/account/OCR limits, and prevent resource-limit errors from falling through to OCR.
- [x] Update assessment and checklist; add an automated evidence-consistency check.
- [ ] Record a passing current parser CI run, including all JVM tests, 78 exact PDF outcomes, malformed-PDF fuzz and real OCR tests. Until then, these three items are not marked verified/closed.

Reproduction and exact corpus coverage: [PARSER_ROBUSTNESS.md](docs/PARSER_ROBUSTNESS.md). Timeouts and a bounded JVM heap detect regressions; a finite green run does not prove all possible PDFs are safe.

## Owner launch acceptance — remains open

These are separate from the three code follow-up items. Attach dated results, build/commit identity, and any remediation before checking a gate.

- [ ] Qualified legal review of terms, privacy, billing, disclosures and workflows.
- [ ] Independent penetration/security review; resolve material findings.
- [ ] Permissioned real-report validation across real bureau layouts; no consumer reports in the synthetic corpus or CI artifacts.
- [ ] Actual device/accessibility checks and signed release install/update validation.
- [ ] Staging Firebase email/Google sign-in → Convex → AI acceptance with a verified account and fictional case; verify Google sign-in on the relevant signing certificates.
- [ ] Play license-test purchase/restore/pending/cancellation/expiry/refund/account-switch scenarios and deletion/re-authentication acceptance; verify server access mode matches the app.
- [ ] Monitored support/deletion contact and public policy/deletion URLs accepted.
- [ ] Required real-person Play closed testing and owner submission/release decision.

## Existing configuration references

- Firebase project `credit-repair-ai-v2`; main package `com.creditrepairai.v2`; debug package `com.creditrepairai.v2.debug`.
- [Firebase repair record](docs/FIREBASE_REPAIR_2026-10-17.md), [integration acceptance record](docs/INTEGRATION_ACCEPTANCE_2026-10-17.md), and [launch pricing record](docs/LAUNCH_PRICING_AND_RELEASE_CANDIDATE_2026-10-17.md) retain their dated evidence. Do not infer current console settings from those records.
- Public production release requires the existing production configuration checks and release-ready gate. The separate `production-test` mode requires purchases disabled. This follow-up changes neither mode nor secrets, billing settings, signing keys, production deployments or Play state.
- Detailed older console/configuration instructions are retained in [the archived checklist](docs/archive/RELEASE_OWNER_CHECKLIST_BEFORE_2026-10-26.md) for reference. Its chronological status statements and checkboxes are historical and superseded by this checklist; confirm live settings before acting on old instructions.

Store credentials only in protected configuration. Parser test artifacts contain generated fictional data and test results; no consumer reports or account credentials are required.
