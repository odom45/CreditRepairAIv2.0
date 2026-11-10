import SwiftUI
import GoogleSignIn
@main struct CreditRepairAppleApp:App {
 @StateObject private var store=SessionStore()
 @Environment(\.scenePhase) private var phase
 @State private var backgroundLockTask:Task<Void,Never>?
 var body:some Scene {WindowGroup{RootView(store:store).tint(.mint).onOpenURL{_ = GIDSignIn.sharedInstance.handle($0)}.onChange(of:phase){_,value in
  if value == .background && !store.isAuthenticatingGoogle {
   backgroundLockTask?.cancel()
   backgroundLockTask=Task { @MainActor in
    try? await Task.sleep(nanoseconds:300_000_000_000)
    guard !Task.isCancelled,phase == .background else{return}
    store.lock()
   }
  } else if value == .active {
   backgroundLockTask?.cancel();backgroundLockTask=nil
   if store.user?.isEmailVerified == false && !store.busy {store.run{try await store.refreshVerification()}}
  }
 }}}
}
struct RootView:View {
 @ObservedObject var store:SessionStore
 var body:some View {VStack(spacing:0){if !store.notice.isEmpty{Text(store.notice).font(.callout).padding().frame(maxWidth:.infinity).background(.orange.opacity(0.15))};if store.user==nil{LoginView(store:store)}else if store.user?.isEmailVerified != true{VerificationView(store:store)}else if store.document==nil{UnlockView(store:store)}else{PrivateWorkspace(store:store)}}.privacySensitive()}
}
struct VerificationView:View {
 @ObservedObject var store:SessionStore
 var body:some View {Form{Section("Verify your email before opening your case"){Text(store.user?.email ?? "");Text("Your encrypted case stays on this device. Verify the signed-in email, then refresh here before unlocking reports or using case tools.");Button("Resend verification email"){store.run{try await store.verify()}}.disabled(store.busy);Button("I verified my email — refresh"){store.run{try await store.refreshVerification()}}.disabled(store.busy);Button("Sign out"){store.lock()}}}.formStyle(.grouped).frame(minWidth:320)}
}
struct LoginView:View {
 @ObservedObject var store:SessionStore
 @State private var email=""
 @State private var password=""
 @State private var create=false
 var body:some View {Form{Section{Text("CreditRepairAI").font(.largeTitle.bold());Text("Understand your reports. Take the next step.").font(.title2);Text("Private on-device report review with the shared Android legal-source AI service.")};Section(create ? "Create account" : "Sign in"){TextField("Email",text:$email).textContentType(.username);SecureField("Password",text:$password).textContentType(create ? .newPassword : .password);Button(create ? "Create account" : "Sign in"){store.run{try await store.signIn(email,password:password,create:create);password=""}}.disabled(store.busy || !store.configured || email.isEmpty || password.isEmpty);Button("Continue with Google"){store.run{try await store.signInGoogle()}}.disabled(store.busy || !store.configured);Toggle("Create a new account",isOn:$create);Button("Reset password"){store.run{try await store.reset(email)}}.disabled(email.isEmpty || store.busy)};if !store.configured{Section("Configuration required"){Text("Include this target's registered GoogleService-Info.plist from the credit-repair-ai-v2 Firebase project. No placeholder login or sample account is substituted.")}};Section{Text("Fresh sign-in is required after closing the app. A five-minute background-return window supports email verification and official-portal handoffs; the session locks if the app remains backgrounded. Educational information only — not legal or financial advice. No score, deletion, approval or timing guarantees.").font(.footnote)}}.frame(minWidth:320).formStyle(.grouped)}
}
struct UnlockView:View {
 @ObservedObject var store:SessionStore
 @State private var password=""
 var body:some View {Form{Section("Unlock your private workspace"){Text(store.user?.email ?? "");Text("Your vault passphrase is separate from your account password. Keep it safe: it cannot be recovered. Use it with your encrypted transfer on other platforms.");SecureField("Vault passphrase (12+ characters)",text:$password);Button("Unlock or create vault"){store.run{try await store.unlock(password);password=""}}.disabled(password.count<12 || store.busy);Button("Sign out"){store.lock()}}}.formStyle(.grouped).frame(minWidth:320)}
}
