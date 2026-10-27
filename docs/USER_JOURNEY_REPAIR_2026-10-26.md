# Account, dispute, letter and conversation repair

The owner tested signed 2.0.7 and reported no visible sign-in/verification or sign-out, no offered bureau walkthrough, and an assistant blocked on verification with weak interaction. Physical mailed dispute letters are a required addition. These findings supersede the previous 100% backlog estimate.

## Implemented in PR #18 — awaiting CI

- A restored unverified Firebase account now sees a verification screen before report/dispute tools. The active email, resend/check controls and confirmed account switch are visible. Returning to the app refreshes verification and the ID token. Import handlers also require verification. Existing encrypted reports are retained during verification; no key or Firebase configuration is replaced.
- Every saved dispute offers a walkthrough and print/mail action for each relevant known bureau. Creating or reopening the draft from Analysis opens its own guide. The guide retains separate bureau progress and does not silently default to an unrelated bureau.
- Paper letters contain one bureau recipient, local-only sender fields, editable facts/resolution/evidence and signature space. Export and print require an explicit review. Native PDF pagination preserves all lines and supports print page ranges. The user prints, signs, adds bureau-required forms/identity records/supporting copies and mails the letter personally. Generating/saving/printing does not record submission.
- Mailing, confirmed delivery and response receipts are encrypted on device. 30/45-day check-ins use confirmed delivery; no response date or deletion is promised. Postal CFPB readiness considers an actual response or confirmed delivery plus 45 days; the user must still check the actual complaint requirements.
- AI requests include bounded, redacted recent conversation only for the selected scope, under the existing 4,000-character server contract. Earlier turns are labeled untrusted context. Error replies and other-case chat are excluded. Mail status dates/bureau can inform case history; raw tracking numbers, sender names/addresses and exported letter text are excluded from the agent snapshot. Existing authenticated server and separate AI-sharing consent remain required.
- The server outcome guard analyzes Markdown emphasis and narrowly qualified negation without allowing actual promises. A blocked draft gets at most one revision attempt using the same retrieved context and original deadline; a second failing draft remains withheld. Mail metadata is separately allowlisted by bureau/date; sender and tracking fields are stripped.
- The assistant scrolls to new messages, offers follow-up questions and exposes a stop-waiting action. Stopping the UI does not claim to retract a request already received by the provider.

## Evidence required before delivering 2.0.8

The new secret-free native-experience workflow compiles the app/test APK, runs all native JVM tests, lint and the full emulator instrumentation suite. Added tests reproduce restored unverified sessions, account/sign-out discovery and confirmation, correct bureau guide entry, letter-export review requirements, multi-page PDF first/last-line integrity, print page ranges, encrypted postal/chat persistence and exclusion of private tracking from agent JSON. Conversation properties check case isolation, redaction, bounded history and preservation of the current question.

No actual owner Firebase account, consumer report, postal mailing, printer or live OpenAI response is used in these offline CI fixtures. Those results must not be presented as owner-phone acceptance. Existing protected credentials are reused by the established build process; this patch neither creates nor changes keys.

## Official mail sources checked October 26, 2026

- Experian: [official dispute help](https://www.experian.com/help/dispute-credit/), Experian / Dispute by Mail / P.O. Box 4500 / Allen, TX 75013.
- Equifax: [official dispute request form](https://assets.equifax.com/assets/personal/Dispute.pdf), Equifax Information Services LLC / P.O. Box 740256 / Atlanta, GA 30374.
- TransUnion: [official mail/phone dispute instructions](https://www.transunion.com/credit-disputes/dispute-your-credit/mail-or-phone), TransUnion Consumer Solutions / P.O. Box 2000 / Chester, PA 19016-2000.

The app links these source pages and asks the user to recheck current address, form and identity-document requirements. For example, Equifax's official form requires identification/address information beyond the letter; the app does not imply that a name/address-only letter satisfies every requirement. Required sensitive identifiers can be added by the user directly to the bureau form without sending them to AI.
