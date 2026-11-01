import { cpSync, mkdirSync, readdirSync } from 'node:fs';
mkdirSync('public/ocr/core',{recursive:true});
cpSync('node_modules/tesseract.js/dist/worker.min.js','public/ocr/worker.min.js');
for (const name of readdirSync('node_modules/tesseract.js-core')) if (/\.(?:js|wasm)$/.test(name)) cpSync(`node_modules/tesseract.js-core/${name}`,`public/ocr/core/${name}`);
cpSync('node_modules/@tesseract.js-data/eng/4.0.0_best_int/eng.traineddata.gz','public/ocr/eng.traineddata.gz');
console.log('Bundled local OCR worker, WASM cores and English model.');
