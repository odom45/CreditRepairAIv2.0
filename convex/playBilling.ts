"use node";
import { createHash } from "node:crypto";
import { playRequest } from "./playApi";
import { action, internalAction } from "./_generated/server";
import { internal } from "./_generated/api";
import { v } from "convex/values";
import { evaluateSubscription, isBillingAdmin, MONTHLY_PRODUCT, SUBSCRIPTION_PRODUCTS, type PlaySubscription } from "./billingPolicy";

const google = (path: string, method = "GET", data?: unknown) => playRequest<PlaySubscription>(path, method, data);
export const verify = internalAction({ args: { owner: v.string(), token: v.string() }, handler: async (ctx, args) => {
  const ownership = { ...args, ownerHash: createHash("sha256").update(args.owner).digest("hex") };
  if (!args.token || args.token.length > 4096) throw new Error("Invalid purchase token.");
  let purchase: PlaySubscription;
  try { purchase = await google(`/purchases/subscriptionsv2/tokens/${encodeURIComponent(args.token)}`); }
  catch (error) {
    // Google permanently expires purchase-token lookups. One retired token must
    // not lock out a later valid subscription; other failures remain fail-closed.
    const status = (error as { response?: { status?: number } }).response?.status;
    if (status !== 410) throw new Error("Play verification is temporarily unavailable.");
    const existing = await ctx.runQuery(internal.billingData.byOwner, { owner: args.owner });
    if (!existing.some(row => row.token === args.token)) throw new Error("Unknown retired purchase.");
    const expired = { productId: MONTHLY_PRODUCT, state: "EXPIRED_UNAVAILABLE", active: false, expiresAt: 0 };
    await ctx.runMutation(internal.billingData.upsert, { ...ownership, ...expired });
    return expired;
  }
  if (purchase.externalAccountIdentifiers?.obfuscatedExternalAccountId !== createHash("sha256").update(args.owner).digest("hex")) throw new Error("Purchase account does not match.");
  const entitlement = evaluateSubscription(purchase, Date.now());
  if (!SUBSCRIPTION_PRODUCTS.includes(entitlement.productId)) throw new Error("Unexpected subscription product.");
  // Reserve ownership without access; acknowledge before persisting an active grant.
  const reserved = await ctx.runMutation(internal.billingData.upsert, { ...ownership, ...entitlement, active: false });
  if (!reserved) return { ...entitlement, active: false, expiresAt: 0, state: "REPLACED" };
  // Only purchased/active entitlements are acknowledged. Pending payments never grant access.
  if (entitlement.active && purchase.acknowledgementState === "ACKNOWLEDGEMENT_STATE_PENDING") {
    await google(`/purchases/subscriptions/${entitlement.productId}/tokens/${encodeURIComponent(args.token)}:acknowledge`, "POST", {});
  }
  const granted = await ctx.runMutation(internal.billingData.upsert, { ...ownership, ...entitlement, linkedToken: entitlement.active ? purchase.linkedPurchaseToken : undefined });
  if (!granted) return { ...entitlement, active: false, expiresAt: 0, state: "REPLACED" };
  return entitlement;
} });
export const deleteOwner = internalAction({ args: { owner: v.string(), actor: v.string() }, handler: async (ctx, args) => {
  await ctx.runMutation(internal.billingData.deleteOwner, { ...args, ownerHash: createHash("sha256").update(args.owner).digest("hex") });
  return { success: true };
} });
export const allowAgentRequest = internalAction({ args: { owner: v.string() }, handler: async (ctx, args): Promise<boolean> => {
  return ctx.runMutation(internal.billingData.allowAgentRequest, { ...args, ownerHash: createHash("sha256").update(args.owner).digest("hex") });
} });
export const status = internalAction({ args: { owner: v.string() }, handler: async (ctx, args): Promise<{ active: boolean; expiresAt: number; state: string; caseHashes: string[] }> => {
  const records = await ctx.runQuery(internal.billingData.byOwner, args);
  const verified = [];
  for (const record of records) {
    if (["REPLACED", "EXPIRED_UNAVAILABLE"].includes(record.state)) continue;
    verified.push(await ctx.runAction(internal.playBilling.verify, { owner: args.owner, token: record.token }));
  }
  const subscription = verified.sort((a, b) => Number(b.active) - Number(a.active) || b.expiresAt - a.expiresAt)[0] || { active: false, expiresAt: 0, state: "Not subscribed" };
  return { ...subscription, caseHashes: await ctx.runAction(internal.caseBilling.status, args) };
} });
export const refreshBatch = internalAction({ args: { cursor: v.union(v.string(), v.null()) }, handler: async (ctx, args) => {
  const page = await ctx.runQuery(internal.billingData.batch, args);
  for (const row of page.page) {
    if (["REPLACED", "EXPIRED_UNAVAILABLE"].includes(row.state)) continue;
    try { await ctx.runAction(internal.playBilling.verify, { owner: row.owner, token: row.token }); }
    catch { /* No optimistic access. Expiry still gates access; next cron/foreground retries. */ }
  }
  if (!page.isDone) await ctx.scheduler.runAfter(0, internal.playBilling.refreshBatch, { cursor: page.continueCursor });
} });
export const adminManage = action({ args: { id: v.id("playSubscriptions"), operation: v.union(v.literal("refresh"), v.literal("cancel"), v.literal("refundAndRevoke")), confirmed: v.boolean() }, handler: async (ctx, args) => {
  const identity = await ctx.auth.getUserIdentity();
  if (!identity || !isBillingAdmin(identity.tokenIdentifier)) throw new Error("Administrator access required.");
  if (!args.confirmed) throw new Error("Explicit administrator confirmation required.");
  const row = await ctx.runQuery(internal.billingData.byId, { id: args.id });
  if (!row) throw new Error("Subscription not found.");
  try {
    if (args.operation === "cancel") await google(`/purchases/subscriptionsv2/tokens/${encodeURIComponent(row.token)}:cancel`, "POST", { cancellationContext: { cancellationType: "USER_REQUESTED_STOP_RENEWALS" } });
    if (args.operation === "refundAndRevoke") await google(`/purchases/subscriptionsv2/tokens/${encodeURIComponent(row.token)}:revoke`, "POST", { revocationContext: { fullRefund: {} } });
    await ctx.runAction(internal.playBilling.verify, { owner: row.owner, token: row.token });
    await ctx.runMutation(internal.billingData.audit, { actor: identity.tokenIdentifier, subscriptionId: args.id, action: args.operation, result: "completed" });
    return { success: true };
  } catch {
    await ctx.runMutation(internal.billingData.audit, { actor: identity.tokenIdentifier, subscriptionId: args.id, action: args.operation, result: "failed-or-verification-pending" });
    throw new Error("Play management did not finish or verification is pending. Refresh status before retrying a financial action; inspect Play Console.");
  }
} });
