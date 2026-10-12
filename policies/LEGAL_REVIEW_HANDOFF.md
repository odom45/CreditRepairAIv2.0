# CreditRepairAI policy and release review handoff

Draft version: 2026-10-12.2. Operator identified by owner context: 3six0 Studios LLC; reviewer/owner must confirm legal identity and public business/contact information.

The source of truth is `policies/documents.json`. Run `npm run policies:generate` after approved edits and `npm run policies:check` to prevent drift between Android, backend metadata, Markdown and public HTML. Any content change changes the document digest and invalidates earlier app acceptance; use a new version for material changes. Do not present these drafts as attorney-approved or proof of compliance.

## Requested protection, expressed accurately

The terms explain that suggestions and purchases are optional, the user independently decides whether to act, the user verifies submissions and supporting facts, and the creator's personal experience is not professional training. They provide a qualified availability/warranty disclaimer and proposed liability limitation with mandatory-law exceptions. They do not claim all liability can be shifted to users, do not waive statutory consumer rights, and do not excuse the operator's unlawful conduct. Companion products must conspicuously incorporate the actual accepted version at their own checkout/download; existing sales are not retroactively bound.

## Counsel decisions still required

- Determine CROA and applicable state credit-services coverage for the actual product, AI-guided drafts, advertising and monthly payment model. If applicable, implement required disclosures/contracts, fee timing and cancellation procedures before selling. A software/education label and disclaimer do not establish exemption.
- Approve or revise the proposed US$100/12-month-fee limitation, warranty disclaimer, consumer-rights exceptions, cancellation/refund wording, companion-product incorporation and eligibility language for intended jurisdictions.
- Approve privacy categories, provider/contractual processing and transfers, public support/deletion pathways, retention schedules, and the minimal hashed deletion marker's currently indefinite retention. The code does not invent a legally required retention duration.
- Review authentication/SDK processing, separate AI disclosure and revocation, agreement evidence, and accessibility of notice/acceptance. Native acceptance is encrypted and account scoped; the server records a document digest and server timestamp. A record is evidence of a client-reported affirmative action, not conclusive proof of enforceability.
- Review store listing, promotions and companion-product claims together with these documents. No future promotional assets were received or approved in this task.

## Current authoritative references checked

- CROA, 15 USC 1679f: https://usc-cdn.house.gov/view.xhtml?req=granuleid:USC-prelim-title15-section1679f&num=0&edition=prelim — nonwaivable protections. The primary House host was under maintenance when checked; a current official CDN/search record was available. Counsel should verify the enacted text and current applicability.
- FTC CROA overview: https://www.ftc.gov/legal-library/browse/statutes/credit-repair-organizations-act — disclosure, representation, payment and cancellation issues.
- Berman v. Freedom Financial Network, 30 F.4th 849 (9th Cir. 2022): https://cdn.ca9.uscourts.gov/datastore/opinions/2022/04/05/20-16900.pdf — notice and assent analysis under the laws addressed in that case; not a certification that this app's agreement is enforceable nationwide.
- Google Play User Data: https://support.google.com/googleplay/android-developer/answer/10144311 — in-app/public privacy notice and separate prominent affirmative data-sharing disclosure.
- Google Play deletion requirements: https://support.google.com/googleplay/android-developer/answer/13327111 — working external request pathway and clear retention information.

## Handoff package

`PRIVACY.md`, `TERMS.md`, `DISCLAIMER.md`, their public HTML under `public/legal`, the native acceptance/disclosure screens, and `RELEASE_OWNER_CHECKLIST.md`. Public page files are build artifacts, not proof of hosting or a working mailbox. Do not publish for commercial use before contact/hosting and qualified review are complete.
