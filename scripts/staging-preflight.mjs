import { spawnSync } from 'node:child_process';
import { writeFileSync, appendFileSync } from 'node:fs';

// Only booleans/statuses leave this script. Provider bodies and CLI outputs are private.
const names = ['OPENAI_API_KEY','CONVEX_DEPLOY_KEY','CREDIT_AI_GATEWAY_URL','ANDROID_KEYSTORE_BASE64','ANDROID_KEYSTORE_PASSWORD','ANDROID_KEY_ALIAS','ANDROID_KEY_PASSWORD'];
const report = { checkedAt: new Date().toISOString(), protectedInputs: Object.fromEntries(names.map(k => [k, Boolean(process.env[k])])), supportContactConfigured: /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(process.env.CREDIT_SUPPORT_EMAIL || ''), checks: {} };
async function status(url, init = {}) {
  try { const r = await fetch(url, { ...init, signal: AbortSignal.timeout(15000), redirect: 'error' }); await r.body?.cancel(); return r.status; }
  catch { return 'unreachable'; }
}
const gateway = (process.env.CREDIT_AI_GATEWAY_URL || 'https://lovable-dinosaur-379.convex.site').replace(/\/$/, '').replace(/\/credit-agent$/, '');
const expectedGateway = /^https:\/\/lovable-dinosaur-379\.convex\.site$/;
if (expectedGateway.test(gateway)) for (const route of ['/credit-agent','/policy/accept','/account/delete']) report.checks[route] = await status(gateway+route, { method: 'POST', headers: {'Content-Type':'application/json'}, body: '{}' });
else report.checks.gateway = 'unexpected-origin; not probed';
if(expectedGateway.test(gateway)) for(const route of ['/legal/privacy.html','/legal/terms.html','/legal/disclaimer.html','/legal/delete-account.html']) report.checks[route]=await status(gateway+route);
if (process.env.OPENAI_API_KEY) report.checks.openaiCredentialReadOnly = await status('https://api.openai.com/v1/models', {headers:{Authorization:`Bearer ${process.env.OPENAI_API_KEY}`}});
const key = process.env.CONVEX_DEPLOY_KEY || '';
report.checks.intendedDevelopmentKey = key.startsWith('dev:lovable-dinosaur-379|');
if (report.checks.intendedDevelopmentKey) {
  const result = spawnSync('npx', ['convex','env','list'], { encoding:'utf8', timeout:45000, maxBuffer:2_000_000, stdio:['ignore','pipe','pipe'] });
  report.checks.convexCredentialReadOnly = result.status === 0;
  if (result.status === 0) {
    const required = ['OPENAI_API_KEY','CREDIT_LEGAL_VECTOR_STORE_ID','GOOGLE_PLAY_SERVICE_ACCOUNT_JSON','BILLING_ADMIN_IDENTITIES'];
    report.checks.serverSettingsPresent = Object.fromEntries(required.map(k => [k, new RegExp(`(^|\\n)${k}=`).test(result.stdout)]));
  }
}
writeFileSync('staging-preflight.json', JSON.stringify(report, null, 2));
if (process.env.GITHUB_STEP_SUMMARY) appendFileSync(process.env.GITHUB_STEP_SUMMARY, `\n## Read-only staging preflight\n\n\`\`\`json\n${JSON.stringify(report,null,2)}\n\`\`\`\n\nNo consumer case, authenticated AI answer, purchase/refund, or Play upload was tested. Missing inputs remain owner/account dependencies.\n`);
console.log(JSON.stringify(report, null, 2));
