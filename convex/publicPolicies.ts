import { httpAction } from './_generated/server';
import { PUBLIC_POLICY_ASSETS } from './publicPolicyDocuments';
export const publicPolicies = httpAction(async (_ctx, request) => {
  const path = new URL(request.url).pathname;
  if (path === '/legal/support-config.js') {
    const raw = process.env.CREDIT_SUPPORT_EMAIL?.trim() || '';
    const email = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(raw) ? raw : null;
    return new Response(`window.creditRepairSupportEmail=${JSON.stringify(email)};`, {headers:{'Content-Type':'text/javascript; charset=utf-8','Cache-Control':'no-store','X-Content-Type-Options':'nosniff'}});
  }
  const body = PUBLIC_POLICY_ASSETS[path];
  return new Response(body || 'Not found', {status:body?200:404, headers:{
    'Content-Type':path.endsWith('.js')?'text/javascript; charset=utf-8':'text/html; charset=utf-8',
    'Cache-Control':'no-store','X-Content-Type-Options':'nosniff','Referrer-Policy':'no-referrer',
    'Content-Security-Policy':"default-src 'none'; script-src 'self'; style-src 'unsafe-inline'; base-uri 'none'; frame-ancestors 'none'; form-action 'none'",
  }});
});
