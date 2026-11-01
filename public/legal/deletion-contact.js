const email=window.creditRepairSupportEmail;
if(typeof email==='string' && /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
 const box=document.getElementById('support');box.textContent='';
 const link=document.createElement('a');link.className='request';link.textContent='Request deletion — '+email;
 link.href='mailto:'+encodeURIComponent(email)+'?subject='+encodeURIComponent('CreditRepairAI account deletion request')+'&body='+encodeURIComponent('Please delete my CreditRepairAI account and associated app data. My app sign-in email is the sender address. Please confirm any ownership verification needed and the completion status.');box.append(link);
 const text=document.createElement('p');text.textContent='If your device has no email app, send a message to '+email+' from your own email service. Keep a copy of your request.';box.append(text);
}
