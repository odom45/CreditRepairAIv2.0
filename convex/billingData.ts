import { internalMutation, internalQuery, query } from "./_generated/server";
import { v } from "convex/values";
import { isBillingAdmin } from "./billingPolicy";

export const adminIdentity = query({ args: {}, handler: async ctx => {
  const identity = await ctx.auth.getUserIdentity();
  return identity ? { identity: identity.tokenIdentifier, isAdmin: isBillingAdmin(identity.tokenIdentifier) } : null;
} });
export const adminList = query({ args: {}, handler: async ctx => {
  const identity = await ctx.auth.getUserIdentity();
  if (!identity || !isBillingAdmin(identity.tokenIdentifier)) return [];
  const subscriptions = await ctx.db.query("playSubscriptions").order("desc").take(100);
  return subscriptions.map(({ token: _token, ...safe }) => safe);
} });
export const byOwner = internalQuery({ args: { owner: v.string() }, handler: (ctx, args) => ctx.db.query("playSubscriptions").withIndex("by_owner", q => q.eq("owner", args.owner)).collect() });
export const byId = internalQuery({ args: { id: v.id("playSubscriptions") }, handler: (ctx, args) => ctx.db.get(args.id) });
export const batch = internalQuery({ args: { cursor: v.union(v.string(), v.null()) }, handler: (ctx, args) => ctx.db.query("playSubscriptions").paginate({ cursor: args.cursor, numItems: 50 }) });
export const upsert = internalMutation({ args: {
  owner: v.string(), ownerHash: v.string(), token: v.string(), productId: v.string(), state: v.string(), expiresAt: v.number(), active: v.boolean(), linkedToken: v.optional(v.string()),
}, handler: async (ctx, args) => {
  // This check and insertion are in one serializable mutation. A refresh that
  // started before deletion cannot recreate records after the deletion commits.
  const deleted = await ctx.db.query("billingDeletionMarkers").withIndex("by_owner_hash", q => q.eq("ownerHash", args.ownerHash)).unique();
  if (deleted) throw new Error("Account deletion is pending or completed.");
  const existing = await ctx.db.query("playSubscriptions").withIndex("by_token", q => q.eq("token", args.token)).unique();
  if (existing && existing.owner !== args.owner) throw new Error("Purchase belongs to a different account.");
  if (existing?.state === "REPLACED") return false;
  if (args.linkedToken) {
    const linked = await ctx.db.query("playSubscriptions").withIndex("by_token", q => q.eq("token", args.linkedToken!)).unique();
    if (linked) {
      if (linked.owner !== args.owner) throw new Error("Linked purchase belongs to a different account.");
      await ctx.db.patch(linked._id, { active: false, state: "REPLACED", expiresAt: 0 });
    }
  }
  const { linkedToken: _linked, ownerHash: _hash, ...values } = args;
  if (existing) await ctx.db.patch(existing._id, { ...values, checkedAt: Date.now() });
  else await ctx.db.insert("playSubscriptions", { ...values, checkedAt: Date.now() });
  return true;
} });
export const audit = internalMutation({ args: { actor: v.string(), subscriptionId: v.string(), action: v.string(), result: v.string() }, handler: (ctx, args) => ctx.db.insert("billingAudit", { ...args, at: Date.now() }) });
export const deleteOwner = internalMutation({ args: { owner: v.string(), ownerHash: v.string(), actor: v.string() }, handler: async (ctx, args) => {
  const marker = await ctx.db.query("billingDeletionMarkers").withIndex("by_owner_hash", q => q.eq("ownerHash", args.ownerHash)).unique();
  if (!marker) await ctx.db.insert("billingDeletionMarkers", { ownerHash: args.ownerHash });
  for (const item of await ctx.db.query("playSubscriptions").withIndex("by_owner", q => q.eq("owner", args.owner)).collect()) {
    for (const audit of await ctx.db.query("billingAudit").withIndex("by_subscription", q => q.eq("subscriptionId", item._id)).collect()) await ctx.db.delete(audit._id);
    await ctx.db.delete(item._id);
  }
  for (const item of await ctx.db.query("billingAudit").withIndex("by_actor", q => q.eq("actor", args.actor)).collect()) await ctx.db.delete(item._id);
  for (const item of await ctx.db.query("agentRateLimits").withIndex("by_owner", q => q.eq("owner", args.owner)).collect()) await ctx.db.delete(item._id);
} });
export const allowAgentRequest = internalMutation({ args: { owner: v.string(), ownerHash: v.string() }, handler: async (ctx, args) => {
  if (await ctx.db.query("billingDeletionMarkers").withIndex("by_owner_hash", q => q.eq("ownerHash", args.ownerHash)).unique()) return false;
  const now = Date.now();
  const row = await ctx.db.query("agentRateLimits").withIndex("by_owner", q => q.eq("owner", args.owner)).unique();
  const windowStart = row && now - row.windowStart < 3600000 ? row.windowStart : now;
  const count = windowStart === row?.windowStart ? row.count : 0;
  if (count >= 20) return false;
  if (row) await ctx.db.patch(row._id, { windowStart, count: count + 1 });
  else await ctx.db.insert("agentRateLimits", { owner: args.owner, windowStart, count: 1 });
  return true;
} });
