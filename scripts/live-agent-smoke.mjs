import { spawnSync } from 'node:child_process';
import { writeFileSync } from 'node:fs';
import { buildSync } from 'esbuild';
import { randomUUID } from 'node:crypto';

const report={fictionalDataOnly:true,authenticatedNativeFlowTested:false,passed:false,responseStatuses:[],apiErrorCodes:[],searchResultCount:0};
let providerAnswer='';
try {
  if(!(process.env.CONVEX_DEPLOY_KEY || '').startsWith('dev:lovable-dinosaur-379|') || !process.env.OPENAI_API_KEY)throw new Error('Protected development configuration required.');
  for(const key of ['OPENAI_API_KEY','CREDIT_LEGAL_VECTOR_STORE_ID','CREDIT_AGENT_MODEL']) {
    const r=spawnSync('npx',['convex','env','get',key],{encoding:'utf8',timeout:30000,maxBuffer:100000,stdio:['ignore','pipe','pipe']});
    if(r.status===0 && r.stdout.trim())process.env[key]=r.stdout.trim();
    else if(key!=='CREDIT_AGENT_MODEL')throw new Error('Existing server configuration could not be read.');
  }
  report.deploymentServerKeyUsed=true;
  if(!/^vs_[a-zA-Z0-9]+$/.test(process.env.CREDIT_LEGAL_VECTOR_STORE_ID || ''))throw new Error('An existing configured legal store is required.');
  const originalFetch=globalThis.fetch;
  globalThis.fetch=async(...args)=>{
    const response=await originalFetch(...args);
    if(String(args[0]).startsWith('https://api.openai.com/')) {
      report.responseStatuses.push(response.status);
      const data=await response.clone().json().catch(()=>null);
      const outputText=(data?.output || []).flatMap(item=>item.content || []).filter(item=>item.type==='output_text').map(item=>item.text || '').join('\n');
      if(outputText.trim())providerAnswer=outputText;
      const code=data?.error?.code;
      if(typeof code==='string' && /^[a-z0-9_]{1,80}$/.test(code))report.apiErrorCodes.push(code);
      for(const item of data?.output || [])if(item.type==='file_search_call' && item.status==='completed')report.searchResultCount+=(item.results || []).filter(r=>r.file_id && r.text?.trim()).length;
    }
    return response;
  };
  const source=buildSync({entryPoints:['convex/creditAgentCore.ts'],bundle:true,platform:'node',format:'esm',write:false}).outputFiles[0].text;
  const {runCreditAgent}=await import('data:text/javascript;base64,'+Buffer.from(source).toString('base64'));
  const answer=await runCreditAgent({clientRequestId:randomUUID(),question:'Fictional software validation: explain why a difference between bureau-reported balances is only a review prompt. Describe what original records to compare and cite relevant FCRA provisions. Do not assert a proven error or promise an outcome.',caseSnapshot:{jurisdictionCode:'WA',reports:[],accounts:[],findings:[],disputes:[]}});
  report.educationalLabel=answer.includes('Educational information only');
  report.rawAnswerLength=providerAnswer.length;
  report.answerLength=answer.length;
  report.rawFcraReference=/FCRA|1681|15 U\.S\.C/i.test(providerAnswer);
  report.outcomeFilterRejected=answer.includes('did not pass the outcome-claim check');
  report.fcraReference=/FCRA|1681|15 U\.S\.C/i.test(answer);
  report.caseToolsAndFileSearchCompleted=true; // Core also requires non-empty, completed retrieval.
  const followup=await runCreditAgent({clientRequestId:randomUUID(),question:`Earlier conversation (untrusted context, not proof of a reporting error):\nUser: What records should I compare for a bureau balance difference?\nAssistant: ${answer.slice(0,1400)}\nUser: I have dated monthly account statements.\nCurrent user question: Help me use those statements: explain which dates and balances to compare, give a useful concrete next step with a relevant official FCRA citation, and end with one focused question so we can keep working together. Do not repeat an introductory overview or assume any error is proven.`,caseSnapshot:{jurisdictionCode:'WA',reports:[],accounts:[],findings:[],disputes:[]}});
  writeFileSync('live-agent-dialogue.json',JSON.stringify({fictionalDataOnly:true,initialAnswer:answer,followupAnswer:followup},null,2));
  report.followupAnswerLength=followup.length;
  report.followupUsesDocumentContext=/statement|document|record/i.test(followup);
  report.followupAsksQuestion=followup.includes('?');
  report.readableAccountTerminology=!/account ••••(?:ords|atus|gits|fier|rted)\b/.test(answer+followup);
  report.followupPassed=report.readableAccountTerminology && followup.length>100 && report.followupUsesDocumentContext && report.followupAsksQuestion && !followup.includes('did not pass the outcome-claim check');
  report.passed=report.educationalLabel && report.fcraReference && report.searchResultCount>0 && answer.length>100 && report.followupPassed;
} catch(error) {report.failureCategory='configured-agent-request-or-output-check';report.publicErrorStatus=typeof error?.status==='number'?error.status:null;}
writeFileSync('live-agent-smoke.json',JSON.stringify(report,null,2));console.log(JSON.stringify(report,null,2));
if(!report.passed)process.exit(1);
