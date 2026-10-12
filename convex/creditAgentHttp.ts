import { httpAction } from "./_generated/server";
import { PublicAgentError, runCreditAgent, type CreditAgentRequest } from "./creditAgentCore";
import { internal } from "./_generated/api";
import { accountHash } from "./policyHttp";

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
    if (process.env.CREDIT_PAID_ACCESS_ENABLED === "true") {
      const entitlement = await ctx.runAction(internal.playBilling.status, { owner: identity.subject });
      if (!entitlement.active || entitlement.expiresAt <= Date.now()) throw new PublicAgentError("Verified software-access subscription required.", 402);
    }

    const raw = await request.text();
    if (raw.length > 200_000) throw new PublicAgentError("Request is too large.", 413);
    const body = JSON.parse(raw) as CreditAgentRequest;
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
