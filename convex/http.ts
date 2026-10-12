import { httpRouter } from "convex/server";
import { auth } from "./auth";
import { creditAgentHttp } from "./creditAgentHttp";
import { billingHttp } from "./billingHttp";
import { policyHttp } from "./policyHttp";

const http = httpRouter();
auth.addHttpRoutes(http);
http.route({ path: "/policy/accept", method: "POST", handler: policyHttp });
http.route({
  path: "/credit-agent",
  method: "POST",
  handler: creditAgentHttp,
});
for (const path of ["/billing/verify", "/billing/status", "/account/delete"]) http.route({ path, method: "POST", handler: billingHttp });

export default http;
