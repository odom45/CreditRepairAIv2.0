import { emptyCase, type CaseState } from "./model.ts";
import { caseSchema } from "./validation.ts";
export interface Envelope {
  format: "creditrepairai-vault-v1";
  salt: string;
  iv: string;
  ciphertext: string;
}
const encode = (b: Uint8Array) =>
  btoa(Array.from(b, v => String.fromCharCode(v)).join(""));
const decode = (s: string) => Uint8Array.from(atob(s), c => c.charCodeAt(0));
export async function deriveKey(
  password: string,
  salt: Uint8Array<ArrayBuffer>,
): Promise<CryptoKey> {
  if (password.length < 12)
    throw Error("Use a vault passphrase with at least 12 characters.");
  const material = await crypto.subtle.importKey(
    "raw",
    new TextEncoder().encode(password),
    "PBKDF2",
    false,
    ["deriveKey"],
  );
  return crypto.subtle.deriveKey(
    { name: "PBKDF2", salt, iterations: 310000, hash: "SHA-256" },
    material,
    { name: "AES-GCM", length: 256 },
    false,
    ["encrypt", "decrypt"],
  );
}
export async function seal(
  state: CaseState,
  key: CryptoKey,
  salt: Uint8Array,
  owner: string,
): Promise<Envelope> {
  const validated = validateCase(state);
  const iv = crypto.getRandomValues(new Uint8Array(12));
  const ciphertext = await crypto.subtle.encrypt(
    {
      name: "AES-GCM",
      iv,
      additionalData: new TextEncoder().encode("creditrepairai-v1:" + owner),
    },
    key,
    new TextEncoder().encode(JSON.stringify(validated)),
  );
  return {
    format: "creditrepairai-vault-v1",
    salt: encode(salt),
    iv: encode(iv),
    ciphertext: encode(new Uint8Array(ciphertext)),
  };
}
export async function unseal(
  envelope: Envelope,
  password: string,
  owner: string,
): Promise<{
  state: CaseState;
  key: CryptoKey;
  salt: Uint8Array<ArrayBuffer>;
}> {
  if (
    envelope.format !== "creditrepairai-vault-v1" ||
    envelope.ciphertext.length > 28_000_000
  )
    throw Error("Unsupported or oversized vault.");
  const salt = decode(envelope.salt);
  const iv = decode(envelope.iv);
  if (salt.length !== 16 || iv.length !== 12) throw Error("Invalid vault.");
  const key = await deriveKey(password, salt);
  const raw = await crypto.subtle.decrypt(
    {
      name: "AES-GCM",
      iv,
      additionalData: new TextEncoder().encode("creditrepairai-v1:" + owner),
    },
    key,
    decode(envelope.ciphertext),
  );
  const state = validateCase(JSON.parse(new TextDecoder().decode(raw)));
  return { state, key, salt };
}
export function validateCase(raw: unknown): CaseState {
  if (JSON.stringify(raw).length > 20_000_000)
    throw Error("Case file exceeds 20 MB.");
  const result = caseSchema.safeParse(raw);
  if (!result.success)
    throw Error("Case file has invalid or unsupported fields.");
  return result.data as CaseState;
}
export const vaultName = (uid: string) => "creditrepairai:encrypted:" + uid;
export async function openVault(
  uid: string,
  password: string,
  saved = localStorage.getItem(vaultName(uid)),
) {
  if (saved) return unseal(JSON.parse(saved), password, uid);
  const salt = crypto.getRandomValues(new Uint8Array(16));
  const key = await deriveKey(password, salt);
  return { state: emptyCase(), key, salt };
}
