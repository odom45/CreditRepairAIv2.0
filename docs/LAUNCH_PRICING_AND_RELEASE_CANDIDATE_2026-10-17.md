# Introductory pricing and release candidate — October 17, 2026 (Pacific)

## Pricing decision

The owner authorized $24.99 introductory monthly access, a $9.99 or $7.99 single-dispute price at engineering discretion, a worthwhile introductory annual discount, and visible percentage/duration disclosures in the app. The selected one-case price is $9.99 to retain more contribution per transaction. The monthly duration follows the previously proposed first-three-month offer. Annual pricing is the implementation choice below, not a previously sold price.

| Access | Introductory amount | Ongoing amount | Stated comparison |
| --- | --- | --- | --- |
| Monthly | $24.99/month for first 3 months | $30/month until canceled | 16.7% off renewal price during introduction |
| Yearly | $199.99 upfront for first year | $249.99/year until canceled | 20% off annual renewal price during introduction |
| One saved dispute | $9.99 once | No renewal | No invented former price/discount percentage |

Annual first-year pricing is 44.4% below twelve payments at the **standard** $30 monthly rate ($360); it is 42.0% below the introductory monthly customer's first twelve payments ($344.97 = 3 × $24.99 + 9 × $30). Display explicitly identifies the standard monthly comparison. $199.99/year is approximately $16.67/month equivalent, but the customer pays the annual amount upfront. Never describe that equivalent as a monthly payment option.

Three single-case purchases cost $29.97, versus $24.99 for one introductory subscription month. Once the subscription renews at $30, four single cases ($39.96) exceed one standard subscription month. The price comparison assumes one subscription month and does not imply that a dispute resolves in that time. A saved dispute can span multiple bureaus and its CFPB guidance; it is not a per-letter or per-bureau charge. Local tracking remains free.

The $30 financial-projection baseline needs cohort adjustments for introductory monthly and annual purchases; these lower prices are not $30 of monthly revenue per payer. Actual AI, hosting, refunds, support costs and acquisition costs remain unmeasured.

## Implementation and required console setup

`config/launch-pricing.json` is the intended US configuration. Actual prices and eligibility in the app always come from Google Play, never from that planning file. The app:

- Selects offer ID `launch-intro` on base plan `monthly` or `yearly`, with exactly the supported finite introduction followed by an infinite recurring phase.
- Supports a single non-recurring annual introductory phase before yearly renewal as well as a finite annual cycle.
- Falls back to the regular plan for ineligible users without showing an introductory badge.
- Rejects unknown offers, ambiguous duplicates, malformed durations/currencies, free phases, non-discount phases and installment plans.
- Calculates percentage savings from Play micro-unit amounts, rounds down to one decimal and displays renewal amounts, duration and upfront annual billing beside purchase controls.
- Requeries before checkout and compares the complete selected phase data/token. A changed price, renewal amount, eligibility or duration requires refresh/review.

Create Google-evaluated new-customer acquisition offers for customers who have never subscribed to any subscription in the app. Set monthly introduction to three payments at $24.99 / P1M. Set annual introduction to one payment at $199.99 / P1Y. Keep regular base prices $30 / P1M and $249.99 / P1Y. No fake 'was' price or invented expiry date is displayed. The one-case purchase is the standard $9.99 buy option; there is no recurring commitment or unsupported percentage claim on that SKU.

### Actual saved state and access limitation

Before this change, the monthly $30 base plan was saved inactive, the yearly subscription product existed inactive without a configured base plan, and the single-case product form was unsaved. During this turn, the browser's automatic review rejected opening Play Console: its reported origin was `https://google.play`, inconsistent with the requested `https://play.google.com/console/`. No bypass was attempted. **No new offer, annual base plan or single-case price has been saved or activated in Play this turn.**

Candidate defaults were advanced to 2.0.7 / 20261020 to exceed the prior 2.0.6 / 20261019 upload. This is source preparation only, not a new APK/AAB. The prior GitHub push remains blocked pending explicit authorization for the named repository/branch; no push retry or alternative publication mechanism was attempted.

October 18 follow-up: the owner requested completion of production backend, Play sign-in and billing before release assembly. The signed-release job now waits for `CREDIT_RELEASE_READY=true` and validates paid production settings and genuine Play certificate coverage before building. It uses explicitly configured production credentials/gateway. The historical publication/access state above records the earlier turn; consult the current repository/console evidence for any later successful publication or configuration.

## Release candidate before owner testing

The intended owner test is the signed, optimized release build using production Firebase/Convex/AI and license-test billing through an internal Play track. Internal distribution does not require making the app public or charging a real payment. Freeze the accepted build/source and promote the tested artifact when ready; any intervening app changes require relevant checks again. Testing is part of proving release readiness and cannot be completed before the test itself.

Required gates still open:

1. Register the actual Play signing certificate in Firebase and download genuine updated configuration. Direct upload-signature readiness does not establish Play-delivered Google sign-in.
2. Create/select the authorized production deployment credential, deploy to `efficient-porpoise-478`, configure the production gateway and verify authenticated Firebase → Convex → AI with consent and a fictional case. Production settings alone do not deploy code. Test/operator providers remain disabled.
3. Supply least-privilege Play server verification credentials/app permission and the authenticated billing-admin allowlist. Save/activate the intended plans/offers for internal license testing. Match native/server paid mode; do not charge customers before acceptance and qualified review.
4. Pass eligible/ineligible intro, renewal, cancellation, pending, restore, account switch, expired/held state, same-case duplicate, different-case repeat purchase and refund/revoke license tests. Confirm each displayed price/phase matches the Play checkout.
5. Build/sign version 2.0.7 (or later if that code is already used) with the existing owner key; verify AAB/APK signatures, lint, unit/device checks, R8 and update installation. Confirm production gateway and billing settings in the packaged release.
6. On the same release candidate, perform native sign-in/AI, PDF parsing/redaction, guided filing, deletion/retry and accessibility/device checks. No live disputes or real charges are needed for these checks.
7. Complete qualified legal/privacy/commercial model review, current policy/deletion hosting and monitored contact verification, accurate Play Data Safety/app-access/store declarations, independent security review and the account's required real-person testing/review process before public release.

Security-sensitive credential/access changes still require action-time confirmation under browser policy. General permission to finish the app does not establish that those console actions or real phone acceptance have occurred.

## Verification in this turn

New Kotlin regressions cover intro selection/disclosures, regular fallback, unsupported/ambiguous offers, non-recurring annual introduction, currency-safe annual savings and changed renewal terms. Android Gradle testing was attempted but could not bootstrap because its Java download from `services.gradle.org` was unreachable in this environment. An isolated Kotlin compiler/JUnit check using Maven Central artifacts compiled `BillingPolicy.kt`, `OfferPricing.kt` and their tests: all nine tests passed (six new pricing tests plus three existing billing tests). This validates those pure Kotlin classes only; it does not compile the Android/Compose app or prove Play integration.

Available checks passed: all 50 backend/configuration tests, frontend/Convex type checking, all nine synchronized policy artifacts, source credential-pattern scan and `git diff --check`. The exact offer-adapter method extracted from `PlayBillingManager.kt` also compiled against the official Billing 8.3.0 API artifact, checking the API fields used by the change; this is not a full manager/app build. The genuine Firebase configuration readiness check correctly fails `--require-play-ready`: upload certificate coverage is present, Play distribution certificate coverage remains absent. No new native Gradle test pass, rendered Android UI, signed APK/AAB or production deployment is claimed for this change.
