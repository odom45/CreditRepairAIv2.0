import { httpRouter } from "convex/server";
import { auth } from "./auth";
import { creditAgentHttp } from "./creditAgentHttp";

const http = httpRouter();
auth.addHttpRoutes(http);
http.route({
  path: "/credit-agent",
  method: "POST",
  handler: creditAgentHttp,
});

export default http;
