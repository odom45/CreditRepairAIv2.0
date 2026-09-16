# CreditRepairAI release owner checklist

These owner-controlled items are required before a production Play submission. They must not be committed to Git.

## Required for a live AI-enabled tester build

- An OpenAI Platform project with API billing enabled and a project-scoped API key stored as the GitHub Actions secret `OPENAI_API_KEY`. A ChatGPT login or subscription is not an API credential.
- A production Convex deployment key stored as `CONVEX_DEPLOY_KEY`.
- A native OIDC provider/client for Android and its production issuer/audience configuration. The current APK intentionally refuses to send case context without a short-lived authenticated user token.
- The deployed Convex HTTPS endpoint stored as `CREDIT_AI_GATEWAY_URL` for signed builds.

## Required for signed APK/AAB output

- An owner-controlled Play upload keystore. For a new Play listing, create it once, back it up securely, and never share it through chat or source control.
- GitHub Actions secrets: `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, and `ANDROID_KEY_PASSWORD`.
- Final confirmation that package ID `com.creditrepairai.v2`, app name, version code, and version name are permanent for the new listing.

## Required before production use with real reports

- Permissioned, fully redacted sample PDFs covering current Experian, Equifax, and TransUnion layouts for parser regression tests.
- Privacy policy, terms, Play Data Safety answers, account/data-deletion process, support contact, age/territory decision, and any subscription/pricing terms.
- Business/compliance review of federal CROA obligations and state credit-services/credit-repair registration, bond, contract, cancellation, fee, advertising, consumer-reporting, privacy, and record-retention requirements.
- Independent mobile/backend security assessment and remediation, vendor/data-processing review, incident-response process, and key-rotation procedures.
- Later phase: attorney-reviewed state interpretations, preemption/private-action analysis, and escalation rules. These are deliberately not represented as complete in the current agent.

## Play Console materials

- 512×512 app icon, feature graphic, phone/tablet screenshots, short and full descriptions, category/tags, contact details, privacy-policy URL, content rating, target audience, ads declaration, app-access instructions, and closed-testing tester list.
- A signed `.aab` from the **Signed Android release** workflow. The debug APK is only for direct installation by testers.
