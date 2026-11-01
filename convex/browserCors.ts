import { httpAction, type ActionCtx } from "./_generated/server";
export function allowedWebOrigin(
  origin: string,
  configured = process.env.CREDIT_WEB_ORIGINS || "",
): boolean {
  return configured
    .split(",")
    .map(s => s.trim())
    .filter(Boolean)
    .some(value => {
      try {
        const url = new URL(value);
        return (
          url.origin === value &&
          value === origin &&
          (url.protocol === "https:" ||
            (url.protocol === "http:" &&
              ["localhost", "127.0.0.1"].includes(url.hostname)))
        );
      } catch {
        return false;
      }
    });
}
export function withBrowserCors(
  handler: (ctx: ActionCtx, request: Request) => Promise<Response>,
) {
  return async (ctx: ActionCtx, request: Request) => {
    const origin = request.headers.get("Origin");
    if (origin && !allowedWebOrigin(origin))
      return new Response(
        JSON.stringify({ error: "Web origin is not authorized." }),
        {
          status: 403,
          headers: {
            "Cache-Control": "no-store",
            "Content-Type": "application/json",
          },
        },
      );
    const response =
      request.method === "OPTIONS"
        ? new Response(null, { status: 204 })
        : await handler(ctx, request);
    if (!origin) return response; // Existing Android/native calls have no Origin header.
    const headers = new Headers(response.headers);
    headers.set("Access-Control-Allow-Origin", origin);
    headers.set("Vary", "Origin");
    headers.set("Access-Control-Allow-Methods", "POST, OPTIONS");
    headers.set("Access-Control-Allow-Headers", "Authorization, Content-Type");
    headers.set("Access-Control-Max-Age", "600");
    headers.set("Cache-Control", "no-store");
    return new Response(response.body, { status: response.status, headers });
  };
}
export const browserPreflight = httpAction(
  withBrowserCors(async () => new Response(null, { status: 204 })),
);
