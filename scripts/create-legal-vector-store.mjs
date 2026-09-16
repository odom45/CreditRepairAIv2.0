import { appendFile, readFile } from "node:fs/promises";

const apiKey = process.env.OPENAI_API_KEY;
if (!apiKey) throw new Error("Set OPENAI_API_KEY in the server/operator environment. Never place it in the APK.");

const legalRoot = new URL("../legal-library/", import.meta.url);
const manifest = JSON.parse(await readFile(new URL("manifest.json", legalRoot), "utf8"));
const stateRegistry = JSON.parse(await readFile(new URL("state-source-registry.json", legalRoot), "utf8"));
const caseLaw = JSON.parse(await readFile(new URL("federal-fcra-case-law.json", legalRoot), "utf8"));
const federalAttributes = {
  jurisdiction: "US",
  authority_level: "federal_official",
  category: "consumer_credit_reporting",
  coverage_status: "SOURCE_TEXT_VERIFIED",
  library_version: manifest.libraryVersion,
  reviewed_on: manifest.reviewedOn,
  court_scope: "NON_CASE",
};

const sourceFiles = [
  {
    name: "federal-source-manifest.json",
    bytes: await readFile(new URL("manifest.json", legalRoot)),
    attributes: { ...federalAttributes, category: "source_manifest" },
  },
  {
    name: "case-law-scope.md",
    bytes: await readFile(new URL("case-law-scope.md", legalRoot)),
    attributes: {
      ...federalAttributes,
      authority_level: "case_law_use_policy",
      category: "case_law_guardrails",
      coverage_status: "ATTORNEY_REVIEW_PENDING",
      reviewed_on: caseLaw.reviewedOn,
    },
  },
  {
    name: "federal-credit-reporting-core.md",
    bytes: await readFile(new URL("federal-credit-reporting-core.md", legalRoot)),
    attributes: federalAttributes,
  },
  {
    name: "state-law-scope.md",
    bytes: await readFile(new URL("state-law-scope.md", legalRoot)),
    attributes: {
      ...federalAttributes,
      category: "state_coverage_policy",
      coverage_status: "SOURCE_REGISTRY_ONLY",
    },
  },
  ...stateRegistry.jurisdictions.map((state) => ({
    name: `state-${state.code.toLowerCase()}-source-registry.md`,
    bytes: Buffer.from([
      `# ${state.name} (${state.code}) official-source registry`,
      "",
      `Coverage status: ${state.coverageStatus}`,
      `Registry reviewed: ${stateRegistry.reviewedOn}`,
      `Official code portal: ${state.officialCodePortal}`,
      "",
      "This registry entry is not a verified statement of state law. Until exact provisions are",
      "reviewed and marked SOURCE_TEXT_VERIFIED, use current federal sources and disclose the",
      "state-law coverage gap. Do not infer legal duties or remedies from the portal alone.",
      "",
    ].join("\n")),
    attributes: {
      jurisdiction: state.code,
      authority_level: "state_official_portal",
      category: "state_source_registry",
      coverage_status: state.coverageStatus,
      library_version: manifest.libraryVersion,
      reviewed_on: stateRegistry.reviewedOn,
      court_scope: "NON_CASE",
    },
  })),
];

for (const decision of caseLaw.cases) {
  const caseAttributes = {
    jurisdiction: "US",
    authority_level: "federal_case_law",
    coverage_status: "OFFICIAL_TEXT_VERIFIED",
    library_version: manifest.libraryVersion,
    reviewed_on: caseLaw.reviewedOn,
    court_scope: decision.precedentialScope,
    case_id: decision.id,
    treatment_status: caseLaw.treatmentStatus,
  };
  sourceFiles.push({
    name: `${decision.id}-source-card.md`,
    bytes: Buffer.from([
      `# ${decision.caseName}`,
      "",
      `Official citation: ${decision.citation}`,
      `Court: ${decision.court}`,
      `Decided: ${decision.decidedOn}`,
      `Precedential scope: ${decision.precedentialScope}`,
      `Official opinion: ${decision.officialOpinionUrl}`,
      `Treatment status: ${caseLaw.treatmentStatus}`,
      "",
      `Reviewed holding synopsis: ${decision.holding}`,
      "",
      `Permitted educational use: ${decision.operationalUse}`,
      "",
      `Limits: ${decision.limits}`,
      "",
      "This source card is a reviewed index, not the opinion itself or a formal citator result.",
      "Use the separately indexed official opinion PDF for the primary text.",
      "",
    ].join("\n")),
    attributes: {
      ...caseAttributes,
      authority_level: "case_law_index",
      category: "case_law_source_card",
    },
  });
  const opinion = await downloadOfficialOpinion(decision);
  sourceFiles.push({
    name: `${decision.id}.pdf`,
    bytes: opinion,
    attributes: {
      ...caseAttributes,
      category: "official_opinion",
    },
  });
}

const vectorStore = await openAi("/vector_stores", {
  method: "POST",
  body: JSON.stringify({
    name: `CreditRepairAI legal library ${manifest.libraryVersion}`,
    attributes: {
      application: "CreditRepairAI",
      library_version: manifest.libraryVersion,
      reviewed_on: manifest.reviewedOn,
    },
  }),
  headers: { "Content-Type": "application/json" },
});

