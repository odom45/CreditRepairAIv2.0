import test from 'node:test';
import assert from 'node:assert/strict';
import { inspectFirebaseSigning } from '../scripts/firebase-signing-readiness.mjs';

const target = {
  projectId: 'fixture-project', ownerUploadCertificate: { sha1: 'AA:BB' },
  playAppSigningSha1: 'CC:DD',
  apps: [{ package: 'com.creditrepairai.v2', config: 'release' }, { package: 'com.creditrepairai.v2.debug', config: 'debug' }],
};
function config(kind, playPackage = null) {
  const app = target.apps.find(a => a.config === kind);
  return { project_info: { project_id: target.projectId }, client: [{
    client_info: { android_client_info: { package_name: app.package }, mobilesdk_app_id: `fixture-${kind}` },
    oauth_client: [
      { client_type: 3, client_id: 'fixture-web-client' },
      { client_type: 1, android_info: { package_name: app.package, certificate_hash: 'aabb' } },
      ...(playPackage ? [{ client_type: 1, android_info: { package_name: playPackage, certificate_hash: 'ccdd' } }] : []),
    ],
  }] };
}

test('an upload-ready APK does not establish Play certificate readiness', () => {
  const report = inspectFirebaseSigning(target, kind => config(kind));
  assert.equal(report.localConfigReady, true);
  assert.equal(report.playConfigReady, false);
  assert.equal(report.nativeLiveSignIn, 'UNVERIFIED');
});
test('Play certificate must match the release package, not another app or debug app', () => {
  const report = inspectFirebaseSigning(target, kind => config(kind, 'com.creditrepairai.v2.debug'));
  assert.equal(report.playConfigReady, false);
});
test('genuine release certificate coverage permits Play configuration readiness only', () => {
  const report = inspectFirebaseSigning(target, kind => config(kind, 'com.creditrepairai.v2'));
  assert.equal(report.playConfigReady, true);
  assert.equal(report.consoleProviderStatus, 'UNVERIFIED');
  assert.equal(report.nativeLiveSignIn, 'UNVERIFIED');
  const missingCertificate = inspectFirebaseSigning({ ...target, playAppSigningSha1: null }, kind => config(kind, 'com.creditrepairai.v2'));
  assert.equal(missingCertificate.playConfigReady, false);
});
