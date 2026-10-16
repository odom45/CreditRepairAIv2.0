# Engineering follow-up: 2.0.3

This follow-up continues the authorized native-app completion work. It does not change the prior account/external acceptance requirements or establish commercial launch readiness.

## Corrected source

- An account's fields are now searched through its complete section instead of only its first 40 lines. Payment-history rows no longer hide a later balance, limit or opened date.
- More than 150 detected account sections cause an explicit import error instead of returning only the first 150 accounts as a successful complete report. The existing case remains intact when parsing fails.
- Multiple accounts at the same creditor retain their separate findings. Finding deduplication now includes the actual affected account IDs.
- When the suffix is absent, comparisons retain the full opened-date/type fallback; a type alone is insufficient to match unrelated accounts. Missing identifiers remain a manual-verification limitation.
- Parser source changes now automatically trigger the measured synthetic PDF workflow.
- Production deployment uses a distinct protected `CREDIT_PRODUCTION_DEPLOY_KEY`, explicitly selected deployment/gateway, existing server settings and existing legal vector store. It rejects development keys, mismatched destinations, enabled test tools and inconsistent native/server paid modes. It no longer creates a new billable store or overwrites the model/API key on each deploy. Genuine Firebase registration readiness is required before the production workflow proceeds.
- Native release defaults and signed workflow advance to 2.0.3 / 20261016, retaining the existing owner signing identity.

## Configuration still required

Repository secret: `CREDIT_PRODUCTION_DEPLOY_KEY` for the actual owner's production deployment. Repository variables: `CREDIT_PRODUCTION_DEPLOYMENT`, `CREDIT_PRODUCTION_GATEWAY_URL`, `CREDIT_SUPPORT_EMAIL` and explicit `CREDIT_PAID_ACCESS_ENABLED`. These values must refer to the same deployment and settings the native production build uses. The existing tester build remains pointed at the authorized development gateway until genuine production configuration is available.

Run `node scripts/production-configuration.mjs` to check an explicitly selected protected production environment without deploying. Adding `--deploy` deploys checked backend source after configuration validation. It preserves server secrets and does not create OpenAI resources. These checks establish presence/consistency, not that credentials, model access, retrieved sources, mailbox operation or Play lifecycles work. Firebase/Play account connection and real native acceptance remain necessary.

## Verification

Source `737ec444ac826bb888a008349af6ed40ab85fc42` passed 53 JVM tests, 39 backend/configuration tests, 18 device tests, the strict 60-case synthetic parser batch, and signed-release install/update acceptance on API 36. Exact CI links, limitations and release hashes are recorded in [the verification follow-up](VERIFICATION_RESULTS.md#203-completion-follow-up--2026-10-16-utc). The development deployment succeeded but its live AI check still failed at exhausted API credit. No new production account, live authenticated sign-in, purchase, current consumer report or user-phone installation is claimed.
