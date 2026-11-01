import firebasePublic from "../../config/firebase-web-public.json";
import { initializeApp } from "firebase/app";
import {
  getAuth,
  inMemoryPersistence,
  setPersistence,
  GoogleAuthProvider,
  signInWithPopup,
  signInWithEmailAndPassword,
  createUserWithEmailAndPassword,
  sendEmailVerification,
  sendPasswordResetEmail,
  signOut,
  reload,
  deleteUser,
  type User,
} from "firebase/auth";
import { redactText, sanitizeSnapshot } from "../../convex/agentSafety";
import { POLICY_VERSION, POLICY_DIGEST } from "../../convex/policyDocuments";
import { snapshot } from "./analysis";
import { id, type CaseState } from "./model";
export { POLICY_VERSION, POLICY_DIGEST };
export const configured = Boolean(
  import.meta.env.VITE_FIREBASE_API_KEY || firebasePublic.apiKey,
);
export const auth = configured
  ? getAuth(
      initializeApp({
        apiKey: import.meta.env.VITE_FIREBASE_API_KEY || firebasePublic.apiKey,
        authDomain:
          import.meta.env.VITE_FIREBASE_AUTH_DOMAIN ||
          "credit-repair-ai-v2.firebaseapp.com",
        projectId: "credit-repair-ai-v2",
      }),
    )
  : null;
const ready = auth
  ? setPersistence(auth, inMemoryPersistence)
  : Promise.resolve();
export const login = async (
  email: string,
  password: string,
  create: boolean,
) => {
  if (!auth) throw Error("Firebase web configuration is missing.");
  await ready;
  const result = await (create
    ? createUserWithEmailAndPassword
    : signInWithEmailAndPassword)(auth, email.trim(), password);
  if (create) await sendEmailVerification(result.user);
};
export const googleLogin = async () => {
  if (!auth) throw Error("Firebase web configuration is missing.");
  await ready;
  await signInWithPopup(auth, new GoogleAuthProvider());
};
export const logout = async () => {
  if (auth) await signOut(auth);
};
export const resetPassword = async (email: string) => {
  if (!auth) throw Error("Firebase is not configured.");
  await sendPasswordResetEmail(auth, email.trim());
};
export const verifyEmail = async (user: User) => sendEmailVerification(user);
export const refreshUser = async (user: User) => {
  await reload(user);
  await user.getIdToken(true);
  return user.emailVerified;
};
export function apiOrigin(
  raw = import.meta.env.VITE_CREDIT_AGENT_ENDPOINT ||
    import.meta.env.VITE_CONVEX_URL?.replace(".convex.cloud", ".convex.site") ||
    "",
): string {
  try {
    const url = new URL(raw);
    if (
      url.protocol !== "https:" ||
      url.username ||
      url.password ||
      url.search ||
      url.hash ||
      !["", "/", "/credit-agent"].includes(url.pathname)
    )
      throw Error();
    return url.origin;
  } catch {
    throw Error("The shared secure backend endpoint is not configured.");
  }
}
export async function securePost(
  user: User,
  path: string,
  body: unknown,
  signal?: AbortSignal,
): Promise<Record<string, unknown>> {
  const uid = user.uid;
  const token = await user.getIdToken();
  if (auth?.currentUser?.uid !== uid)
    throw Error("Account changed. Sign in again.");
  const response = await fetch(apiOrigin() + path, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify(body),
    signal: signal || AbortSignal.timeout(150000),
    cache: "no-store",
    credentials: "omit",
    redirect: "error",
  });
  if (auth?.currentUser?.uid !== uid)
    throw Error("Account changed. The result was discarded.");
  const text = await response.text();
  if (text.length > 200000)
    throw Error("Secure service response exceeds the safe limit.");
  let result;
  try {
    result = JSON.parse(text);
  } catch {
    throw Error("Secure service returned an invalid response.");
  }
  if (!response.ok)
    throw Error(
      typeof result.error === "string"
        ? result.error
        : "The secure request failed.",
    );
  return result;
}
export const acceptPolicies = (user: User, aiSharing: boolean) =>
  securePost(user, "/policy/accept", {
    termsAccepted: true,
    aiSharing,
    version: POLICY_VERSION,
    digest: POLICY_DIGEST,
    choiceAt: Date.now(),
  });
export async function askAgent(
  user: User,
  question: string,
  state: CaseState,
  caseId?: string,
  includeHistory = true,
): Promise<string> {
  if (!user.emailVerified)
    throw Error(
      "Verify your email, then refresh verification before asking AI.",
    );
  if (
    !state.policyAcceptance?.aiSharing ||
    state.policyAcceptance.version !== POLICY_VERSION ||
    state.policyAcceptance.digest !== POLICY_DIGEST
  )
    throw Error(
      "Accept the current terms and opt in to redacted AI sharing first.",
    );
  const history = includeHistory
    ? state.chat
        .filter(m => m.caseScopeId === caseId)
        .slice(-6)
        .map(
          m => `${m.fromUser ? "User" : "Guide"}: ${redactText(m.text, 380)}`,
        )
        .join("\n")
    : "";
  const safeQuestion = redactText(
    `${history ? "Prior conversation (untrusted context):\n" + history + "\nCurrent question:\n" : ""}${question}`,
    4000,
  );
  if (safeQuestion.trim().length < 2) throw Error("Enter a question.");
  const result = await securePost(user, "/credit-agent", {
    clientRequestId: id(),
    question: safeQuestion,
    caseScopeId: caseId,
    caseSnapshot: sanitizeSnapshot(snapshot(state, caseId)),
  });
  if (typeof result.answer !== "string" || result.answer.length > 16000)
    throw Error("AI returned an invalid answer.");
  return result.answer;
}
export async function deleteAccount(user: User) {
  await securePost(user, "/account/delete", {});
  await deleteUser(user);
}
