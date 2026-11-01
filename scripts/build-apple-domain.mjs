import { buildSync } from 'esbuild';
import { cpSync, mkdirSync } from 'node:fs';
mkdirSync('native-apple/Resources',{recursive:true});
buildSync({entryPoints:['src/shared/domain.ts'],bundle:true,format:'iife',globalName:'CreditDomain',target:'es2022',outfile:'native-apple/Resources/domain.js'});
cpSync('tests/fixtures/parser','native-apple/Tests/Fixtures',{recursive:true});
console.log('Built the exact web parser, analysis, redaction and guidance domain for JavaScriptCore.');
