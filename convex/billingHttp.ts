import { httpAction } from "./_generated/server";
import { internal } from "./_generated/api";
import { accountHash } from "./policyHttp";

export const billingHttp = httpAction(async (ctx, request) => {
  const headers = { "Content-Type": "application/json", "Cache-Control": "no-store", "X-Content-Type-Options": "nosniff" };
  try {
    const identity = await ctx.auth.getUserIdentity();
    if (!identity || identity.issuer !== "https://securetoken.google.com/credit-repair-ai-v2") return new Response(JSON.stringify({ error: "Firebase sign-in required." }), { status: 401, headers });
    if (!request.headers.get("content-type")?.includes("application/json")) return new Response("{}", { status: 415, headers });
    const raw = await request.text();
    if (raw.length > 8000) return new Response("{}", { status: 413, headers });
    const path = new URL(request.url).pathname;
    let result;
    if (path === "/account/delete") {
      // Delete app records; the native client then asks Firebase to delete the login with recent authentication.
      await ctx.runAction(internal.playBilling.deleteOwner, { owner: identity.subject, actor: identity.tokenIdentifier });
      result = { success: true };
    } else if (path === "/billing/status") result = await ctx.runAction(internal.playBilling.status, { owner: identity.subject });
    else {
      const agreement = await ctx.runQuery(internal.policyAcceptance.current, { ownerHash: await accountHash(identity.subject) });
      if (!agreement.accepted) return new Response(JSON.stringify({ error: "Current terms must be accepted before purchase verification." }), { status: 409, headers });
      const body = JSON.parse(raw);
      if (typeof body.purchaseToken !== "string") return new Response("{}", { status: 400, headers });
      result = await ctx.runAction(internal.playBilling.verify, { owner: identity.subject, token: body.purchaseToken });
    }
    return new Response(JSON.stringify(result), { headers });
  } catch { return new Response(JSON.stringify({ error: "Secure billing/account action could not complete." }), { status: 503, headers }); }
});
