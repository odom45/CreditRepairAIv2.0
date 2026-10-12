import { httpAction } from "./_generated/server";
import { internal } from "./_generated/api";
import { POLICY_VERSION, POLICY_DIGEST } from "./policyDocuments";

export async function accountHash(subject: string): Promise<string> {
  const hash = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(subject));
  return Array.from(new Uint8Array(hash), b => b.toString(16).padStart(2, "0")).join("");
}
export const policyHttp = httpAction(async (ctx, request) => {
  const headers = { "Content-Type": "application/json", "Cache-Control": "no-store", "X-Content-Type-Options": "nosniff" };
  const response = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status, headers });
  try {
    const identity = await ctx.auth.getUserIdentity();
    if (!identity || identity.issuer !== "https://securetoken.google.com/credit-repair-ai-v2") return response({ error: "Firebase sign-in required." }, 401);
    if (!request.headers.get("content-type")?.toLowerCase().includes("application/json")) return response({}, 415);
    const raw = await request.text();
    if (raw.length > 1000) return response({}, 413);
    const body = JSON.parse(raw);
    if (body.termsAccepted !== true || typeof body.aiSharing !== "boolean" || !Number.isSafeInteger(body.choiceAt) || body.choiceAt <= 0 || body.choiceAt > Date.now() + 300000 || body.version !== POLICY_VERSION || body.digest !== POLICY_DIGEST) return response({ error: "Read and accept the current terms; check automatic device date." }, 409);
    const accepted = await ctx.runMutation(internal.policyAcceptance.accept, { ownerHash: await accountHash(identity.subject),
      version: POLICY_VERSION, digest: POLICY_DIGEST, termsAccepted: true, aiSharing: body.aiSharing, choiceAt: body.choiceAt });
    return response(accepted);
  } catch { return response({ error: "Agreement preference could not be saved." }, 503); }
});
