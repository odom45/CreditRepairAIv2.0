import { readFile } from "node:fs/promises";

const manifest = JSON.parse(await readFile(new URL("../legal-library/manifest.json", import.meta.url), "utf8"));
const reviewed = new Date(`${manifest.reviewedOn}T00:00:00Z`);
const ageDays = Math.floor((Date.now() - reviewed.getTime()) / 86_400_000);
if (!Number.isFinite(ageDays) || ageDays < 0) throw new Error("legal-library reviewedOn is invalid or in the future");
if (ageDays > manifest.reviewIntervalDays) {
  throw new Error(`Legal library review is ${ageDays} days old; limit is ${manifest.reviewIntervalDays} days.`);
}

const ids = new Set();
for (const source of manifest.sources) {
  if (!source.id || ids.has(source.id)) throw new Error(`Duplicate or missing source id: ${source.id || "(missing)"}`);
  ids.add(source.id);
  const url = new URL(source.url);
  const allowed = ["uscode.house.gov", "www.ecfr.gov", "www.consumerfinance.gov", "www.ftc.gov"];
  if (url.protocol !== "https:" || !allowed.includes(url.hostname)) {
    throw new Error(`Source must use an approved official HTTPS domain: ${source.url}`);
  }
  if (process.argv.includes("--check-links")) {
    const response = await fetch(source.url, { headers: { "User-Agent": "CreditRepairAI-LegalReview/1.0" } });
    if (!response.ok) throw new Error(`Source unavailable (${response.status}): ${source.url}`);
  }
}

console.log(`Legal library ${manifest.libraryVersion}: ${manifest.sources.length} official sources; reviewed ${ageDays} day(s) ago.`);
