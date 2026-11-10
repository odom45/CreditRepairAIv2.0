import Foundation
import SwiftUI
import CryptoKit
import FirebaseCore
import FirebaseAuth
import GoogleSignIn
#if os(macOS)
import AppKit
#else
import UIKit
#endif
@MainActor final class SessionStore:ObservableObject {
 @Published var user:User?
 @Published var document:CaseDocument?
 @Published var busy=false
 @Published var notice=""
 @Published var progress=""
 @Published var answer=""
 let domain:Domain?
 private var vaultKey:SymmetricKey?
 private var vaultSalt:Data?
 private var epoch=0
 private var loginAttempt=0
 private var acceptingSignIn=false
 private var authHandle:AuthStateDidChangeListenerHandle?
 var isAuthenticatingGoogle=false
 var configured:Bool {FirebaseApp.app() != nil}
 init() {
  domain=try? Domain()
  if let path=Bundle.main.path(forResource:"GoogleService-Info",ofType:"plist"),let options=FirebaseOptions(contentsOfFile:path),options.projectID=="credit-repair-ai-v2" {FirebaseApp.configure(options:options)}
  if configured {
   try? Auth.auth().signOut();GIDSignIn.sharedInstance.signOut()
   authHandle=Auth.auth().addStateDidChangeListener{[weak self] _,user in Task{@MainActor in guard let self else{return};if user != nil && !self.acceptingSignIn {try? Auth.auth().signOut();return};if self.user?.uid != user?.uid {self.epoch+=1;self.document=nil;self.vaultKey=nil;self.vaultSalt=nil;self.answer=""};self.user=user}}
  }
 }
 func run(_ body:@escaping @MainActor ()async throws->Void) {
  guard !busy else{return};busy=true;notice="";let ticket=epoch
  Task {do {try await body()}catch{if epoch==ticket{notice=error.localizedDescription}};if epoch==ticket{busy=false;progress=""}}
 }
 func lock() {epoch+=1;loginAttempt+=1;acceptingSignIn=false;document=nil;vaultKey=nil;vaultSalt=nil;answer="";user=nil;busy=false;if configured{try? Auth.auth().signOut()};GIDSignIn.sharedInstance.signOut()}
 func signIn(_ email:String,password:String,create:Bool)async throws {
  guard configured else{throw ClientError("Register this Apple bundle in the existing Firebase project and include its GoogleService-Info.plist.")}
  let attempt=loginAttempt;acceptingSignIn=true
  let result=create ? try await Auth.auth().createUser(withEmail:email.trimmingCharacters(in:.whitespaces),password:password) : try await Auth.auth().signIn(withEmail:email.trimmingCharacters(in:.whitespaces),password:password)
  guard attempt==loginAttempt else{throw ClientError("Session changed.")}
  user=result.user;busy=false
  if create {try await sendVerificationEmail(result.user)}
 }
 func signInGoogle()async throws {
  guard configured,let clientID=FirebaseApp.app()?.options.clientID else{throw ClientError("This Apple app's Google client configuration is missing.")}
  let attempt=loginAttempt;acceptingSignIn=true
  isAuthenticatingGoogle=true;defer{isAuthenticatingGoogle=false}
  GIDSignIn.sharedInstance.configuration=GIDConfiguration(clientID:clientID)
  #if os(macOS)
  guard let presenter=NSApp.keyWindow else{throw ClientError("Open the app window before signing in.")}
  #else
  guard let presenter=UIApplication.shared.connectedScenes.compactMap({$0 as? UIWindowScene}).flatMap({$0.windows}).first(where:{$0.isKeyWindow})?.rootViewController else{throw ClientError("No sign-in window is available.")}
  #endif
  let result=try await GIDSignIn.sharedInstance.signIn(withPresenting:presenter)
  guard let token=result.user.idToken?.tokenString else{throw ClientError("Google returned no ID token.")}
  guard attempt==loginAttempt else{throw ClientError("Sign-in was canceled by a session lock.")}
  let credential=GoogleAuthProvider.credential(withIDToken:token,accessToken:result.user.accessToken.tokenString)
  let firebaseUser=try await Auth.auth().signIn(with:credential).user;guard attempt==loginAttempt else{try? Auth.auth().signOut();throw ClientError("Session locked.")};user=firebaseUser;busy=false
 }
 func reset(_ email:String)async throws {guard configured else{throw ClientError("Firebase is not configured.")};try await Auth.auth().sendPasswordReset(withEmail:email);notice="Password reset requested. Check your inbox."}
 func verify()async throws {guard let user else{return};try await sendVerificationEmail(user);notice="Verification email sent."}
 func refreshVerification()async throws {guard let user else{return};try await reloadUser(user);try await refreshIDToken(user);objectWillChange.send();notice=user.isEmailVerified ? "Email verified." : "Email is not verified yet."}
 private func sendVerificationEmail(_ user:User)async throws {try await withCheckedThrowingContinuation{(continuation:CheckedContinuation<Void,Error>) in user.sendEmailVerification{error in if let error{continuation.resume(throwing:error)}else{continuation.resume(returning:())}}}}
 private func reloadUser(_ user:User)async throws {try await withCheckedThrowingContinuation{(continuation:CheckedContinuation<Void,Error>) in user.reload{error in if let error{continuation.resume(throwing:error)}else{continuation.resume(returning:())}}}}
 private func refreshIDToken(_ user:User)async throws {try await withCheckedThrowingContinuation{(continuation:CheckedContinuation<Void,Error>) in user.getIDTokenForcingRefresh(true){_,error in if let error{continuation.resume(throwing:error)}else{continuation.resume(returning:())}}}}
 func unlock(_ password:String)async throws {
  guard let owner=user?.uid else{throw ClientError("Sign in first.")};let ticket=epoch
  let url=try Vault.location(owner),saved=try? Data(contentsOf:url)
  let result=try await Task.detached { ()throws->(CaseDocument,SymmetricKey,Data) in
   if let saved {return try Vault.open(JSONDecoder().decode(Envelope.self,from:saved),password:password,owner:owner)}
   let salt=try Vault.randomSalt();return(CaseDocument(),try Vault.derive(password,salt:salt),salt)
  }.value
  guard ticket==epoch,user?.uid==owner else{throw ClientError("Session changed. Vault remained locked.")}
  vaultKey=result.1;vaultSalt=result.2;document=result.0;try persist()
 }
 func persist()throws {guard let document,let key=vaultKey,let salt=vaultSalt,let uid=user?.uid else{return};try Vault.save(Vault.seal(document,key:key,salt:salt,owner:uid),owner:uid)}
 func edit(_ transform:(inout CaseDocument)->Void) {guard var copy=document else{return};transform(&copy);do{guard let key=vaultKey,let salt=vaultSalt,let uid=user?.uid else{return};try Vault.save(Vault.seal(copy,key:key,salt:salt,owner:uid),owner:uid);document=copy}catch{notice="Encrypted save failed: "+error.localizedDescription}}
 func importPDFs(_ urls:[URL])async throws {
  guard let original=user else{throw ClientError("Sign in first.")};guard original.isEmailVerified else{throw ClientError("Verify your email before importing reports.")};guard let domain,document != nil else{throw ClientError("Unlock a case first.")};let owner=original.uid;let ticket=epoch;var parsed:[Parsed]=[]
  for url in urls {progress="Reading \(url.lastPathComponent) on this device";let content=try await Task.detached{try PDFImporter.extract(url)}.value;guard ticket==epoch,user?.uid==owner else{return};parsed.append(try domain.parse(content.0,name:url.lastPathComponent,method:content.1))}
  guard ticket==epoch else{return};var copy=document!;copy.reports+=parsed.map{$0.report};copy.accounts+=parsed.flatMap{$0.accounts};copy.findings=try domain.analyze(copy);edit{$0=copy}
 }
 private func endpoint(_ path:String)throws->URL {
  guard let raw=Bundle.main.object(forInfoDictionaryKey:"CreditAgentEndpoint") as? String,var parts=URLComponents(string:raw),parts.scheme=="https",parts.host != nil,parts.user==nil,parts.password==nil,parts.query==nil,parts.fragment==nil,["","/","/credit-agent"].contains(parts.path) else{throw ClientError("The shared Android production gateway is not configured.")};parts.path=path;guard let url=parts.url else{throw ClientError("Invalid gateway.")};return url
 }
 func post(_ path:String,body:Any)async throws->[String:Any] {
  guard let original=user else{throw ClientError("Sign in first.")};let ticket=epoch,uid=original.uid
  let token=try await original.getIDToken();guard ticket==epoch,user?.uid==uid else{throw ClientError("Session changed.")}
  var request=URLRequest(url:try endpoint(path));request.httpMethod="POST";request.timeoutInterval=150;request.cachePolicy = .reloadIgnoringLocalCacheData;request.setValue("application/json",forHTTPHeaderField:"Content-Type");request.setValue("Bearer "+token,forHTTPHeaderField:"Authorization");request.httpBody=try JSONSerialization.data(withJSONObject:body)
  let config=URLSessionConfiguration.ephemeral;config.urlCache=nil;config.httpCookieStorage=nil
  let session=URLSession(configuration:config);defer{session.invalidateAndCancel()}
  let(data,response)=try await session.data(for:request);guard ticket==epoch,user?.uid==uid else{throw ClientError("Account changed. Result was discarded.")}
  guard data.count<=200000,let http=response as? HTTPURLResponse,let result=try JSONSerialization.jsonObject(with:data) as? [String:Any] else{throw ClientError("Invalid secure service response.")}
  guard (200..<300).contains(http.statusCode) else{throw ClientError(result["error"] as? String ?? "Secure service could not complete this request.")};return result
 }
 func accept(_ sharing:Bool)async throws {
  guard let user,user.isEmailVerified else{throw ClientError("Verify your email before accepting terms and enabling case tools.")};guard let domain else{throw ClientError("Shared domain unavailable.")};let ticket=epoch
  _ = try await post("/policy/accept",body:["termsAccepted":true,"aiSharing":sharing,"version":domain.policyVersion,"digest":domain.policyDigest,"choiceAt":Int(now())]);guard ticket==epoch else{return}
  edit{$0.policyAcceptance=Agreement(version:domain.policyVersion,digest:domain.policyDigest,aiSharing:sharing,acceptedAt:now());$0.onboardingComplete=true}
 }
 func ask(_ question:String,scope:String?=nil,conversation:Bool=true)async throws->String {
  guard let domain,let state=document,let user,user.isEmailVerified else{throw ClientError("Verify your email before asking AI.")}
  guard let agreement=state.policyAcceptance,agreement.aiSharing,agreement.version==domain.policyVersion,agreement.digest==domain.policyDigest else{throw ClientError("Accept current terms and redacted AI sharing first.")}
  let ticket=epoch;let history=conversation ? state.chat.filter{$0.caseScopeId==scope}.suffix(6).map{($0.fromUser ? "User: " : "Guide: ")+String($0.text.prefix(380))}.joined(separator:"\n") : ""
  let safe=try domain.string("redactText",[history+"\nCurrent question: "+question,4000])
  var body:[String:Any]=["clientRequestId":UUID().uuidString,"question":safe,"caseSnapshot":try domain.snapshot(state,scope:scope)];if let scope{body["caseScopeId"]=scope}
  let result=try await post("/credit-agent",body:body);guard ticket==epoch,let reply=result["answer"] as? String,reply.count<=16000 else{throw ClientError("AI response was incomplete or the account changed.")}
  answer=reply
  if conversation {let safePrompt=try domain.string("redactText",[question]);edit{$0.chat += [Chat(fromUser:true,text:safePrompt,caseScopeId:scope),Chat(fromUser:false,text:reply,caseScopeId:scope)];$0.chat=Array($0.chat.suffix(200))}}
  return reply
 }
 func exportVault()throws->Data {guard let doc=document,let key=vaultKey,let salt=vaultSalt,let uid=user?.uid else{throw ClientError("Unlock the vault first.")};return try JSONEncoder().encode(Vault.seal(doc,key:key,salt:salt,owner:uid))}
 func importVault(_ url:URL,password:String)async throws {
  guard let original=user else{throw ClientError("Sign in first.")};guard original.isEmailVerified else{throw ClientError("Verify your email before importing a case.")};let owner=original.uid;let ticket=epoch;let access=url.startAccessingSecurityScopedResource();defer{if access{url.stopAccessingSecurityScopedResource()}}
  guard (try url.resourceValues(forKeys:[.fileSizeKey]).fileSize ?? 0)<=28000000 else{throw ClientError("Vault exceeds the transfer limit.")}
  let data=try Data(contentsOf:url);let result=try await Task.detached{try Vault.open(JSONDecoder().decode(Envelope.self,from:data),password:password,owner:owner)}.value
  guard ticket==epoch,user?.uid==owner else{throw ClientError("Session changed.")};edit{$0=result.0}
 }
 func deleteAccount()async throws {guard let original=user else{return};let owner=original.uid;_ = try await post("/account/delete",body:[:]);try await original.delete();try? FileManager.default.removeItem(at:Vault.location(owner));lock()}
}
