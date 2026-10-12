import test from 'node:test';
import assert from 'node:assert/strict';
import { buildSync } from 'esbuild';
import { mkdtempSync, writeFileSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { pathToFileURL } from 'node:url';

const source = buildSync({ entryPoints: ['convex/creditAgentCore.ts'], bundle: true, platform: 'node', format: 'esm', write: false }).outputFiles[0].text;
const scratch = mkdtempSync(join(tmpdir(), 'credit-agent-test-'));
const fixtureModule = join(scratch, 'core.mjs');
writeFileSync(fixtureModule, source);
let runCreditAgent;
try { ({ runCreditAgent } = await import(pathToFileURL(fixtureModule).href)); }
finally { rmSync(scratch, { recursive: true, force: true }); }
const request = { clientRequestId: '11111111-1111-4111-8111-111111111111', question: 'Explain a balance review prompt.', caseSnapshot: { jurisdictionCode: 'WA', reports: [], accounts: [], findings: [], disputes: [] } };
const summary = { status: 'completed', output: [{ type: 'function_call', name: 'get_case_summary', arguments: '{}', call_id: 'fictional-call' }] };
const answer = { type: 'message', content: [{ type: 'output_text', text: 'Compare the dated statement with the report. FCRA 15 USC 1681i provides a process for supported disputes; verify your records before choosing an action.' }] };

async function withResponses(responses, action) {
  const originalFetch = globalThis.fetch;
  const originalEnv = Object.fromEntries(['OPENAI_API_KEY', 'CREDIT_LEGAL_VECTOR_STORE_ID', 'CREDIT_AGENT_MODEL'].map(k => [k, process.env[k]]));
  process.env.OPENAI_API_KEY = 'fictional-uncredentialed-test';
  process.env.CREDIT_LEGAL_VECTOR_STORE_ID = 'vs_fictional';
  process.env.CREDIT_AGENT_MODEL = 'fictional-model';
  const bodies = [];
  globalThis.fetch = async (_url, init) => {
    assert.ok(init.signal instanceof AbortSignal);
    bodies.push(JSON.parse(init.body));
    const response = responses.shift();
    assert.ok(response, 'Unexpected extra API request');
    if (typeof response === 'function') return response(init);
    if (response instanceof Error) throw response;
    return new Response(JSON.stringify(response), { status: 200, headers: { 'Content-Type': 'application/json' } });
  };
  try { await action(bodies); }
  finally {
    globalThis.fetch = originalFetch;
    for (const [key, value] of Object.entries(originalEnv)) if (value === undefined) delete process.env[key]; else process.env[key] = value;
  }
}

test('agent accepts completed case tools and usable legal retrieval; requests result evidence', async () => {
  await withResponses([summary, { status: 'completed', output: [{ type: 'file_search_call', status: 'completed', results: [{ file_id: 'file_fictional', text: 'FCRA 15 USC 1681i official source excerpt.' }] }, answer] }], async bodies => {
    const result = await runCreditAgent(request);
    assert.ok(result.startsWith('Educational information only'));
    assert.equal(bodies[0].tool_choice.name, 'get_case_summary');
    assert.equal(bodies[1].tool_choice.type, 'file_search');
    assert.ok(bodies.every(body => body.store === false && body.include.includes('file_search_call.results')));
  });
});

test('agent rejects empty or unusable retrieval instead of returning ungrounded legal prose', async () => {
  for (const results of [[], [{ file_id: 'file_fictional', text: '  ' }], [{ text: 'No identifiable source' }]]) {
    await withResponses([summary, { status: 'completed', output: [{ type: 'file_search_call', status: 'completed', results }, answer] }], async () => {
      await assert.rejects(runCreditAgent(request), error => error.status === 503 && error.message.includes('usable sources'));
    });
  }
});

test('failed search cannot qualify even if it includes stale-looking results', async () => {
  await withResponses([summary, { status: 'completed', output: [{ type: 'file_search_call', status: 'failed', results: [{ file_id: 'file_fictional', text: 'Stale text' }] }, answer] }], async () => {
    await assert.rejects(runCreditAgent(request), error => error.status === 503);
  });
});

test('agent cannot skip the required case summary despite a search result and answer', async () => {
  const search = { status: 'completed', output: [{ type: 'file_search_call', status: 'completed', results: [{ file_id: 'file_fictional', text: 'FCRA source' }] }, answer] };
  await withResponses(Array(7).fill(search), async bodies => {
    await assert.rejects(runCreditAgent(request), error => error.status === 502 && error.message.includes('case-tool checks'));
    assert.ok(bodies.every(body => body.tool_choice.name === 'get_case_summary'));
  });
});

test('truncated Responses output is rejected rather than exposed as a complete answer', async () => {
  await withResponses([{ status: 'incomplete', output: [answer] }], async () => {
    await assert.rejects(runCreditAgent(request), error => error.status === 502 && error.message.includes('did not complete'));
  });
});

test('provider network failure becomes a safe public error without exposing the provider body', async () => {
  await withResponses([new Error('private-provider-body')], async () => {
    await assert.rejects(runCreditAgent(request), error => error.status === 502 && !error.message.includes('private-provider-body'));
  });
});

test('one overall deadline prevents a new provider call after the budget expires', async () => {
  const now = Date.now;
  let calls = 0;
  Date.now = () => calls++ === 0 ? 0 : 60_001;
  try {
    await withResponses([], async bodies => {
      await assert.rejects(runCreditAgent(request), error => error.status === 504);
      assert.equal(bodies.length, 0);
    });
  } finally { Date.now = now; }
});

test('deadline aborts an outstanding provider request and returns a safe error', async () => {
  const now = Date.now;
  let calls = 0;
  let signal;
  Date.now = () => calls++ === 0 ? 0 : 59_995;
  try {
    await withResponses([init => new Promise((_resolve, reject) => {
      signal = init.signal;
      signal.addEventListener('abort', () => reject(new Error('private-abort-details')), { once: true });
    })], async () => {
      await assert.rejects(runCreditAgent(request), error => error.status === 502 && !error.message.includes('private-abort-details'));
      assert.equal(signal.aborted, true);
    });
  } finally { Date.now = now; }
});
