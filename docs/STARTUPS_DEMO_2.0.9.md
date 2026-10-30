# Startups.com presentation candidate — October 30, 2026, 7 a.m. Pacific

CreditRepairAI is an Android tool for reviewing imported three-bureau credit data, preparing factual disputes, and guiding users through bureau and CFPB workflows. It keeps original report parsing on the phone and redacts high-risk identifiers before AI requests.

## Changes in 2.0.9

- Fresh explicit sign-in is required on a new app session and after leaving the app. Cached Firebase identity alone no longer opens the case. Locking hides in-memory case data and retains the encrypted reports for the same account; explicit sign-out still removes local data. Android document-picker continuations and an explicit Google sign-in are handled separately so those flows can finish.
- The assistant offers report review, next-step planning, evidence checks, factual letter coaching, bureau-response interpretation and mailing guidance. Case conversations remain scoped, redacted and bounded. Replies can be copied or explained in simpler words. AI advice remains dependent on the real server connection and verified sign-in.
- The physical-letter screen adds a paid print-and-mail handoff to LetterStream after the current PDF is saved and reviewed. Users upload the finished packet, select mail service, review the quote and pay the provider. Self-printing stays included. This is external provider checkout; CreditRepairAI does not yet collect a mailing markup or submit automated mail orders.

## Previously discussed purchase options

These are the earlier pricing decisions, not proof that live Play purchases are enabled:

| Option | Planned price |
| --- | --- |
| Monthly introductory offer | $24.99/month for three months, then $30/month |
| Annual introductory offer | $199.99 first year, then $249.99/year |
| One saved dispute | $9.99, one-time |
| Optional physical print-and-mail | Separately quoted and charged by the mailing provider in this build |

## Demonstration sequence

1. Install the signed update over the current app; keep the same package/signing identity.
2. Open the app and show that it requires fresh login rather than immediately exposing reports. Sign in and confirm the existing encrypted case returns. Leave and reopen to check the lock.
3. Use fictional/demo reports for a public presentation. Open the assistant, choose a case, ask for evidence help, then ask a follow-up and show a simpler explanation.
4. Open that bureau's physical letter, review the fictional facts, save the PDF, acknowledge the separate provider fee and open LetterStream. Stop before uploading real personal documents or buying a mailing during the presentation.
5. Show the bureau walkthrough and saved case follow-up tracking.

## Evidence and limits

CI, signed artifact, actual authenticated phone behavior and real-provider AI checks must be recorded before claiming verified behavior. A generated answer, PDF export or checkout visit is not proof of bureau submission, physical mailing, deletion, a score increase or a legal outcome. Do not present live billing, a paid CreditRepairAI mail API, owner-phone acceptance, external review or store-release approval as complete without evidence.
