import { readFileSync, writeFileSync } from 'node:fs';

// Query public Maven coordinates only. No app data, credential or consumer record is sent.
const report = { checkedAt: new Date().toISOString(), ecosystem: 'Maven', complete: false, packages: [], findings: [] };
let failure;
try {
  report.packages = JSON.parse(readFileSync(process.argv[2], 'utf8'));
  if (!Array.isArray(report.packages) || !report.packages.length || report.packages.length > 1000) throw new Error('Invalid or empty resolved runtime inventory.');
  if (!report.packages.every(p => /^[A-Za-z0-9_.-]+:[A-Za-z0-9_.-]+$/.test(p.name) && typeof p.version === 'string' && p.version.length < 100)) throw new Error('Invalid Maven coordinates.');
  for (let start = 0; start < report.packages.length; start += 64) {
    let pending = report.packages.slice(start, start + 64).map(p => ({ package: { ecosystem: 'Maven', name: p.name }, version: p.version }));
    for (let page = 0; pending.length; page++) {
      if (page >= 20) throw new Error('OSV pagination exceeded the safe bound; scan is incomplete.');
      const response = await fetch('https://api.osv.dev/v1/querybatch', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ queries: pending }), signal: AbortSignal.timeout(30_000),
      });
      if (!response.ok) throw new Error(`OSV returned HTTP ${response.status}; scan is incomplete.`);
      const data = await response.json();
      if (!Array.isArray(data.results) || data.results.length !== pending.length) throw new Error('OSV response did not cover every query.');
      const next = [];
      data.results.forEach((result, index) => {
        if (!result || typeof result !== 'object' || (result.vulns !== undefined && !Array.isArray(result.vulns))) throw new Error('Invalid OSV query result.');
        for (const vuln of result.vulns || []) {
          if (typeof vuln.id !== 'string') throw new Error('Missing advisory identifier.');
          const query = pending[index];
          report.findings.push({ name: query.package.name, version: query.version, id: vuln.id });
        }
        if (result.next_page_token) next.push({ ...pending[index], page_token: result.next_page_token });
      });
      pending = next;
    }
  }
  report.findings = [...new Map(report.findings.map(f => [`${f.name}:${f.version}:${f.id}`, f])).values()];
  report.complete = true;
} catch (error) { failure = error instanceof Error ? error.message : 'Dependency scan did not complete.'; report.failure = failure; }
writeFileSync('android-dependency-audit.json', JSON.stringify(report, null, 2));
console.log(JSON.stringify({ complete: report.complete, packageCount: report.packages.length, findings: report.findings, failure: failure || null }, null, 2));
if (!report.complete || report.findings.length) process.exit(1);
