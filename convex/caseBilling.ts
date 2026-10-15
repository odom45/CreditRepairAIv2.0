"use node";
import { createHash } from "node:crypto";
import { internalAction } from "./_generated/server";
import { internal } from "./_generated/api";
import { v } from "convex/values";
import { CASE_PRODUCT, evaluateCasePurchase, type PlayCasePurchase } from "./billingPolicy";
import { playRequest } from "./playApi";

export const verify = internalAction({ args: { owner: v.string(), token: v.string() }, handler: async (ctx, args): Promise<{ caseHash: string; active: boolean; state: string }> => {
  if (!args.token || args.token.length > 4096) throw new Error("Invalid purchase token.");
  const ownerHash = createHash("sha256").update(args.owner).digest("hex");
  let purchase: PlayCasePurchase;
  try { purchase = await playRequest<PlayCasePurchase>(`/purchases/productsv2/tokens/${encodeURIComponent(args.token)}`); }
  catch (error) {
    const status = (error as { response?: { status?: number } }).response?.status;
    if (status !== 410) throw new Error("Play verification is temporarily unavailable.");
    const existing = (await ctx.runQuery(internal.caseBillingData.byOwner, { owner: args.owner })).find(row => row.token === args.token);
    if (!existing) throw new Error("Unknown retired purchase.");
    await ctx.runMutation(internal.caseBillingData.upsert, { ...args, ownerHash, caseHash: existing.caseHash, state: "RETIRED", active: false });
    return { caseHash: existing.caseHash, state: "RETIRED", active: false };
  }
  if (purchase.obfuscatedExternalAccountId !== ownerHash) throw new Error("Purchase account does not match.");
  const entitlement = evaluateCasePurchase(purchase);
  const values = { ...args, ownerHash, caseHash: entitlement.caseHash, state: entitlement.state, active: false };
  const reserved = await ctx.runMutation(internal.caseBillingData.upsert, values);
  if (!reserved || !entitlement.active) return { caseHash: entitlement.caseHash, state: entitlement.state, active: false };
  // Persist the case binding before consuming. Consumption acknowledges and enables
  // another purchase of this SKU for a different case; retries reuse the same binding.
  if (!entitlement.consumed) await playRequest<void>(`/purchases/products/${CASE_PRODUCT}/tokens/${encodeURIComponent(args.token)}:consume`, "POST");
  const granted = await ctx.runMutation(internal.caseBillingData.upsert, { ...values, active: true });
  return { caseHash: entitlement.caseHash, state: entitlement.state, active: granted };
} });
export const status = internalAction({ args: { owner: v.string() }, handler: async (ctx, args): Promise<string[]> => {
  const records = await ctx.runQuery(internal.caseBillingData.byOwner, args);
  const active = new Set<string>();
  for (const record of records) {
    if (["CANCELLED", "REFUNDED", "RETIRED"].includes(record.state)) continue;
    const verified = await ctx.runAction(internal.caseBilling.verify, { owner: args.owner, token: record.token });
    if (verified.active) active.add(verified.caseHash);
  }
  return [...active];
} });
export const refreshBatch = internalAction({ args: { cursor: v.union(v.string(), v.null()) }, handler: async (ctx, args) => {
  const page = await ctx.runQuery(internal.caseBillingData.batch, args);
  for (const row of page.page) {
    if (["CANCELLED", "REFUNDED", "RETIRED"].includes(row.state)) continue;
    try { await ctx.runAction(internal.caseBilling.verify, { owner: row.owner, token: row.token }); } catch { /* Foreground checks fail closed and retry. */ }
  }
  if (!page.isDone) await ctx.scheduler.runAfter(0, internal.caseBilling.refreshBatch, { cursor: page.continueCursor });
} });
