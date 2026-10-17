import { readFileSync, writeFileSync } from 'node:fs';
import { execFileSync } from 'node:child_process';
import { pathToFileURL } from 'node:url';

const CASE_ID = 'gorman-v-wolpoff-2009';
const REVISION = 'amended-20091021-review-20261017';
const CATEGORIES = ['official_opinion', 'case_law_source_card'];

/** Replace only this case's store attachments after both corrected sources are usable.
 * Uploaded originals are retained for rollback; no store or global file is deleted.
 */
export async function refreshGorman({ storeId, api, sources, wait = ms => new Promise(r => setTimeout(r, ms)) }) {
  if (!/^vs_[A-Za-z0-9_-]+$/.test(storeId)) throw new Error('Existing legal store configuration is required.');
  if (sources.length !== 2 || new Set(sources.map(s => s.attributes.category)).size !== 2 ||
      sources.some(s => !CATEGORIES.includes(s.attributes.category))) throw new Error('Both source categories are required.');
  const files = [];
  let after = '';
  for (let page = 0; page < 100; page++) {
    const result = await api(`/vector_stores/${storeId}/files?limit=100${after ? `&after=${encodeURIComponent(after)}` : ''}`, { method: 'GET' });
    if (!Array.isArray(result.data)) throw new Error('Legal store listing was incomplete.');
    files.push(...result.data);
    if (!result.has_more) break;
    if (!result.last_id || result.last_id === after || page === 99) throw new Error('Legal store pagination was incomplete.');
    after = result.last_id;
  }
  const prior = files.filter(f => f.attributes?.case_id === CASE_ID && CATEGORIES.includes(f.attributes?.category));
  if (CATEGORIES.some(category => !prior.some(f => f.attributes.category === category))) {
    throw new Error('The existing Gorman source pair could not be identified; nothing was replaced.');
  }
  const replacements = [];
  for (const source of sources) {
    const attributes = { ...source.attributes, case_id: CASE_ID, source_revision: REVISION };
    let attached = prior.find(f => f.attributes.category === attributes.category && f.attributes.source_revision === REVISION);
    if (!attached) {
      const form = new FormData();
      form.set('purpose', 'assistants');
      form.set('file', new Blob([source.bytes]), source.name);
      const file = await api('/files', { method: 'POST', body: form });
      if (!/^file-[A-Za-z0-9_-]+$/.test(file.id || '')) throw new Error('Source upload did not return a file.');
      attached = await api(`/vector_stores/${storeId}/files`, { method: 'POST', body: { file_id: file.id, attributes } });
    }
    for (let attempt = 0; attached.status === 'in_progress' && attempt < 90; attempt++) {
      await wait(1000);
      attached = await api(`/vector_stores/${storeId}/files/${attached.id}`, { method: 'GET' });
    }
    if (attached.status !== 'completed') throw new Error('Corrected source indexing is incomplete; existing attachments were retained.');
    replacements.push(attached.id);
  }
  // No old attachment is removed until BOTH replacements have completed indexing.
  const retired = prior.filter(f => !replacements.includes(f.id));
  for (const file of retired) {
    const result = await api(`/vector_stores/${storeId}/files/${file.id}`, { method: 'DELETE' });
    if (result.deleted !== true) throw new Error('Old source detachment was not confirmed; inspect the store before retrying.');
  }
  return { status: 'refreshed', completedSources: replacements.length, retiredAttachments: retired.length,
    storeReused: true, globalFilesRetained: true, sourceRevision: REVISION };
}

async function main() {
  if (!(process.env.CONVEX_DEPLOY_KEY || '').startsWith('dev:lovable-dinosaur-379|')) throw new Error('Only the existing authorized development deployment is allowed.');
  const setting = name => execFileSync('npx', ['convex', 'env', 'get', name], { encoding: 'utf8', timeout: 30000, stdio: ['ignore', 'pipe', 'pipe'] }).trim();
  const apiKey = setting('OPENAI_API_KEY');
  const storeId = setting('CREDIT_LEGAL_VECTOR_STORE_ID');
  if (!apiKey) throw new Error('Existing server AI configuration is missing.');
  const api = async (path, init) => {
    const json = init.body && !(init.body instanceof FormData);
    const response = await fetch(`https://api.openai.com/v1${path}`, {
      ...init, body: json ? JSON.stringify(init.body) : init.body,
      headers: { Authorization: `Bearer ${apiKey}`, ...(json ? { 'Content-Type': 'application/json' } : {}) },
      signal: AbortSignal.timeout(30000),
    });
    if (!response.ok) throw new Error(`Legal source maintenance returned HTTP ${response.status}.`);
    return response.json();
  };
  const registry = JSON.parse(readFileSync('legal-library/federal-fcra-case-law.json', 'utf8'));
  const manifest = JSON.parse(readFileSync('legal-library/manifest.json', 'utf8'));
  const decision = registry.cases.find(c => c.id === CASE_ID);
  const expectedUrl = 'https://www.govinfo.gov/content/pkg/USCOURTS-ca9-06-17226/pdf/USCOURTS-ca9-06-17226-1.pdf';
  if (decision?.officialOpinionUrl !== expectedUrl || registry.treatmentStatus !== 'ATTORNEY_REVIEW_PENDING') throw new Error('Reviewed amended opinion and treatment limits are required.');
  const response = await fetch(expectedUrl, { signal: AbortSignal.timeout(30000) });
  if (!response.ok || new URL(response.url).hostname !== 'www.govinfo.gov') throw new Error('Amended official opinion was unavailable.');
  const pdf = new Uint8Array(await response.arrayBuffer());
  if (pdf.length < 1000 || pdf.length > 20000000 || new TextDecoder().decode(pdf.slice(0, 5)) !== '%PDF-') throw new Error('Official opinion response was not a valid bounded PDF.');
  const card = `# ${decision.caseName}\n\nOfficial citation: ${decision.citation}\nCourt: ${decision.court}\nAmended: ${decision.decidedOn}\nPrecedential scope: ${decision.precedentialScope}\nOfficial opinion: ${decision.officialOpinionUrl}\nTreatment status: ${registry.treatmentStatus}\n\nReviewed holding synopsis: ${decision.holding}\n\nPermitted educational use: ${decision.operationalUse}\n\nLimits: ${decision.limits}\n\nThis card indexes the amended official opinion and is not a formal citator result.\n`;
  const attributes = { jurisdiction: 'US', authority_level: 'federal_case_law', coverage_status: 'OFFICIAL_TEXT_VERIFIED',
    library_version: manifest.libraryVersion, reviewed_on: registry.reviewedOn, court_scope: decision.precedentialScope,
    treatment_status: registry.treatmentStatus };
  const report = await refreshGorman({ storeId, api, sources: [
    { name: `${CASE_ID}-amended.pdf`, bytes: pdf, attributes: { ...attributes, category: 'official_opinion' } },
    { name: `${CASE_ID}-source-card.md`, bytes: card, attributes: { ...attributes, authority_level: 'case_law_index', category: 'case_law_source_card' } },
  ] });
  writeFileSync('legal-source-refresh.json', JSON.stringify(report, null, 2));
  console.log(JSON.stringify(report));
}
if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  main().catch(() => { console.error('Legal source refresh did not complete. Existing source files are retained; inspect the maintenance run.'); process.exitCode = 1; });
}
