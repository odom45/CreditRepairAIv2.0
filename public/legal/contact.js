const policyEmail = window.creditRepairSupportEmail;
const policyContact = document.getElementById('public-contact');
if (policyContact && typeof policyEmail === 'string' && /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(policyEmail)) {
  policyContact.textContent = 'Support and privacy contact: ';
  const link = document.createElement('a');
  link.textContent = policyEmail; link.href = 'mailto:' + encodeURIComponent(policyEmail);
  policyContact.append(link);
}
