import { execFileSync } from "node:child_process";
import { readFileSync } from "node:fs";
const paths = execFileSync("git", ["ls-files", "-co", "--exclude-standard", "-z"]).toString().split("\0").filter(Boolean);
const signatures = [
  /sk-(?:proj-|svcacct-)?[A-Za-z0-9_-]{30,}/,
  /(?:dev|prod):[a-z0-9-]+\|[A-Za-z0-9+/=]{25,}/,
  /-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----\s*\n[A-Za-z0-9+/=]{30,}/,
  /gh[pousr]_[A-Za-z0-9]{30,}/,
];
const findings = [];
if (process.argv.includes("--history")) {
  let scanned = 0;
  const objects = execFileSync("git", ["rev-list", "--objects", "--all"]).toString().trim().split("\n");
  for (const row of objects) {
    const [sha, ...parts] = row.split(" "); const path = parts.join(" ");
    if (!path || path.endsWith("google-services.json")) continue;
    if (/\.(jks|keystore|p12)$/.test(path)) { findings.push(path + ": historical signing material"); continue; }
    if (execFileSync("git", ["cat-file", "-t", sha]).toString().trim() !== "blob") continue;
    if (Number(execFileSync("git", ["cat-file", "-s", sha]).toString()) > 1000000) continue;
    scanned++;
    const content = execFileSync("git", ["cat-file", "blob", sha]).toString();
    if (signatures.some(pattern => pattern.test(content))) findings.push(path + ": historical credential signature (value withheld)");
  }
  console.log(`History audit: ${scanned} reachable blobs up to 1 MB scanned; Firebase client keys excluded. Other public branches not fetched are outside this scan.`);
}
let firebaseConfigs = 0;
for (const path of new Set(paths)) {
  if (/\.(jks|keystore|p12)$/.test(path)) { findings.push(path + ": signing material"); continue; }
  let text;
  try { text = readFileSync(path, "utf8"); } catch { continue; }
  if (path.endsWith("google-services.json")) { firebaseConfigs++; continue; }
  if (signatures.some(pattern => pattern.test(text))) findings.push(path + ": credential signature (value withheld)");
}
console.log(`Source audit: ${new Set(paths).size} files; ${firebaseConfigs} public Firebase client configurations require owner restrictions.`);
if (findings.length) { findings.forEach(item => console.error(item)); process.exitCode = 1; }
else console.log("No private-key/API-token/signing-file signatures found in the current source snapshot. This is a pattern scan, not a penetration test or exhaustive secret detector.");
