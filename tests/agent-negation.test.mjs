import test from 'node:test';
import assert from 'node:assert/strict';
import { educationalAnswer, redactText, sanitizeSnapshot } from '../convex/agentSafety.ts';

test('explicitly negated error claims retain their legal explanation', () => {
  for (const phrase of ['is not proof of an error', 'does not prove an error', 'doesn’t prove an error', 'never proves an error', 'is not automatically proof of an error']) {
    const text = `A balance difference ${phrase}. Compare the dated records. FCRA 15 USC 1681i describes the dispute process.`;
    assert.ok(educationalAnswer(text).includes('1681i'), phrase);
  }
});
test('a disclaimer cannot hide a later affirmative promise', () => {
  for (const text of [
    'This is not proof of an error, but it will be deleted.',
    'We cannot guarantee deletion. This service guarantees approval.',
    'It does not prove an error. It will boost your credit score.',
    'There is no proof of an error. It proves an error anyway.',
  ]) assert.ok(educationalAnswer(text).includes('did not pass'), text);
});
test('negation from another phrase cannot qualify a positive claim', () => {
  for (const text of ['Not only guaranteed deletion but a refund.', 'Not a warning: guaranteed approval.', 'Without knowing the facts, this proves an error.']) {
    assert.ok(educationalAnswer(text).includes('did not pass'), text);
  }
});
test('ordinary positive outcome claims remain blocked', () => {
  for (const text of ['Guaranteed deletion.', 'This guarantees approval.', 'Your account will be deleted.', 'This will increase your credit score.', 'This is proof of an error.']) {
    assert.ok(educationalAnswer(text).includes('did not pass'), text);
  }
});
test('answer redaction retains late citations while request text stays bounded', () => {
  const explanation = 'Compare the dated records and ask which fact needs review. '.repeat(95);
  const result = educationalAnswer(explanation + ' Sources: FCRA 15 USC 1681i. Contact fictional@example.invalid.');
  assert.ok(result.includes('1681i'));
  assert.ok(result.includes('[EMAIL REDACTED]'));
  assert.ok(!result.includes('fictional@example.invalid'));
  assert.equal(redactText(explanation).length, 4000);
  const oversized = educationalAnswer('A'.repeat(16001) + ' FCRA 1681i.');
  assert.ok(oversized.includes('no partial legal answer is displayed'));
  assert.ok(!oversized.includes('1681i'));
});

test('Markdown emphasis and qualified negation retain useful explanations while formatted promises fail', () => {
  for (const phrase of ['is **not** proof of an error', 'does **not** guarantee deletion', 'is not conclusive proof of an error', 'is not the same as proof of an error']) {
    const text = `A comparison flag ${phrase}. Check the dated records. FCRA 15 USC 1681i describes reinvestigation.`;
    assert.ok(educationalAnswer(text).includes('1681i'), phrase);
  }
  for (const claim of ['This **will** be deleted.', '**Guaranteed deletion**.', 'It is not conclusive proof of an error, but it will be deleted.']) {
    assert.ok(educationalAnswer(claim).includes('did not pass'), claim);
  }
});
test('mailing history keeps only bureau and dates; no sender or tracking data reaches tools', () => {
  const result = sanitizeSnapshot({ disputes: [{ id: 'fictional', mailingHistory: [
    { bureau: 'equifax', mailedAt: 1000, deliveredAt: 2000, responseReceivedAt: null, trackingNumber: 'PRIVATE-TRACKING', name: 'PRIVATE-SENDER', address: 'PRIVATE-ADDRESS' },
    { bureau: 'unknown', mailedAt: 1000 },
  ] }] });
  assert.deepEqual(result.disputes[0].mailingHistory, [{ bureau: 'equifax', mailedAt: 1000, deliveredAt: 2000, responseReceivedAt: null }]);
  assert.ok(!JSON.stringify(result).includes('PRIVATE-'));
});

test('privacy redaction preserves ordinary account terminology and masks actual identifiers', () => {
  const readable = 'Compare account records, account status, masked account identifier and account digits. Use account number ending in 1234.';
  assert.equal(redactText(readable), readable);
  for (const value of ['account AB12CD3456', 'acctAB123456', 'acct#abcdefgh', 'account number qwertyui', 'acct no. AB123456']) {
    assert.match(redactText(value), /••••/);
    assert.notEqual(redactText(value), value);
  }
});

test('generated blank account labels do not consume the next prose line while request identifiers remain masked', () => {
  const prose = 'Use a masked account number\n- Reported current balance\n- Statement date';
  assert.ok(educationalAnswer(prose).includes(prose));
  const opaque = 'account number\nabcdefgh';
  assert.ok(!redactText(opaque).includes('abcdefgh'));
  assert.ok(!educationalAnswer('account number\nAB123456').includes('AB123456'));
});
