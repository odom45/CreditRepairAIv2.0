import test from 'node:test';
import assert from 'node:assert/strict';
import { emptyCase } from '../src/workspace/model.ts';
import { deriveKey, seal, unseal } from '../src/workspace/vault.ts';
test('AES-GCM vault roundtrip, wrong key/owner and tampering protections',async()=> {
 const state=emptyCase(); state.goal.description='Fictional private notes';
 const salt=crypto.getRandomValues(new Uint8Array(16)), key=await deriveKey('synthetic passphrase only',salt);
 const a=await seal(state,key,salt,'fictional-owner'), b=await seal(state,key,salt,'fictional-owner');
 assert.notEqual(a.iv,b.iv);assert.ok(!JSON.stringify(a).includes(state.goal.description));
 assert.deepEqual((await unseal(a,'synthetic passphrase only','fictional-owner')).state,state);
 await assert.rejects(unseal(a,'wrong password here','fictional-owner'));await assert.rejects(unseal(a,'synthetic passphrase only','different-owner'));
 await assert.rejects(unseal({...a,ciphertext:(a.ciphertext[0]==='A'?'B':'A')+a.ciphertext.slice(1)},'synthetic passphrase only','fictional-owner'));
});
test('invalid in-memory records cannot overwrite a recoverable encrypted vault',async()=> {
 const state=emptyCase();state.scores=[{bureau:'equifax',score:900,recordedAt:1}];
 const salt=crypto.getRandomValues(new Uint8Array(16)),key=await deriveKey('synthetic test passphrase',salt);
 await assert.rejects(seal(state,key,salt,'owner'));
});
