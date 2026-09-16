# CreditRepairAI legal knowledge library

This directory is the reviewable source set for the server-side credit agent. It is intentionally limited to official U.S. federal primary law and agency guidance. It is not bundled into the APK and it does not contain user data.

## Production use

1. A qualified reviewer checks every entry in `manifest.json`, records any material change in `CHANGELOG.md`, and advances `reviewedOn`.
2. With `OPENAI_API_KEY` available only in the operator environment, run `npm run legal:create-vector-store` to upload the reviewed files to a new, versioned OpenAI vector store.
3. Set `CREDIT_LEGAL_VECTOR_STORE_ID`, `OPENAI_API_KEY`, and optionally `CREDIT_AGENT_MODEL` only in the Convex server environment.
4. Run `npm run legal:check`. The scheduled workflow also fails when review is older than 45 days or an official link is unavailable.

Retrieval is supporting context, not an autonomous legal conclusion. The agent must cite retrieved official sources, distinguish report facts from possible errors, and never promise deletion or a score result. State law is not yet included; state-specific questions must be escalated or answered only at the federal level.

## Updating

Do not automatically overwrite legal summaries from changed webpages. A source change creates a human-review task because an apparently small wording change can alter scope, effective date, or required procedure. After review, update the summary, manifest date, and changelog, then rebuild the vector store.
