import test from 'node:test';
import assert from 'node:assert/strict';
import {parseAnswerDraft} from '../src/workspace/answerDraft.ts';
import {validateCase} from '../src/workspace/vault.ts';
import {emptyCase} from '../src/workspace/model.ts';
test('only complete bounded editable answer drafts are accepted',()=>{
 const draft='WHAT_HAPPENED:\nVerified difference\nCOMPANY_RESPONSE:\n[ADD VERIFIED DETAILS]\nREQUESTED_REVIEW:\nInvestigate the balance\nEVIDENCE:\nDated statement\nEND_DRAFT';
 assert.equal(parseAnswerDraft(draft).problem,'Verified difference');
 assert.throws(()=>parseAnswerDraft(draft.replace('END_DRAFT','')));assert.throws(()=>parseAnswerDraft(draft.replace('COMPANY_RESPONSE:','EVIDENCE:')));assert.throws(()=>parseAnswerDraft(draft.replace('Dated statement','x'.repeat(2001))));
});
test('vault schema rejects malformed records and normalizes Android bureau enums',()=>{
 const s=emptyCase();s.scores=[{bureau:'EQUIFAX',score:640,recordedAt:1}];assert.equal(validateCase(s).scores[0].bureau,'equifax');
 assert.throws(()=>validateCase({...emptyCase(),accounts:[{}]}));assert.throws(()=>validateCase({...emptyCase(),scores:[{bureau:'equifax',score:900,recordedAt:1}]}));
});
