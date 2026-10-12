import { readFileSync } from 'node:fs';
const [file] = process.argv.slice(2);
if (!file) throw new Error('Provide the measured parser JSON, not an instrumentation success percentage.');
const data = JSON.parse(readFileSync(file, 'utf8'));
if (data.total !== 60 || data.cases?.length !== 60 || data.supportedCases !== 54) throw new Error('Incomplete/unexpected synthetic corpus.');
const percent = (n,d) => `${(100*n/d).toFixed(2)}%`;
console.log(JSON.stringify({ phase:data.phase, androidApi:data.androidApi, fixtureKind:data.fixtureKind,
  accepted:`${data.supportedAccepted}/${data.supportedCases} (${percent(data.supportedAccepted,data.supportedCases)})`,
  completeAccurateReports:`${data.supportedExactPass}/${data.supportedCases} (${percent(data.supportedExactPass,data.supportedCases)})`,
  coreFields:`${data.fieldMatches}/${data.fieldTotal} (${percent(data.fieldMatches,data.fieldTotal)})`,
  safelyRejected:`${data.guardsSafelyRejected}/${data.guards}`,
  failures:data.cases.filter(c=>!c.exactPass).map(c=>({id:c.id,error:c.error,fields:c.fieldMatches,count:c.accountCount})) },null,2));
