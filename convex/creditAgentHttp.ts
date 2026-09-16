import { httpAction } from "./_generated/server";
import { PublicAgentError, runCreditAgent, type CreditAgentRequest } from "./creditAgentCore";

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
    if (!identity) throw new PublicAgentError("Sign in is required.", 401);

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
