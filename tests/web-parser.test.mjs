import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { parseText, validateText } from '../src/workspace/parser.ts';
import { analyze, chunks, cfpbReady, canReview, submitReceipt, snapshot } from '../src/workspace/analysis.ts';
import { emptyCase, DAY } from '../src/workspace/model.ts';
const base = 'tests/fixtures/parser/corpus/';
for (const row of readFileSync(base+'manifest.tsv','utf8').trim().split('\n').filter(l=>!l.startsWith('#'))) {
 const [file,bureau,score,suffixes,balances] = row.split('\t');
 test(`Android fixture parity: ${file}`,()=>{
  const parsed = parseText(readFileSync(base+file,'utf8'),file);
  assert.equal(parsed.report.bureau,bureau.toLowerCase()); assert.equal(parsed.report.score,score==='-'?null:+score);
  assert.deepEqual(parsed.accounts.map(a=>a.accountSuffix),suffixes==='-'?[]:suffixes.split(','));
  assert.deepEqual(parsed.accounts.map(a=>a.balance),balances==='-'?[]:balances.split(',').map(Number));
 });
}
test('limits reject before normalization, with no account truncation',()=> {
 for(const text of ['a'.repeat(4097),'a\n'.repeat(50001),'a'.repeat(1000001)]) assert.throws(()=>validateText(text));
 assert.throws(()=>parseText(Array.from({length:151},(_,i)=>`Creditor: Fake ${i}\nAccount Number: XXXX${1000+i}\nBalance: 10`).join('\n'),'equifax.txt'));
});
test('2,400 seeded mutations terminate and respect account and suffix bounds',()=> {
 let seed=0x6319;const rnd=()=>{seed=(Math.imul(seed,1664525)+1013904223)>>>0;return seed;};
 const source=readFileSync(base+'experian-inline.txt','utf8');
 for(let i=0;i<2400;i++) {const at=rnd()%source.length;const text=source.slice(0,at)+['\u00a0','|','\n','\u0000','broken:',''].at(rnd()%6)+source.slice(at+1);const result=parseText(text,'report.txt');assert.ok(result.accounts.length<=150);assert.ok(result.accounts.every(a=>a.accountSuffix.length<=4));assert.equal(new Set(result.accounts.map(a=>a.id)).size,result.accounts.length);}
});
test('analysis matches Android balance, status, missing-bureau and identity rules',()=> {
 const a=parseText('Equifax\nCreditor: Fictional Bank\nAccount Number: XXXX1234\nBalance: 100\nStatus: Open','a');
 const b=parseText('Experian\nCreditor: Fictional Bank\nAccount Number: XXXX1234\nBalance: 300\nStatus: Closed','b');
 assert.deepEqual(new Set(analyze([...a.accounts,...b.accounts],[a.report,b.report]).map(f=>f.type)),new Set(['BALANCE_MISMATCH','STATUS_MISMATCH']));
 const unmatched=b.accounts.map(a=>({...a,accountSuffix:'',openedDate:''}));
 assert.ok(!analyze([...a.accounts,...unmatched],[a.report,b.report]).some(f=>f.type==='BALANCE_MISMATCH'));
});
test('guided filing never marks placeholders or unreviewed facts submitted',()=> {
 const f={id:'f',disputeId:'d',portal:'equifax',step:6,reviewed:false,problem:'Actual mismatch',evidence:'Dated statements',resolution:'Review balance',companyResponse:'',confirmation:'receipt'};
 assert.throws(()=>submitReceipt(f,1));assert.equal(canReview({...f,evidence:'[ADD DOCUMENT]'}),false);assert.throws(()=>submitReceipt({...f,reviewed:true,evidence:'[ADD DOCUMENT]'},1));
 assert.equal(submitReceipt({...f,reviewed:true},1).submittedAt,1);
});
test('CFPB readiness excludes future, missing and reversed response dates',()=> {
 const d={id:'d',sentAt:100,craResponseDueAt:100+45*DAY};
 assert.equal(cfpbReady(d,[],[],100+44*DAY),false);assert.equal(cfpbReady(d,[],[],100+45*DAY),true);
 assert.equal(cfpbReady(d,[{disputeId:'d',portal:'equifax',submittedAt:100,responseReceivedAt:99}],[],200),false);
 assert.equal(cfpbReady(d,[{disputeId:'d',portal:'equifax',submittedAt:100,responseReceivedAt:200}],[],200),true);
 assert.equal(cfpbReady(d,[],[{disputeId:'d',mailedAt:100,deliveredAt:99}],100+45*DAY),true); // own due date still applies
 assert.equal(cfpbReady({...d,craResponseDueAt:undefined},[],[{disputeId:'d',mailedAt:100,deliveredAt:99}],100+45*DAY),false);
});
test('chunking preserves all content, and case limits fail closed',()=> {
 const text='x'.repeat(1300);assert.equal(chunks(text,100).join(''),text);assert.throws(()=>chunks(text,0));
 const s=emptyCase();s.accounts=Array(201).fill({});assert.throws(()=>snapshot(s));assert.throws(()=>snapshot(emptyCase(),'missing'));
});
