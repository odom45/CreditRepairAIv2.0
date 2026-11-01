import { internalMutation, internalQuery } from "./_generated/server";
import { v } from "convex/values";
import { POLICY_VERSION, POLICY_DIGEST } from "./policyDocuments";

export const current = internalQuery({ args: { ownerHash: v.string() }, handler: async (ctx, args) => {
  const row = await ctx.db.query("policyAcceptances").withIndex("by_owner_hash", q => q.eq("ownerHash", args.ownerHash)).unique();
  const accepted = row?.version === POLICY_VERSION && row?.digest === POLICY_DIGEST;
  return { accepted, aiSharing: accepted && row?.aiSharing === true };
} });
export const accept = internalMutation({ args: {
  ownerHash: v.string(), version: v.string(), digest: v.string(), termsAccepted: v.boolean(), aiSharing: v.boolean(), choiceAt: v.number(),
}, handler: async (ctx, args) => {
  if (!args.termsAccepted || args.version !== POLICY_VERSION || args.digest !== POLICY_DIGEST) throw new Error("Current affirmative agreement required.");
  if (!Number.isSafeInteger(args.choiceAt) || args.choiceAt <= 0 || args.choiceAt > Date.now() + 300000) throw new Error("Check automatic device date and retry your choice.");
  if (await ctx.db.query("billingDeletionMarkers").withIndex("by_owner_hash", q => q.eq("ownerHash", args.ownerHash)).unique()) throw new Error("Account deletion is pending or completed.");
  const row = await ctx.db.query("policyAcceptances").withIndex("by_owner_hash", q => q.eq("ownerHash", args.ownerHash)).unique();
  // A request already in flight when sharing was disabled cannot re-enable it.
  if (row?.version === args.version && row?.digest === args.digest && args.choiceAt <= row.choiceAt) return { acceptedAt: row.acceptedAt, aiSharing: row.aiSharing };
  const values = { ownerHash: args.ownerHash, version: args.version, digest: args.digest, aiSharing: args.aiSharing, acceptedAt: Date.now(), choiceAt: args.choiceAt };
  if (row) await ctx.db.patch(row._id, values); else await ctx.db.insert("policyAcceptances", values);
  return { acceptedAt: values.acceptedAt, aiSharing: values.aiSharing };
} });
