import { v } from "convex/values";
import { mutation, query } from "./_generated/server";
import { auth } from "./auth";

export const list = query({
  args: {},
  handler: async (ctx) => {
    const userId = await auth.getUserId(ctx);
    if (!userId) return [];
    return await ctx.db
      .query("disputes")
      .withIndex("by_userId", (q) => q.eq("userId", userId))
      .order("desc")
      .collect();
  },
});

export const create = mutation({
  args: {
    accountName: v.string(),
    bureau: v.string(),
    reason: v.string(),
  },
  handler: async (ctx, args) => {
    const userId = await auth.getUserId(ctx);
    if (!userId) throw new Error("Unauthorized");
    return await ctx.db.insert("disputes", {
      userId,
      accountName: args.accountName,
      bureau: args.bureau,
      reason: args.reason,
      status: "draft",
      createdAt: new Date().toISOString(),
    });
  },
});

export const updateStatus = mutation({
  args: {
    id: v.id("disputes"),
    status: v.union(
      v.literal("draft"),
      v.literal("pending"),
      v.literal("sent"),
      v.literal("resolved")
    ),
  },
  handler: async (ctx, args) => {
    const userId = await auth.getUserId(ctx);
    if (!userId) throw new Error("Unauthorized");
    const dispute = await ctx.db.get(args.id);
    if (!dispute || dispute.userId !== userId) throw new Error("Not found");
    await ctx.db.patch(args.id, { status: args.status });
  },
});

export const remove = mutation({
  args: { id: v.id("disputes") },
  handler: async (ctx, args) => {
    const userId = await auth.getUserId(ctx);
    if (!userId) throw new Error("Unauthorized");
    const dispute = await ctx.db.get(args.id);
    if (!dispute || dispute.userId !== userId) throw new Error("Not found");
    await ctx.db.delete(args.id);
  },
});
