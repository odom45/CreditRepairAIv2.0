import test from 'node:test';
import assert from 'node:assert/strict';
import { validateProductionTarget, validateProductionSettings, checkProductionConfiguration } from '../scripts/production-configuration.mjs';

const env = {
  CREDIT_PRODUCTION_DEPLOYMENT: 'fictional-production-123',
  CONVEX_DEPLOY_KEY: 'prod:fictional-production-123|fixture',
  CREDIT_AI_GATEWAY_URL: 'https://fictional-production-123.convex.site',
  CREDIT_SUPPORT_EMAIL: 'support@example.com', CREDIT_PAID_ACCESS_ENABLED: 'false',
};
const settings = { OPENAI_API_KEY: 'fixture', CREDIT_LEGAL_VECTOR_STORE_ID: 'vs_fixture', CREDIT_SUPPORT_EMAIL: env.CREDIT_SUPPORT_EMAIL };

test('production rejects development and mismatched keys before running any CLI', () => {
  for (const key of ['dev:fictional-production-123|fixture', 'prod:other-123|fixture', '']) {
    let calls = 0;
    assert.throws(() => checkProductionConfiguration({ ...env, CONVEX_DEPLOY_KEY: key }, () => { calls++; }));
    assert.equal(calls, 0);
  }
});
test('production gateway cannot target a different deployment or endpoint', () => {
  for (const gateway of ['https://staging-123.convex.site', env.CREDIT_AI_GATEWAY_URL + '/credit-agent', 'http://fictional-production-123.convex.site']) {
    assert.throws(() => validateProductionTarget({ ...env, CREDIT_AI_GATEWAY_URL: gateway }));
  }
  assert.equal(validateProductionTarget(env).productionKeySelected, true);
});
test('production requires explicit support and paid-access selections', () => {
  for (const name of ['CREDIT_PRODUCTION_DEPLOYMENT', 'CREDIT_SUPPORT_EMAIL', 'CREDIT_PAID_ACCESS_ENABLED']) {
    assert.throws(() => validateProductionTarget({ ...env, [name]: '' }));
  }
});
test('production preserves existing settings and never provisions a vector store', () => {
  const calls = [];
  const result = checkProductionConfiguration(env, (_command, args) => {
    calls.push(args);
    return { status: 0, stdout: settings[args[3]] || '' };
  });
  assert.equal(result.requiredServerSettingsPresent, true);
  assert.equal(calls.length, 8);
  assert.ok(calls.every(args => args.slice(0, 3).join(' ') === 'convex env get'));
});
test('production read failures cannot masquerade as disabled flags', () => {
  assert.throws(() => checkProductionConfiguration(env, () => ({ status: 1, stderr: 'private provider body' })), /could not be read/);
});
test('production requires agent settings and consistent support contact', () => {
  for (const name of ['OPENAI_API_KEY', 'CREDIT_LEGAL_VECTOR_STORE_ID', 'CREDIT_SUPPORT_EMAIL']) {
    assert.throws(() => validateProductionSettings({ ...settings, [name]: '' }, env));
  }
  assert.throws(() => validateProductionSettings({ ...settings, CREDIT_SUPPORT_EMAIL: 'other@example.com' }, env));
});
test('production blocks test-auth, operator tooling and inconsistent paid modes', () => {
  for (const name of ['CREDIT_ENABLE_TEST_AUTH', 'CREDIT_ENABLE_OPERATOR_TOOLS', 'CREDIT_PAID_ACCESS_ENABLED']) {
    assert.throws(() => validateProductionSettings({ ...settings, [name]: 'true' }, env));
  }
});
test('paid production requires existing Play and billing administration settings', () => {
  const paidEnv = { ...env, CREDIT_PAID_ACCESS_ENABLED: 'true' };
  const paidSettings = { ...settings, CREDIT_PAID_ACCESS_ENABLED: 'true' };
  assert.throws(() => validateProductionSettings(paidSettings, paidEnv));
  assert.equal(validateProductionSettings({ ...paidSettings, GOOGLE_PLAY_SERVICE_ACCOUNT_JSON: '{}', BILLING_ADMIN_IDENTITIES: 'fixture-subject' }, paidEnv).paidAccessMatches, true);
});
