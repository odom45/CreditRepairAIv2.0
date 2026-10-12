import { httpRouter } from "convex/server";
import { auth } from "./auth";
import { creditAgentHttp } from "./creditAgentHttp";
import { billingHttp } from "./billingHttp";
import { policyHttp } from "./policyHttp";
import { publicPolicies } from "./publicPolicies";

const http = httpRouter();
auth.addHttpRoutes(http);
http.route({ path: "/policy/accept", method: "POST", handler: policyHttp });
for (const asset of ["privacy.html", "terms.html", "disclaimer.html", "delete-account.html", "contact.js", "deletion-contact.js", "support-config.js"]) http.route({ path: `/legal/${asset}`, method: "GET", handler: publicPolicies });
http.route({
  path: "/credit-agent",
  method: "POST",
  handler: creditAgentHttp,
});
for (const path of ["/billing/verify", "/billing/status", "/account/delete"]) http.route({ path, method: "POST", handler: billingHttp });

export default http;
