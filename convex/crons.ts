import { cronJobs } from "convex/server";
import { internal } from "./_generated/api";
const crons = cronJobs();
crons.interval("Reverify Play subscription lifecycle", { hours: 1 }, internal.playBilling.refreshBatch, { cursor: null });
crons.interval("refresh case purchase status", { hours: 1 }, internal.caseBilling.refreshBatch, { cursor: null });
export default crons;
