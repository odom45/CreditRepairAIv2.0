import { spawnSync } from 'node:child_process';
import { writeFileSync } from 'node:fs';
const report = { deployment:'lovable-dinosaur-379', kind:'development', status:'blocked' };
if (!(process.env.CONVEX_DEPLOY_KEY || '').startsWith('dev:lovable-dinosaur-379|')) throw new Error('This unattended workflow accepts only the owner-authorized development deployment key.');
// Existing server keys/vector store are preserved. No vector store or signing key is created.
const result = spawnSync('npx', ['convex','deploy','--yes','--typecheck','enable'], {encoding:'utf8',timeout:180000,maxBuffer:4_000_000,stdio:['ignore','pipe','pipe']});
report.status = result.status === 0 ? 'deployed' : 'failed';
// Capture only generic error categories, never CLI bodies or environment values.
if (result.status !== 0) report.failureCategory = /schema|validator/i.test(result.stderr || '') ? 'schema-or-validator' : /typescript|typecheck|TS\d/i.test(result.stderr || '') ? 'typecheck' : 'deployment-or-configuration';
writeFileSync('development-deployment.json', JSON.stringify(report,null,2));
console.log(JSON.stringify(report,null,2));
if (result.status !== 0) process.exit(1);
