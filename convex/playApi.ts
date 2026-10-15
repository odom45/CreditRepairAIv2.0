"use node";
import { GoogleAuth } from "google-auth-library";

const base = "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/com.creditrepairai.v2";
export async function playRequest<T>(path: string, method = "GET", data?: unknown): Promise<T> {
  const credentials = process.env.GOOGLE_PLAY_SERVICE_ACCOUNT_JSON;
  if (!credentials) throw new Error("Play server verification is not configured.");
  const client = await new GoogleAuth({ credentials: JSON.parse(credentials), scopes: ["https://www.googleapis.com/auth/androidpublisher"] }).getClient();
  const response = await client.request<T>({ url: base + path, method: method as "GET" | "POST", data, timeout: 15000 });
  return response.data;
}
