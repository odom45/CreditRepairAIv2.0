import { readFile } from "node:fs/promises";

const legalRoot = new URL("../legal-library/", import.meta.url);
const manifest = JSON.parse(await readFile(new URL("manifest.json", legalRoot), "utf8"));
const stateRegistry = JSON.parse(await readFile(new URL("state-source-registry.json", legalRoot), "utf8"));
const caseLaw = JSON.parse(await readFile(new URL("federal-fcra-case-law.json", legalRoot), "utf8"));
const checkLinks = process.argv.includes("--check-links");
const allowedFederalDomains = new Set([
  "uscode.house.gov",
  "www.ecfr.gov",
  "www.consumerfinance.gov",
  "www.ftc.gov",
]);
const validCoverageStates = new Set([
  "SOURCE_REGISTRY_ONLY",
  "SOURCE_TEXT_VERIFIED",
  "REVIEW_DUE",
  "RETIRED",
]);
const allowedCaseDomains = new Set([
  "tile.loc.gov",
  "www.supremecourt.gov",
  "www.govinfo.gov",
  "media.ca11.uscourts.gov",
]);
const allowedPrecedentialScopes = new Set([
  "US_SUPREME_COURT",
  "SECOND_CIRCUIT",
  "NINTH_CIRCUIT",
  "ELEVENTH_CIRCUIT",
]);
const requiredJurisdictions = new Set([
  "AL", "AK", "AZ", "AR", "CA", "CO", "CT", "DE", "DC", "FL", "GA", "HI", "ID",
  "IL", "IN", "IA", "KS", "KY", "LA", "ME", "MD", "MA", "MI", "MN", "MS", "MO",
  "MT", "NE", "NV", "NH", "NJ", "NM", "NY", "NC", "ND", "OH", "OK", "OR", "PA",
  "RI", "SC", "SD", "TN", "TX", "UT", "VT", "VA", "WA", "WV", "WI", "WY",
]);

const federalAgeDays = requireCurrentReview("Federal legal library", manifest.reviewedOn, manifest.reviewIntervalDays);
const stateAgeDays = requireCurrentReview("State source registry", stateRegistry.reviewedOn, stateRegistry.reviewIntervalDays);
const caseAgeDays = requireCurrentReview("Federal case-law library", caseLaw.reviewedOn, caseLaw.reviewIntervalDays);

const ids = new Set();
for (const source of manifest.sources) {
  if (!source.id || ids.has(source.id)) throw new Error(`Duplicate or missing source id: ${source.id || "(missing)"}`);
  ids.add(source.id);
  const url = requireHttps(source.url);
  if (!allowedFederalDomains.has(url.hostname)) {
    throw new Error(`Federal source must use an approved official domain: ${source.url}`);
  }
}

if (!Array.isArray(stateRegistry.jurisdictions) || stateRegistry.jurisdictions.length !== requiredJurisdictions.size) {
  throw new Error(`State registry must contain all 50 states plus DC (${requiredJurisdictions.size} entries).`);
}
const stateCodes = new Set();
for (const jurisdiction of stateRegistry.jurisdictions) {
  if (!requiredJurisdictions.has(jurisdiction.code) || stateCodes.has(jurisdiction.code)) {
    throw new Error(`Invalid or duplicate jurisdiction code: ${jurisdiction.code || "(missing)"}`);
  }
  stateCodes.add(jurisdiction.code);
  if (!jurisdiction.name || !validCoverageStates.has(jurisdiction.coverageStatus)) {
    throw new Error(`Invalid state registry entry for ${jurisdiction.code}.`);
  }
  const url = requireHttps(jurisdiction.officialCodePortal);
  if (/justia|findlaw|lawserver|lexisnexis|westlaw/i.test(url.hostname)) {
    throw new Error(`Unofficial aggregator is not allowed for ${jurisdiction.code}: ${url.hostname}`);
  }
}
for (const code of requiredJurisdictions) {
  if (!stateCodes.has(code)) throw new Error(`Missing jurisdiction: ${code}`);
}

