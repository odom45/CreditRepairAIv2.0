import { v } from "convex/values";
import { action, mutation, query } from "./_generated/server";
import { auth } from "./auth";

export const list = query({
  args: {},
  handler: async (ctx) => {
    const userId = await auth.getUserId(ctx);
    if (!userId) return [];
    return await ctx.db
      .query("reports")
      .withIndex("by_userId", (q) => q.eq("userId", userId))
      .order("desc")
      .collect();
  },
});

export const saveReport = mutation({
  args: {
    bureau: v.union(
      v.literal("transunion"),
      v.literal("experian"),
      v.literal("equifax"),
      v.literal("unknown")
    ),
    accounts: v.array(
      v.object({
        accountName: v.string(),
        accountNumber: v.string(),
        balance: v.number(),
        status: v.string(),
        bureau: v.string(),
      })
    ),
  },
  handler: async (ctx, args) => {
    const userId = await auth.getUserId(ctx);
    if (!userId) throw new Error("Unauthorized");
    return await ctx.db.insert("reports", {
      userId,
      bureau: args.bureau,
      accounts: args.accounts,
      parseDate: new Date().toISOString(),
    });
  },
});

// Placeholder for AI analysis action
export const analyzeReports = action({
  args: {
    reportIds: v.array(v.id("reports")),
  },
  handler: async (ctx) => {
    const userId = await auth.getUserId(ctx);
    if (!userId) throw new Error("Unauthorized");
    
    // In a real app, we would fetch the reports and compare them
    // For now, we'll return sample comparison data
    return [
      {
        name: "OREGON COMMUNITY CU",
        number: "XXXX1234",
        transunion: null,
        experian: { status: "Charge-off", balance: 1087 },
        equifax: { status: "Charge-off", balance: 1087 },
        type: "missing",
        aiReason: "Account missing from TransUnion"
      },
      {
        name: "BOEING EMPLOYEES CU",
        number: "XXXX5678",
        transunion: { status: "Charge-off", balance: 466 },
        experian: { status: "Charge Off", balance: 466 },
        equifax: { status: "Charge-off", balance: 466 },
        type: "wording",
        aiReason: "Status wording inconsistent"
      }
    ];
  },
});
