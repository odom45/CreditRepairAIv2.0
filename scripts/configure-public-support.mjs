import { writeFileSync } from 'node:fs';
const email = (process.env.CREDIT_SUPPORT_EMAIL || process.env.VITE_CREDIT_SUPPORT_EMAIL || '').trim();
if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) throw new Error('A valid monitored support contact is required.');
writeFileSync('public/legal/support-config.js', `// Configured public contact; no credentials.\nwindow.creditRepairSupportEmail = ${JSON.stringify(email || null)};\n`);
console.log(`Public deletion contact: ${email ? 'configured (owner must verify mailbox operation)' : 'not configured; commercial publication remains blocked'}.`);