const caseIds = new Set();
for (const decision of caseLaw.cases || []) {
  if (!decision.id || caseIds.has(decision.id)) throw new Error(`Duplicate or missing case id: ${decision.id || "(missing)"}`);
  caseIds.add(decision.id);
  if (!decision.caseName || !decision.citation || !decision.decidedOn || !decision.holding || !decision.limits) {
    throw new Error(`Case-law entry is incomplete: ${decision.id}`);
  }
  if (!allowedPrecedentialScopes.has(decision.precedentialScope)) {
    throw new Error(`Unsupported precedential scope for ${decision.id}: ${decision.precedentialScope}`);
  }
  const url = requireHttps(decision.officialOpinionUrl);
  if (!allowedCaseDomains.has(url.hostname)) {
    throw new Error(`Case opinion must use an approved official domain: ${decision.officialOpinionUrl}`);
  }
}
if (caseLaw.treatmentStatus !== "ATTORNEY_REVIEW_PENDING" || caseLaw.usePolicy !== "EDUCATIONAL_PRECEDENT_WITH_SCOPE_CHECK") {
  throw new Error("Case-law guardrail status must remain explicit until attorney/citator review is completed.");
}

if (checkLinks) {
  const links = [
    ...manifest.sources.map((source) => ({ label: source.id, url: source.url })),
    ...stateRegistry.jurisdictions.map((state) => ({ label: state.code, url: state.officialCodePortal })),
    ...caseLaw.cases.map((decision) => ({ label: decision.id, url: decision.officialOpinionUrl })),
  ];
  for (let index = 0; index < links.length; index += 8) {
    await Promise.all(links.slice(index, index + 8).map(checkOfficialLink));
  }
}

console.log(
  `Legal library ${manifest.libraryVersion}: ${manifest.sources.length} federal sources (${federalAgeDays}d old); ` +
  `${stateCodes.size} state/DC official-source entries (${stateAgeDays}d old); ` +
  `${caseIds.size} official federal opinions (${caseAgeDays}d old; attorney treatment review pending).`,
);

function requireCurrentReview(label, reviewedOn, reviewIntervalDays) {
  const reviewed = new Date(`${reviewedOn}T00:00:00Z`);
  const ageDays = Math.floor((Date.now() - reviewed.getTime()) / 86_400_000);
  if (!Number.isFinite(ageDays) || ageDays < 0) throw new Error(`${label} reviewedOn is invalid or in the future.`);
  if (!Number.isInteger(reviewIntervalDays) || reviewIntervalDays < 1 || ageDays > reviewIntervalDays) {
    throw new Error(`${label} review is ${ageDays} days old; limit is ${reviewIntervalDays} days.`);
  }
  return ageDays;
}

function requireHttps(value) {
  const url = new URL(value);
  if (url.protocol !== "https:") throw new Error(`Official sources must use HTTPS: ${value}`);
  return url;
}

async function checkOfficialLink(link) {
  let lastError;
  for (let attempt = 1; attempt <= 3; attempt += 1) {
    try {
      const response = await fetch(link.url, {
        headers: { "User-Agent": "CreditRepairAI-LegalReview/1.0" },
        redirect: "follow",
        signal: AbortSignal.timeout(20_000),
      });
      // Some official portals intentionally block automated requests while remaining
      // valid public sources. Auth/rate-limit responses therefore prove reachability.
      if (response.ok || [401, 403, 429].includes(response.status)) return;
      lastError = new Error(`HTTP ${response.status}`);
    } catch (error) {
      lastError = error;
    }
    if (attempt < 3) await new Promise((resolve) => setTimeout(resolve, attempt * 1_000));
  }
  const cause = lastError instanceof Error
    ? `${lastError.cause?.code || lastError.message}`
    : "unknown network error";
  throw new Error(`Official source unavailable for ${link.label} after 3 attempts (${cause}): ${link.url}`);
}
