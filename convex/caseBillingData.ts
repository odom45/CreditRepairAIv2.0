import { internalMutation, internalQuery } from "./_generated/server";
import { v } from "convex/values";

export const byOwner = internalQuery({ args: { owner: v.string() }, handler: (ctx, args) =>
  ctx.db.query("playCasePurchases").withIndex("by_owner", q => q.eq("owner", args.owner)).collect() });
export const batch = internalQuery({ args: { cursor: v.union(v.string(), v.null()) }, handler: (ctx, args) =>
  ctx.db.query("playCasePurchases").paginate({ cursor: args.cursor, numItems: 50 }) });
export const upsert = internalMutation({ args: {
  owner: v.string(), ownerHash: v.string(), token: v.string(), caseHash: v.string(), state: v.string(), active: v.boolean(),
}, handler: async (ctx, args) => {
  if (!/^[a-f0-9]{64}$/.test(args.caseHash)) throw new Error("Invalid case binding.");
  if (await ctx.db.query("billingDeletionMarkers").withIndex("by_owner_hash", q => q.eq("ownerHash", args.ownerHash)).unique()) throw new Error("Account deletion is pending or completed.");
  const existing = await ctx.db.query("playCasePurchases").withIndex("by_token", q => q.eq("token", args.token)).unique();
  if (existing && (existing.owner !== args.owner || existing.caseHash !== args.caseHash)) throw new Error("Purchase belongs to a different account or case.");
  // Refunded/retired receipts cannot be resurrected by a delayed successful verification.
  if (existing && ["CANCELLED", "REFUNDED", "RETIRED"].includes(existing.state) && args.state === "PURCHASED") return false;
  const { ownerHash: _hash, ...values } = args;
  if (existing) await ctx.db.patch(existing._id, { ...values, checkedAt: Date.now() });
  else await ctx.db.insert("playCasePurchases", { ...values, checkedAt: Date.now() });
  return true;
} });
