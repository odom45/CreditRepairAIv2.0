# CreditRepairAI Privacy policy

Version: 2026-10-12.2 · Operator: 3six0 Studios LLC

**Review draft — commercial release requires qualified legal review**

## Scope and operator

CreditRepairAI is operated by 3six0 Studios LLC for U.S. adults. This policy describes the native Android app and its supporting services. A companion product is covered only if it expressly links to this policy; another seller's checkout and privacy terms also apply. This review draft must be approved and the operator's monitored support contact supplied before commercial release.

## Information on your device

You select credit-report PDFs for on-device PDF parsing or OCR. The app stores extracted account details, findings, drafts, portal receipts, responses, goals, manual score history, chat, checklist progress and agreement choices in an encrypted case using Android Keystore AES-GCM. Screen capture is restricted and app backup is disabled. These controls reduce risk; they do not guarantee perfect security. Browser downloads and copies you export, paste or share are outside this storage and must be managed separately.

## App authentication and technical information

Firebase processes app sign-in information, account identifiers, email verification, authentication tokens and related technical/security information. Google Sign-In is optional. Authentication and infrastructure providers can receive IP addresses and device or connection metadata. Password handling is performed by the authentication provider; the app does not save passwords in its case. The app never reads, fills, stores or transmits bureau or government-site credentials. Those are entered by you in the official site's browser.

## Optional AI sharing

Before an AI request, a separate disclosure asks whether you allow sharing. With your affirmative choice, CreditRepairAI sends your question and a redacted structured case snapshot over HTTPS through Convex to OpenAI for educational explanations and planning. The snapshot can include bureau names, account/furnisher details with only a last-four suffix, balances, statuses, findings, citations and dispute history. Original report PDFs are not uploaded. Automated redaction can miss unusual identifiers; do not type names, full account numbers, SSNs, birth dates or contact details into AI questions. AI can be disabled while local tools remain available. Withdrawing this choice stops future app requests; it cannot recall information already sent to a provider.

## Providers, purchases and agreement records

Google Play processes subscriptions and payment details. Convex stores purchase tokens, account association, product, verification state/expiry and limited administrative audit records needed for access verification and authorized subscription management. The server also stores the current agreement version, document digest, AI-sharing choice and server timestamp against a hashed account identifier. Rate-limit records help prevent abuse. The native app has no advertising or analytics SDK and does not sell case information. We use providers to operate the service, process requested purchases and protect it; we may respond to legal obligations. Providers have their own published retention and processing terms.

## Retention and deletion

Local case data remains until you clear it, sign out, switch accounts or remove the app. Clearing a case destroys its prior encryption key; current agreement/onboarding choices may be saved separately in a newly encrypted empty case. Account deletion removes Convex subscription, acceptance, rate-limit and relevant audit records, then asks Firebase to delete the login; a recent sign-in may be required. If either step fails, the app reports incomplete deletion and permits retry. A minimal hashed deletion marker remains to block delayed server work from recreating deleted records; it has no reports, purchase tokens or contact details. It currently has no automatic expiry. Qualified review must determine and approve its retention and any necessary legal/security retention before launch.

## Vendor retention and your choices

The OpenAI request uses store:false, but this is not a promise of zero vendor logging or retention. Provider backups, security logs, legal retention and Google Play transaction records follow applicable provider policies and law. Account deletion does not automatically cancel Google Play renewal or delete files from a browser, recipients or external portals. Cancel renewal in Google Play separately; deletion is not conditional on buying anything. You can use Settings to clear a case, delete an account, change AI sharing, and review agreement choices. A public deletion-request page must offer a working support pathway without requiring the app to be reinstalled.

## Support, children and changes

Use the monitored support/privacy contact displayed in the released app or public deletion-request page for access, correction, deletion or privacy questions. Do not send reports, SSNs, passwords or full account numbers to support. We may need proportionate verification of account ownership and will explain any lawful retention. The app is not intended for anyone under 18. Material changes receive a new document version and require fresh affirmative acceptance; additional AI sharing requires its own disclosure and choice. Technical implementation and this policy do not establish legal compliance or a security certification.
