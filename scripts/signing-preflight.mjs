import { mkdtempSync,writeFileSync,rmSync,readFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { spawnSync } from 'node:child_process';
const required=['ANDROID_KEYSTORE_BASE64','ANDROID_KEYSTORE_PASSWORD','ANDROID_KEY_ALIAS','ANDROID_KEY_PASSWORD'];
const result={existingOwnerKey:true,credentialsValid:false};
const directory=mkdtempSync(join(tmpdir(),'creditrepair-signing-check-'));
try {
  if(required.some(k=>!process.env[k])) throw new Error('Missing protected signing configuration.');
  const path=join(directory,'owner-upload.jks');writeFileSync(path,Buffer.from(process.env.ANDROID_KEYSTORE_BASE64,'base64'),{mode:0o600});
  const inspect=spawnSync('keytool',['-list','-v','-keystore',path,'-alias',process.env.ANDROID_KEY_ALIAS,'-storepass:env','ANDROID_KEYSTORE_PASSWORD'],{encoding:'utf8',timeout:15000});
  if(inspect.status!==0) throw new Error('Existing keystore/alias validation failed.');
  result.certificateSha1=inspect.stdout.match(/SHA1:\s*([A-Fa-f0-9:]+)/)?.[1]?.replaceAll(':','').toLowerCase();
  result.certificateSha256=inspect.stdout.match(/SHA256:\s*([A-Fa-f0-9:]+)/)?.[1]?.replaceAll(':','').toLowerCase();
  writeFileSync(join(directory,'probe.txt'),'Fictional signing credential validation only.');
  const jar=join(directory,'probe.jar');
  if(spawnSync('jar',['--create','--file',jar,'-C',directory,'probe.txt'],{encoding:'utf8',timeout:15000}).status!==0)throw new Error('Signing probe unavailable.');
  const sign=spawnSync('jarsigner',['-keystore',path,'-storepass:env','ANDROID_KEYSTORE_PASSWORD','-keypass:env','ANDROID_KEY_PASSWORD',jar,process.env.ANDROID_KEY_ALIAS],{encoding:'utf8',timeout:15000});
  const verify=spawnSync('jarsigner',['-verify',jar],{encoding:'utf8',timeout:15000});
  result.credentialsValid=sign.status===0 && verify.status===0 && /jar verified/.test(verify.stdout);
  result.firebaseCertificateMatches=Object.fromEntries(['main','debug'].map(kind=>{
    const config=JSON.parse(readFileSync(kind==='main' ? 'native-android/app/google-services.json' : 'native-android/app/src/debug/google-services.json','utf8'));
    return [kind,config.client.some(c=>c.oauth_client?.some(o=>o.client_type===1 && o.android_info?.certificate_hash?.toLowerCase()===result.certificateSha1))];
  }));
} catch { result.failure='Signing inputs or provider registration require owner correction; no values exposed.'; }
finally {rmSync(directory,{recursive:true,force:true});}
writeFileSync('signing-preflight.json',JSON.stringify(result,null,2));console.log(JSON.stringify(result,null,2));
