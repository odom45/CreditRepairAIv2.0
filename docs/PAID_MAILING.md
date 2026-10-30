# Optional paid physical mailing — 2.0.9

This version adds a user-controlled handoff to LetterStream's existing print-and-mail service. It does not implement CreditRepairAI merchant checkout, a markup, API order submission, document upload, or automatic postal status synchronization. The subscription purchase configuration is unchanged.

## Phone workflow

1. Open a saved dispute and the relevant bureau's **print / mail letter** action.
2. Enter sender details, verify facts, evidence and bureau requirements, and approve the review.
3. Save the current letter PDF successfully. Canceling or failing the save does not enable the provider handoff. Editing the letter requires a new save and review.
4. Read **Have my letter printed and mailed**, acknowledge the separate provider purchase and select **Continue to LetterStream — paid mailing**.
5. Sign in or create a LetterStream account. Complete any required signature and forms, upload the final packet and supporting documents, check recipient and return addresses and every page of the proof, select service, review the total and pay there.
6. Return to the app after actual mailing confirmation to record mailing and tracking. Payment, opening the browser, or saving a PDF is not evidence of mailing or delivery.

The provider supports First-Class Mail, Certified Mail, and optional electronic return receipts. Page count and service change the total; no fixed retail fee is promised in the app. The user uploads the final packet manually. No document or personal data is attached to the provider URL or sent to AI by this handoff. Downloaded copies and documents uploaded to the provider are outside encrypted app storage.

Provider instructions checked October 29, 2026:

- https://www.letterstream.com/creditrepairmail/
- https://help.letterstream.com/article/71-how-to-send-certified-mail-online

## Remaining work for a CreditRepairAI revenue add-on

Connect an approved provider API/monetization account and payment setup; establish retail prices after current provider costs; implement authenticated quoted orders, explicit upload consent, purchase verification, duplicate-order protection, status reconciliation, cancellation/refund handling and retention controls. The provider documents partnership, automation and monetization API modes at https://www.letterstream.com/api . No account, credential, charge or live mailing is created by this change.

## Verification scope

Device tests cover save/review/consent gates and invalidation after edits or review changes. Existing PDF export retains owner/dispute checks. Provider checkout and a real physical mailing require separate acceptance. CI results and signed artifact delivery must be recorded before calling this version verified or installed.
