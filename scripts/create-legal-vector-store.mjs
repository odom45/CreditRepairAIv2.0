import { readFile } from "node:fs/promises";
import { basename } from "node:path";

const apiKey = process.env.OPENAI_API_KEY;
if (!apiKey) throw new Error("Set OPENAI_API_KEY in the server/operator environment. Never place it in the APK.");

const manifestPath = new URL("../legal-library/manifest.json", import.meta.url);
const manifest = JSON.parse(await readFile(manifestPath, "utf8"));
const files = [
  manifestPath,
  new URL("../legal-library/federal-credit-reporting-core.md", import.meta.url),
];

const vectorStore = await openAi("/vector_stores", {
  method: "POST",
  body: JSON.stringify({ name: `CreditRepairAI federal law ${manifest.libraryVersion}` }),
  headers: { "Content-Type": "application/json" },
});

for (const fileUrl of files) {
  const form = new FormData();
  form.set("purpose", "assistants");
  form.set("file", new Blob([await readFile(fileUrl)]), basename(fileUrl.pathname));
  const uploaded = await openAi("/files", { method: "POST", body: form });
  await openAi(`/vector_stores/${vectorStore.id}/files`, {
    method: "POST",
    body: JSON.stringify({ file_id: uploaded.id }),
    headers: { "Content-Type": "application/json" },
  });
}

console.log(`Created reviewed legal vector store. Set CREDIT_LEGAL_VECTOR_STORE_ID=${vectorStore.id} on the server.`);

async function openAi(path, init) {
  const response = await fetch(`https://api.openai.com/v1${path}`, {
    ...init,
    headers: { Authorization: `Bearer ${apiKey}`, ...(init.headers || {}) },
  });
  if (!response.ok) {
    const message = (await response.text()).slice(0, 500);
    throw new Error(`OpenAI ${path} failed (${response.status}): ${message}`);
  }
  return response.json();
}
