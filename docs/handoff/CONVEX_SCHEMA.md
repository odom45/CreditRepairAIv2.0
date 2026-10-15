# Convex schema and ownership map

Authoritative definition: `convex/schema.ts`. Run `node scripts/export-convex-schema.mjs` after any schema/library update. `convex-schema.json` is the actual exported validator/index definition, including inherited Convex Auth tables; it contains no database contents. Convex adds `_id` and `_creationTime` to stored records.

The native case—PDF text, parsed tradelines, findings, drafts, receipts and chat—lives in Android Keystore-encrypted local storage. The native agent receives a redacted snapshot for the request; the native app does **not** write its case into the legacy web `reports`/`disputes` tables. Firebase UID owns native server billing/consent state; a Convex Auth user ID owns the companion web records. Those identifiers are not interchangeable.

| Table | Fields | Indexes | Ownership and use |
| --- | --- | --- | --- |
| `playSubscriptions` | `owner, token, productId, state, expiresAt, active, checkedAt` | `by_owner(owner)`, `by_token(token)` | Firebase UID; server verifies with Play, binds tokens to one owner, and rejects expired/ineligible states. Never expose purchase tokens to buyers. |
| `billingAudit` | `actor, subscriptionId, action, at, result` | `by_actor(actor)`, `by_subscription(subscriptionId)` | Protected billing-admin operations; account deletion removes relevant rows. |
| `agentRateLimits` | `owner, windowStart, count` | `by_owner(owner)` | Firebase UID request limit. |
| `billingDeletionMarkers` | `ownerHash` | `by_owner_hash(ownerHash)` | Hashed Firebase UID tombstone prevents delayed work from restoring deleted rows. No current automatic expiry. |
| `policyAcceptances` | `ownerHash, version, digest, aiSharing, acceptedAt, choiceAt` | `by_owner_hash(ownerHash)` | Current affirmative document/AI choice; stale writes cannot undo revocation/deletion. |
| `users` | optional `name, email, phone, image, emailVerificationTime, phoneVerificationTime, isAnonymous, plan, discount, freeMonths` | `by_email(email)` | Companion Convex Auth user. This app overrides the library's `users` definition; use exported schema rather than assuming library-default indexes. |
| `reports` | `userId`, bureau union, account array, `parseDate` | `by_userId(userId)` | Companion web only. Account fields: `accountName, accountNumber, balance, status, bureau`. Its `analyzeReports` action currently returns sample comparisons; it is not the native parser or native analysis. |
| `disputes` | `userId, accountName, bureau, reason, status, createdAt` | `by_userId(userId)` | Companion web only; status union `draft/pending/sent/resolved`. |
| `authSessions`, `authAccounts`, `authRefreshTokens`, `authVerificationCodes`, `authVerifiers`, `authRateLimits` | See exported validators | See exported index definitions | Inherited from installed `@convex-dev/auth`; passwords/tokens/auth data must not be exported in a buyer preview. |

## Native HTTP contract

All POST endpoints below require a Firebase JWT issued by `https://securetoken.google.com/credit-repair-ai-v2`; `auth.config.ts` pins RS256, audience/project and Google's JWKS. If the buyer uses a different Firebase project, update that provider and the issuer checks in `creditAgentHttp.ts`, `billingHttp.ts` and `policyHttp.ts` together, then rerun authenticated tests.

| Route | Input/use | Additional gate |
| --- | --- | --- |
| `POST /policy/accept` | Current version/digest, `termsAccepted`, `aiSharing`, `choiceAt` | Valid current documents and plausible timestamp; owner hash comes from server identity. |
| `POST /credit-agent` | `clientRequestId`, question, redacted `caseSnapshot` | Verified email, current affirmative AI sharing, rate limit, active Play entitlement if paid mode enabled. |
| `POST /billing/verify` | Play `purchaseToken` | Current accepted terms and server Play verification. |
| `POST /billing/status` | Refresh/read entitlement | Server owner identity and Play state. |
| `POST /account/delete` | Remove native server records | Native client then requests Firebase login deletion with recent authentication; failures remain visible and retryable. |
| `GET /legal/*` | Public policy/deletion assets | Public; `support-config.js` reads monitored-contact configuration. |

Admin calls (`billingData.adminList`, `playBilling.adminManage`) require exact `BILLING_ADMIN_IDENTITIES`; an email address or UI route never grants admin rights. Hourly `crons.ts` re-verifies Play subscriptions. RTDN/PubSub is not configured. State-specific source text remains registry-only unless explicitly verified; the server enforces allowed jurisdiction/precedent scopes.

## Migration/deletion

No schema change is included in this hand-off. Validate additions on a fresh development deployment first; back up owned production data through the owner's authorized process before incompatible migrations. Reuse existing signing identity for updates and do not expose real consumer records, OAuth secrets, API keys or signing material in the buyer package. A source/schema export is not a database export.

One-time dispute access is stored separately in `playCasePurchases` with owner, token, hashed dispute binding, verified state, activity and check time. It has `by_owner` and `by_token` indexes. Account deletion removes these receipts and retains only the existing minimal deletion marker. No report content is added to this table.
