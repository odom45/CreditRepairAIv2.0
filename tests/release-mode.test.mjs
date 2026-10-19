import test from 'node:test';
import assert from 'node:assert/strict';
import { validateReleaseMode } from '../scripts/production-configuration.mjs';

test('production test candidate cannot enable purchases or become the default release mode', () => {
  assert.deepEqual(validateReleaseMode({ CREDIT_RELEASE_PURPOSE: 'production-test', CREDIT_PAID_ACCESS_ENABLED: 'false' }),
    { releasePurpose: 'production-test', purchasesEnabled: false });
  for (const paid of ['true', '', undefined]) {
    assert.throws(() => validateReleaseMode({ CREDIT_RELEASE_PURPOSE: 'production-test', CREDIT_PAID_ACCESS_ENABLED: paid }));
  }
  assert.throws(() => validateReleaseMode({ CREDIT_PAID_ACCESS_ENABLED: 'false' }));
});
test('public production release requires paid mode and rejects unrecognized purposes', () => {
  assert.deepEqual(validateReleaseMode({ CREDIT_PAID_ACCESS_ENABLED: 'true' }),
    { releasePurpose: 'production', purchasesEnabled: true });
  for (const purpose of ['debug', 'staging', 'test']) {
    assert.throws(() => validateReleaseMode({ CREDIT_RELEASE_PURPOSE: purpose, CREDIT_PAID_ACCESS_ENABLED: 'true' }));
  }
});
