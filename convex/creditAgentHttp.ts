import { httpAction } from "./_generated/server";
import { PublicAgentError, runCreditAgent, type CreditAgentRequest } from "./creditAgentCore";
import { internal } from "./_generated/api";
import { accountHash } from "./policyHttp";
import { scopeCaseSnapshot } from "./caseScope";

const jsonHeaders = {
  "Content-Type": "application/json; charset=utf-8",
  "Cache-Control": "no-store",
  "X-Content-Type-Options": "nosniff",
  "Referrer-Policy": "no-referrer",
};

export const creditAgentHttp = httpAction(async (ctx, request) => {
  try {
    if (request.headers.get("content-type")?.toLowerCase().includes("application/json") !== true) {
      throw new PublicAgentError("Content-Type must be application/json.", 415);
    }
    const identity = await ctx.auth.getUserIdentity();
    if (!identity || identity.issuer !== "https://securetoken.google.com/credit-repair-ai-v2") throw new PublicAgentError("Firebase sign-in is required.", 401);
    if (identity.emailVerified !== true) throw new PublicAgentError("Verify your account email, then sign in again before asking AI.", 403);
    const agreement = await ctx.runQuery(internal.policyAcceptance.current, { ownerHash: await accountHash(identity.subject) });
    if (!agreement.accepted || !agreement.aiSharing) throw new PublicAgentError("Current terms and affirmative AI-sharing consent are required.", 403);
    if (!await ctx.runAction(internal.playBilling.allowAgentRequest, { owner: identity.subject })) throw new PublicAgentError("Account access is unavailable or the educational AI request limit was reached.", 429);
    const raw = await request.text();
    if (raw.length > 200_000) throw new PublicAgentError("Request is too large.", 413);
    let body: CreditAgentRequest;
    try { body = JSON.parse(raw); } catch { throw new PublicAgentError("Invalid JSON request.", 400); }
    if (!body || typeof body !== "object" || !body.caseSnapshot || typeof body.caseSnapshot !== "object") throw new PublicAgentError("A case snapshot is required.", 400);
    if (process.env.CREDIT_PAID_ACCESS_ENABLED === "true") {
      const entitlement = await ctx.runAction(internal.playBilling.status, { owner: identity.subject });
      if (!entitlement.active || entitlement.expiresAt <= Date.now()) {
        const caseId = body.caseScopeId;
        if (typeof caseId !== "string" || !/^[A-Za-z0-9_-]{1,128}$/.test(caseId)) throw new PublicAgentError("Choose a purchased dispute or a subscription for AI access.", 402);
        const caseHash = await accountHash(`${identity.subject}:${caseId}`);
        if (!entitlement.caseHashes.includes(caseHash)) throw new PublicAgentError("Verified access for this dispute is required.", 402);
        try { body = { ...body, caseSnapshot: scopeCaseSnapshot(body.caseSnapshot, caseId) }; }
        catch { throw new PublicAgentError("Select the purchased dispute before asking AI.", 400); }
      }
    }
    const answer = await runCreditAgent(body);
    return new Response(JSON.stringify({ answer }), { status: 200, headers: jsonHeaders });
  } catch (error) {
    const known = error instanceof PublicAgentError;
    return new Response(
      JSON.stringify({ error: known ? error.message : "The secure AI service could not complete the request." }),
      { status: known ? error.status : 500, headers: jsonHeaders },
    );
  }
});
