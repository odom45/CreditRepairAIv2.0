# CreditRepairAI legal knowledge library

This directory is the reviewable source set for the server-side credit agent. Federal primary law and agency guidance are the governing core. It also contains a 50-state-plus-DC official-source registry, strict state-coverage policy, and a scoped federal FCRA case-law layer sourced from official opinions. It is not bundled into the APK and it does not contain user data.

## Production use

1. A qualified reviewer checks every federal entry in `manifest.json`, every state entry in `state-source-registry.json`, and every official opinion in `federal-fcra-case-law.json`; records material changes in `CHANGELOG.md`; and advances the applicable `reviewedOn` date.
2. With `OPENAI_API_KEY` available only in the operator environment, run `npm run legal:create-vector-store` to upload the reviewed files to a new, versioned OpenAI vector store. Each file receives jurisdiction and coverage-status attributes.
3. Set `CREDIT_LEGAL_VECTOR_STORE_ID`, `OPENAI_API_KEY`, and optionally `CREDIT_AGENT_MODEL` only in the Convex server environment.
4. Run `npm run legal:check`. The scheduled workflow also fails when review is older than 45 days or an official link is unavailable.

Retrieval is supporting context, not an autonomous legal conclusion. The agent must cite retrieved official sources, distinguish report facts from possible errors, and never promise deletion or a score result. A state portal marked `SOURCE_REGISTRY_ONLY` is not law content: the agent must use verified federal sources and disclose the state coverage gap. State-specific claims are enabled only after exact official provisions are marked `SOURCE_TEXT_VERIFIED`.

Case-law retrieval is also hard-filtered by court scope. Supreme Court opinions are available nationally; circuit opinions are retrievable only when the user's selected state belongs to that circuit. The mixed case manifest is deployment metadata and is not uploaded for retrieval, preventing summaries from another circuit from bypassing that filter. Until attorney/citator review is completed, case treatment remains `ATTORNEY_REVIEW_PENDING` and may be used only to explain official published holdings for educational planning—not to predict litigation.

## Updating

Do not automatically overwrite legal summaries from changed webpages. A source change creates a human-review task because an apparently small wording change can alter scope, effective date, or required procedure. After review, update the summary, registry/manifest date, and changelog, then rebuild the vector store. The weekly freshness workflow validates review age, jurisdiction completeness, official domains, and link reachability.
