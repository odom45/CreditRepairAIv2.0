import { readFileSync, writeFileSync } from 'node:fs';
const target = JSON.parse(readFileSync('config/firebase-signing.json', 'utf8'));
const normalize = value => (value || '').replace(/:/g, '').toLowerCase();
const rows = target.apps.map(app => {
  const config = JSON.parse(readFileSync(app.config, 'utf8'));
  const client = config.client?.find(c => c.client_info?.android_client_info?.package_name === app.package);
  return {
    package: app.package, config: app.config, projectMatches: config.project_info?.project_id === target.projectId,
    firebaseAppId: client?.client_info?.mobilesdk_app_id || null,
    webClientPresent: Boolean(client?.oauth_client?.some(c => c.client_type === 3 && c.client_id)),
    uploadCertificateRegisteredInDownloadedConfig: Boolean(client?.oauth_client?.some(c => c.client_type === 1 && c.android_info?.package_name === app.package && normalize(c.android_info.certificate_hash) === normalize(target.ownerUploadCertificate.sha1))),
  };
});
const distinctAndroidAppIds = new Set(rows.map(r => r.firebaseAppId).filter(Boolean)).size === rows.length;
const report = { projectId: target.projectId, certificate: target.ownerUploadCertificate, distinctAndroidAppIds,
  localConfigReady: distinctAndroidAppIds && rows.every(r => r.projectMatches && r.webClientPresent && r.uploadCertificateRegisteredInDownloadedConfig),
  consoleProviderStatus: 'UNVERIFIED', nativeLiveSignIn: 'UNVERIFIED', playAppSigningCertificate: target.playAppSigningSha1 || 'OWNER_INPUT_REQUIRED', apps: rows };
writeFileSync('firebase-signing-readiness.json', JSON.stringify(report, null, 2) + '\n');
console.log(JSON.stringify(report, null, 2)); // Public app/certificate metadata; no client API keys, tokens or passwords.
if (process.argv.includes('--require-ready') && !report.localConfigReady) process.exit(1);
