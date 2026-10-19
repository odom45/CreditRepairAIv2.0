import { readFileSync, writeFileSync } from 'node:fs';
import { pathToFileURL } from 'node:url';
const normalize = value => (value || '').replace(/:/g, '').toLowerCase();
export function inspectFirebaseSigning(target, loadConfig = path => JSON.parse(readFileSync(path, 'utf8'))) {
  const rows = target.apps.map(app => {
    const config = loadConfig(app.config);
    const client = config.client?.find(c => c.client_info?.android_client_info?.package_name === app.package);
    return {
      package: app.package, config: app.config, projectMatches: config.project_info?.project_id === target.projectId,
      firebaseAppId: client?.client_info?.mobilesdk_app_id || null,
      webClientPresent: Boolean(client?.oauth_client?.some(c => c.client_type === 3 && c.client_id)),
      uploadCertificateRegisteredInDownloadedConfig: Boolean(client?.oauth_client?.some(c => c.client_type === 1 && c.android_info?.package_name === app.package && normalize(c.android_info.certificate_hash) === normalize(target.ownerUploadCertificate.sha1))),
      playCertificateRegisteredInDownloadedConfig: Boolean(target.playAppSigningSha1 && client?.oauth_client?.some(c => c.client_type === 1 && c.android_info?.package_name === app.package && normalize(c.android_info.certificate_hash) === normalize(target.playAppSigningSha1))),
    };
  });
  const distinctAndroidAppIds = new Set(rows.map(r => r.firebaseAppId).filter(Boolean)).size === rows.length;
  const localConfigReady = distinctAndroidAppIds && rows.every(r => r.projectMatches && r.webClientPresent && r.uploadCertificateRegisteredInDownloadedConfig);
  const release = rows.find(r => r.package === 'com.creditrepairai.v2');
  const report = { projectId: target.projectId, certificate: target.ownerUploadCertificate, distinctAndroidAppIds,
    localConfigReady,
    playConfigReady: localConfigReady && Boolean(release?.playCertificateRegisteredInDownloadedConfig),
    consoleProviderStatus: 'UNVERIFIED', nativeLiveSignIn: 'UNVERIFIED', playAppSigningCertificate: target.playAppSigningSha1 || 'OWNER_INPUT_REQUIRED', apps: rows };
  return report;
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  const target = JSON.parse(readFileSync('config/firebase-signing.json', 'utf8'));
  const report = inspectFirebaseSigning(target);
  writeFileSync('firebase-signing-readiness.json', JSON.stringify(report, null, 2) + '\n');
  console.log(JSON.stringify(report, null, 2)); // Public app/certificate metadata; no client API keys, tokens or passwords.
  if (process.argv.includes('--require-ready') && !report.localConfigReady) process.exit(1);
  if (process.argv.includes('--require-play-ready') && !report.playConfigReady) process.exit(1);
}