const uploadedFiles = [];
for (const source of sourceFiles) {
  const form = new FormData();
  form.set("purpose", "assistants");
  form.set("file", new Blob([source.bytes]), source.name);
  const uploaded = await openAi("/files", { method: "POST", body: form });
  uploadedFiles.push({ file_id: uploaded.id, attributes: source.attributes });
}

const batch = await openAi(`/vector_stores/${vectorStore.id}/file_batches`, {
  method: "POST",
  body: JSON.stringify({ files: uploadedFiles }),
  headers: { "Content-Type": "application/json" },
});
const completedBatch = await waitForBatch(vectorStore.id, batch.id);
if (completedBatch.status !== "completed" || completedBatch.file_counts?.failed > 0) {
  throw new Error("Legal vector-store indexing did not complete successfully.");
}

await smokeSearch(vectorStore.id, "federal credit report dispute reinvestigation", {
  type: "and",
  filters: [
    { type: "eq", key: "jurisdiction", value: "US" },
    { type: "eq", key: "court_scope", value: "NON_CASE" },
  ],
});
await smokeSearch(vectorStore.id, "Washington official state code portal coverage", {
  type: "and",
  filters: [
    { type: "in", key: "jurisdiction", value: ["US", "WA"] },
    { type: "eq", key: "court_scope", value: "NON_CASE" },
  ],
});
await smokeSearch(vectorStore.id, "reasonable furnisher investigation official appellate precedent", {
  type: "and",
  filters: [
    { type: "eq", key: "category", value: "official_opinion" },
    { type: "in", key: "court_scope", value: ["US_SUPREME_COURT", "ELEVENTH_CIRCUIT"] },
  ],
});

if (process.env.GITHUB_OUTPUT) {
  await appendFile(
    process.env.GITHUB_OUTPUT,
    `vector_store_id=${vectorStore.id}\nlibrary_version=${manifest.libraryVersion}\n`,
    { encoding: "utf8", mode: 0o600 },
  );
}
console.log(`READY CREDIT_LEGAL_VECTOR_STORE_ID=${vectorStore.id}`);

async function waitForBatch(vectorStoreId, batchId) {
  for (let attempt = 0; attempt < 60; attempt += 1) {
    const result = await openAi(`/vector_stores/${vectorStoreId}/file_batches/${batchId}`, { method: "GET" });
    if (["completed", "failed", "cancelled"].includes(result.status)) return result;
    await new Promise((resolve) => setTimeout(resolve, 2_000));
  }
  throw new Error("Timed out waiting for legal vector-store indexing.");
}

async function smokeSearch(vectorStoreId, query, filters) {
  const result = await openAi(`/vector_stores/${vectorStoreId}/search`, {
    method: "POST",
    body: JSON.stringify({ query, filters, max_num_results: 5 }),
    headers: { "Content-Type": "application/json" },
  });
  if (!Array.isArray(result.data) || result.data.length === 0) {
    throw new Error("Legal vector-store smoke test returned no results.");
  }
}

async function downloadOfficialOpinion(decision) {
  const url = new URL(decision.officialOpinionUrl);
  const allowedHosts = new Set([
    "tile.loc.gov",
    "www.supremecourt.gov",
    "www.govinfo.gov",
    "media.ca11.uscourts.gov",
  ]);
  if (url.protocol !== "https:" || !allowedHosts.has(url.hostname)) {
    throw new Error(`Unapproved official opinion source for ${decision.id}.`);
  }
  const response = await fetch(url, {
    redirect: "follow",
    headers: { "User-Agent": "CreditRepairAI-LegalDeploy/1.0" },
    signal: AbortSignal.timeout(30_000),
  });
  if (!response.ok) throw new Error(`Official opinion download failed for ${decision.id} (${response.status}).`);
  const finalUrl = new URL(response.url);
  if (finalUrl.protocol !== "https:" || !allowedHosts.has(finalUrl.hostname)) {
    throw new Error(`Official opinion redirected to an unapproved source for ${decision.id}.`);
  }
  const contentLength = Number(response.headers.get("content-length") || 0);
  if (contentLength > 20_000_000) throw new Error(`Official opinion is unexpectedly large: ${decision.id}.`);
  const bytes = new Uint8Array(await response.arrayBuffer());
  if (bytes.byteLength < 1_000 || bytes.byteLength > 20_000_000) {
    throw new Error(`Official opinion size is invalid: ${decision.id}.`);
  }
  if (new TextDecoder().decode(bytes.slice(0, 5)) !== "%PDF-") {
    throw new Error(`Official opinion response is not a PDF: ${decision.id}.`);
  }
  return bytes;
}

async function openAi(path, init) {
  const response = await fetch(`https://api.openai.com/v1${path}`, {
    ...init,
    headers: { Authorization: `Bearer ${apiKey}`, ...(init.headers || {}) },
  });
  if (!response.ok) {
    throw new Error(`OpenAI ${path} failed with HTTP ${response.status}.`);
  }
  return response.json();
}
