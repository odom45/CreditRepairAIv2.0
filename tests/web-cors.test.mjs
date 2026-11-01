import test from 'node:test';
import assert from 'node:assert/strict';
import { buildSync } from 'esbuild';
const source=buildSync({entryPoints:['convex/browserCors.ts'],bundle:true,platform:'node',format:'esm',write:false}).outputFiles[0].text;
const {withBrowserCors,allowedWebOrigin}=await import('data:text/javascript;base64,'+Buffer.from(source).toString('base64'));
test('CORS requires exact allowlisted origin, preserves Android, covers error responses',async()=> {
 process.env.CREDIT_WEB_ORIGINS='https://example.test,http://localhost:5173';
 assert.equal(allowedWebOrigin('https://example.test'),true);assert.equal(allowedWebOrigin('https://example.test.attacker.test'),false);assert.equal(allowedWebOrigin('null'),false);assert.equal(allowedWebOrigin('http://example.test'),false);
 const handler=withBrowserCors(async()=>new Response('{"error":"verify email"}',{status:403}));
 const preflight=await handler({},new Request('https://backend.test/credit-agent',{method:'OPTIONS',headers:{Origin:'https://example.test'}}));
 assert.equal(preflight.status,204);assert.equal(preflight.headers.get('Access-Control-Allow-Origin'),'https://example.test');assert.ok(preflight.headers.get('Access-Control-Allow-Headers').includes('Authorization'));
 const error=await handler({},new Request('https://backend.test/credit-agent',{method:'POST',headers:{Origin:'https://example.test'}}));assert.equal(error.status,403);assert.equal(error.headers.get('Access-Control-Allow-Origin'),'https://example.test');
 const native=await handler({},new Request('https://backend.test/credit-agent',{method:'POST'}));assert.equal(native.status,403);assert.equal(native.headers.get('Access-Control-Allow-Origin'),null);
 const blocked=await handler({},new Request('https://backend.test/credit-agent',{method:'POST',headers:{Origin:'https://evil.test'}}));assert.equal(blocked.status,403);assert.equal(blocked.headers.get('Access-Control-Allow-Origin'),null);
 delete process.env.CREDIT_WEB_ORIGINS;
});
