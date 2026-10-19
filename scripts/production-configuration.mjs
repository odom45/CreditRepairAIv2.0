import { spawnSync } from 'node:child_process';
import { writeFileSync } from 'node:fs';
import { pathToFileURL } from 'node:url';

export function validateReleaseMode(env) {
  const purpose = env.CREDIT_RELEASE_PURPOSE || 'production';
  if (!['production', 'production-test'].includes(purpose)) throw Error('Select a recognized release purpose.');
  const paid = env.CREDIT_PAID_ACCESS_ENABLED;
  if (purpose === 'production' && paid !== 'true') throw Error('The public production release requires configured paid access.');
  if (purpose === 'production-test' && paid !== 'false') throw Error('The production test candidate must keep purchases disabled.');
  return { releasePurpose: purpose, purchasesEnabled: paid === 'true' };
}

// Never print keys, provider bodies or protected CLI output.
export function validateProductionTarget(env) {
  const deployment = env.CREDIT_PRODUCTION_DEPLOYMENT || '';
  if (!/^[a-z][a-z0-9-]*-[0-9]+$/.test(deployment)) throw Error('Explicit production deployment name required.');
  if (!(env.CONVEX_DEPLOY_KEY || '').startsWith(`prod:${deployment}|`)) throw Error('A matching production deployment key is required; development keys are rejected.');
  if (env.CREDIT_AI_GATEWAY_URL !== `https://${deployment}.convex.site`) throw Error('The native gateway must match the selected production deployment site origin.');
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(env.CREDIT_SUPPORT_EMAIL || '')) throw Error('An operator support contact is required.');
  if (!['true', 'false'].includes(env.CREDIT_PAID_ACCESS_ENABLED || '')) throw Error('Select the intended paid-access setting explicitly.');
  return { deployment, gatewayMatches: true, productionKeySelected: true };
}

export function validateProductionSettings(settings, env) {
  const required = ['OPENAI_API_KEY', 'CREDIT_LEGAL_VECTOR_STORE_ID', 'CREDIT_SUPPORT_EMAIL'];
  if (required.some(name => !settings[name]?.trim())) throw Error('Existing production agent, legal-store and support settings are required.');
  if (!/^vs_[a-zA-Z0-9]+$/.test(settings.CREDIT_LEGAL_VECTOR_STORE_ID)) throw Error('An existing production legal vector store is required.');
  if (settings.CREDIT_SUPPORT_EMAIL !== env.CREDIT_SUPPORT_EMAIL) throw Error('Native and production support contacts must match.');
  for (const flag of ['CREDIT_ENABLE_TEST_AUTH', 'CREDIT_ENABLE_OPERATOR_TOOLS']) {
    if (settings[flag] && settings[flag] !== 'false') throw Error('Test-auth and operator tools must be disabled in production.');
  }
  const serverPaid = settings.CREDIT_PAID_ACCESS_ENABLED || 'false';
  if (serverPaid !== env.CREDIT_PAID_ACCESS_ENABLED) throw Error('Native and production paid-access settings must match.');
  if (serverPaid === 'true' && (!settings.GOOGLE_PLAY_SERVICE_ACCOUNT_JSON?.trim() || !settings.BILLING_ADMIN_IDENTITIES?.trim())) {
    throw Error('Paid access requires existing Play verification and billing-admin configuration.');
  }
  return { requiredServerSettingsPresent: true, testToolsDisabled: true, paidAccessMatches: true };
}

export function checkProductionConfiguration(env = process.env, run = spawnSync) {
  const report = validateProductionTarget(env);
  const settings = {};
  for (const name of ['OPENAI_API_KEY', 'CREDIT_LEGAL_VECTOR_STORE_ID', 'CREDIT_SUPPORT_EMAIL', 'CREDIT_PAID_ACCESS_ENABLED', 'CREDIT_ENABLE_TEST_AUTH', 'CREDIT_ENABLE_OPERATOR_TOOLS', 'GOOGLE_PLAY_SERVICE_ACCOUNT_JSON', 'BILLING_ADMIN_IDENTITIES']) {
    const result = run('npx', ['convex', 'env', 'get', name], { env, encoding: 'utf8', timeout: 30000, maxBuffer: 200000, stdio: ['ignore', 'pipe', 'pipe'] });
    // A CLI/network/auth failure must not look like an absent optional flag.
    if (result.status !== 0) throw Error('Production settings could not be read; deployment was not attempted.');
    settings[name] = (result.stdout || '').trim();
  }
  return { ...report, ...validateProductionSettings(settings, env) };
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  let report = { passed: false, deployed: false, newBillableResourcesCreated: false };
  try {
    report = { ...report, ...checkProductionConfiguration(), passed: true };
    if (process.argv.includes('--deploy')) {
      const result = spawnSync('npx', ['convex', 'deploy', '--yes', '--typecheck', 'enable'], { encoding: 'utf8', timeout: 180000, maxBuffer: 4000000, stdio: ['ignore', 'pipe', 'pipe'] });
      report.deployed = result.status === 0;
      if (!report.deployed) throw Error('Production deployment failed; inspect protected operator logs.');
    }
  } catch (error) {
    report.passed = false;
    report.error = error.message;
    process.exitCode = 1;
  }
  writeFileSync('production-configuration.json', JSON.stringify(report, null, 2) + '\n');
  console.log(JSON.stringify(report, null, 2));
}
