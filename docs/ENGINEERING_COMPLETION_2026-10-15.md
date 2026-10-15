# CreditRepairAI remaining engineering work

This change completes the missing native draft-answer and pricing-option paths in source. Commercial launch acceptance still requires account configuration and live evidence.

## Implemented

- Guided filing can generate four AI draft fields from saved facts. The user previews and explicitly applies them to editable fields; review resets. Incomplete output is rejected and changed facts invalidate pending drafts. Unfilled drafting placeholders prevent review/submission recording. Nothing is submitted to an external portal.
- Monthly and yearly subscriptions cover all selected items. The app reads actual eligible prices from Play, rejects unsupported offers and rechecks the selected offer before checkout.
- A consumable one-time purchase grants AI assistance for one saved dispute. Google verifies the account and hashed dispute binding. The server persists that binding before consumption, rejects pending/refunded/canceled receipts, prevents reassignment, and rechecks access. Consumption permits another purchase for a different dispute. Local organization/tracking stays available for all items.
- Case-scoped chat and filing requests filter context on the device. The server independently scopes requests for case-only access. All-item planning still requires a subscription when paid mode is enabled.
- Deletion removes case purchase records and prevents delayed verification from recreating them. Refunded/retired case tokens cannot be resurrected by delayed responses.
- Policy drafts were updated together to describe these choices and the hashed case binding. They remain drafts for qualified review. Purchases remain disabled by default.
- Release defaults are version 2.0.2, code 20261015. Signing uses the existing owner key. The release workflow now tests installing 2.0.1 and updating to the new signed release on a disposable emulator (API 35 and API 36 release checks are recorded separately) before exporting artifacts.

## Play configuration required

Create/activate `creditrepairai_monthly` with a standard auto-renewing P1M plan, `creditrepairai_yearly` with a standard auto-renewing P1Y plan, and `creditrepairai_dispute` with one standard buy option. Do not enable multi-quantity, preorder or rental offers for the one-dispute SKU. Set owner-approved prices in Play; no annual or one-time price is invented in code. Give the existing server identity the required Android Publisher permissions. Keep paid access off until review and license-tester acceptance.

Exercise both subscription periods and one-time purchases with Play test accounts: pending, cancel checkout, success, restore, refund, repeat purchase for a different case, account switch, expiry/hold/grace, and deletion during verification. A one-time pass does not restore deleted local report/draft content. Account identity and the original saved dispute must remain available to use it.

## Remaining account or external evidence

- Firebase: register the existing owner upload SHA-1/SHA-256 for the genuine release and debug Android apps; obtain genuine refreshed configuration. Do not fabricate OAuth config. Verify email/Google login and native Firebase-to-Convex AI using fictional records.
- Production: select the actual production Convex deployment and authorized credentials, then verify the full signed-release workflow. The existing development deployment is not production.
- API credit: excluded from coding completion but required for a successful live AI request.
- Support: verify the approved support/deletion mailbox receives requests.
- User device: emulator installation/update cannot identify an old certificate mismatch or installer behavior on a specific phone. Keep the current app/data until the installed certificate/error is known; do not silently uninstall it.
- Independent security review, permissioned current bureau reports, lawyer review, actual closed-test participants and elapsed testing remain required. State-law provision coverage remains the later scope; official jurisdiction links are not a verified body of state-law advice.

## Verification

The changed application source passed 48 JVM tests, 31 backend regressions, 18 device scenarios and the strict 60-case synthetic parser batch. Signed release install/update checks passed on API 35 and API 36 emulators. See [the verification record](VERIFICATION_RESULTS.md#engineering-completion-follow-up--2026-10-15) for exact source commits, CI runs, limits and release digests. Live AI remains blocked by exhausted API credit; Firebase/Play/production acceptance remains unverified.
