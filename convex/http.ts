import { httpRouter } from "convex/server";
import { auth } from "./auth";
import { creditAgentHttp } from "./creditAgentHttp";
import { billingHttp } from "./billingHttp";

const http = httpRouter();
auth.addHttpRoutes(http);
http.route({
  path: "/credit-agent",
  method: "POST",
  handler: creditAgentHttp,
});
for (const path of ["/billing/verify", "/billing/status", "/account/delete"]) http.route({ path, method: "POST", handler: billingHttp });

export default http;
