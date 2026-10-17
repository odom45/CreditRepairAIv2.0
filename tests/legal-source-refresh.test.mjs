import test from 'node:test';
import assert from 'node:assert/strict';
import { refreshGorman } from '../scripts/refresh-gorman-source.mjs';

const caseId = 'gorman-v-wolpoff-2009';
const categories = ['official_opinion', 'case_law_source_card'];
const old = categories.map((category, i) => ({ id: `file-old${i}`, status: 'completed', attributes: { case_id: caseId, category } }));
const sources = categories.map(category => ({ name: 'fictional.md', bytes: 'fictional source', attributes: { category } }));

test('source maintenance completes both replacements before detaching originals and never deletes global files', async () => {
  const calls = []; let uploaded = 0;
  const api = async (path, init) => {
    calls.push([path, init.method]);
    if (init.method === 'GET') return { data: [...old, { id: 'file-unrelated', attributes: { case_id: 'other' } }], has_more: false };
    if (path === '/files') return { id: `file-new${uploaded++}` };
    if (init.method === 'POST') return { id: init.body.file_id, status: 'completed' };
    return { deleted: true };
  };
  const report = await refreshGorman({ storeId: 'vs_fictional', api, sources });
  assert.equal(report.completedSources, 2);
  assert.equal(report.retiredAttachments, 2);
  assert.equal(report.globalFilesRetained, true);
  const firstDelete = calls.findIndex(([, method]) => method === 'DELETE');
  assert.equal(calls.slice(0, firstDelete).filter(([path, method]) => path.startsWith('/vector_stores/') && path.endsWith('/files') && method === 'POST').length, 2);
  assert.ok(calls.filter(([, method]) => method === 'DELETE').every(([path]) => path.startsWith('/vector_stores/vs_fictional/files/file-old')));
  assert.ok(!calls.some(([path, method]) => path === '/vector_stores' || (path.startsWith('/files/') && method === 'DELETE')));
});
test('failed indexing retains every original attachment', async () => {
  const calls = [];
  const api = async (path, init) => {
    calls.push([path, init.method]);
    if (init.method === 'GET') return { data: old, has_more: false };
    if (path === '/files') return { id: 'file-new' };
    return { id: 'file-new', status: 'failed' };
  };
  await assert.rejects(refreshGorman({ storeId: 'vs_fictional', api, sources }), /indexing is incomplete/);
  assert.ok(!calls.some(([, method]) => method === 'DELETE'));
});
test('completed corrected sources are reused on retry without another upload', async () => {
  const current = old.map(f => ({ ...f, attributes: { ...f.attributes, source_revision: 'amended-20091021-review-20261017' } }));
  const api = async (_path, init) => {
    assert.equal(init.method, 'GET');
    return { data: current, has_more: false };
  };
  const report = await refreshGorman({ storeId: 'vs_fictional', api, sources });
  assert.equal(report.completedSources, 2);
  assert.equal(report.retiredAttachments, 0);
});
