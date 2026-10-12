import { spawnSync } from 'node:child_process';
import { writeFileSync } from 'node:fs';
const result = spawnSync('npm', ['audit','--json'], { encoding:'utf8', timeout:60000, maxBuffer:10_000_000, stdio:['ignore','pipe','pipe'] });
let audit; try { audit = JSON.parse(result.stdout); } catch { console.log('Dependency advisory lookup unavailable.'); process.exit(1); }
writeFileSync('dependency-audit.json', JSON.stringify(audit,null,2));
console.log(JSON.stringify({ counts:audit.metadata?.vulnerabilities, findings:Object.values(audit.vulnerabilities || {}).map(v => ({ name:v.name,severity:v.severity,isDirect:v.isDirect,range:v.range,fixAvailable:v.fixAvailable,via:v.via.filter(x => typeof x==='object').map(x=>({title:x.title,url:x.url,range:x.range})) })) },null,2));
if ((audit.metadata?.vulnerabilities?.high || 0) + (audit.metadata?.vulnerabilities?.critical || 0) > 0) process.exit(1);
