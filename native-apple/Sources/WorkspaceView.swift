import SwiftUI
import UniformTypeIdentifiers
#if os(macOS)
import AppKit
#else
import UIKit
#endif
struct ExportDocument:FileDocument {
 static var readableContentTypes:[UTType]{[.data,.json,.pdf]}
 var data:Data
 init(_ data:Data){self.data=data}
 init(configuration:ReadConfiguration)throws{data=configuration.file.regularFileContents ?? Data()}
 func fileWrapper(configuration:WriteConfiguration)throws->FileWrapper{FileWrapper(regularFileWithContents:data)}
}
struct PrivateWorkspace:View {
 @ObservedObject var store:SessionStore
 @Environment(\.openURL) private var openURL
 @State private var panel:String?="Overview"
 @State private var showImport=false
 @State private var importingVault=false
 @State private var showExport=false
 @State private var export:ExportDocument?
 @State private var exportType:UTType = .json
 @State private var exportName="CreditRepairAI-encrypted-vault"
 @State private var vaultPassword=""
 @State private var terms=false
 @State private var sharing=false
 @State private var deleteConfirmation=""
 @State private var disputeID=""
 @State private var portal="equifax"
 @State private var reason=""
 @State private var findingID=""
 @State private var scope=""
 @State private var question=""
 @State private var response=""
 @State private var chunkLimit=1000
 @State private var sender=""
 @State private var address=""
 @State private var letterFacts=""
 @State private var evidence=""
 @State private var resolution=""
 @State private var letterBureau="equifax"
 @State private var tracking=""
 @State private var mailedDate=Date()
 @State private var score=""
 @State private var scoreBureau="equifax"
 @State private var pendingDraft:[String:String]?
 @State private var draftContext=""
 private let panels=["Overview","Reports","Analysis","Disputes","Guided filing","AI guide","Goals & scores","Account"]
 private var doc:CaseDocument{store.document ?? CaseDocument()}
 private var dispute:Dispute?{doc.disputes.first{$0.id==disputeID}}
 private var filing:Filing?{doc.filings.first{$0.disputeId==disputeID && $0.portal==portal}}
 private var currentAgreement:Bool{guard let domain=store.domain,let agreement=doc.policyAcceptance else{return false};return agreement.version==domain.policyVersion && agreement.digest==domain.policyDigest}
 var body:some View {
  NavigationSplitView{List(panels,id:\.self,selection:$panel){Text($0)}.navigationTitle("CreditRepairAI")}detail:{ScrollView{VStack(alignment:.leading,spacing:22){header;if store.user?.isEmailVerified != true{verificationCard}else{if !currentAgreement{agreementCard};if !store.progress.isEmpty{ProgressView(store.progress)};content;if !store.answer.isEmpty{card("AI explanation"){Text(store.answer).textSelection(.enabled)}}}}.padding(24).frame(maxWidth:1100,alignment:.leading)}.navigationTitle(panel ?? "Overview")}
  .fileImporter(isPresented:$showImport,allowedContentTypes:importingVault ? [.json] : [.pdf],allowsMultipleSelection:!importingVault){result in switch result{case .success(let urls):if importingVault{guard let url=urls.first else{return};store.run{try await store.importVault(url,password:vaultPassword);vaultPassword=""}}else{store.run{try await store.importPDFs(urls)}};case .failure(let error):store.notice=error.localizedDescription}}
  .fileExporter(isPresented:$showExport,document:export,contentType:exportType,defaultFilename:exportName){result in if case .failure(let error)=result{store.notice=error.localizedDescription}}
  .onChange(of:panel){_,_ in store.answer="";pendingDraft=nil}
 }
 private var header:some View {HStack{VStack(alignment:.leading){Text("YOUR NEXT STEP, CLEARER").font(.caption).foregroundStyle(.mint);Text(panel ?? "Overview").font(.largeTitle.bold());Text(store.user?.email ?? "").font(.caption).foregroundStyle(.secondary)};Spacer();Button("Lock & sign out"){store.lock()}}}
 private func card<Content:View>(_ title:String,@ViewBuilder content:()->Content)->some View {GroupBox{VStack(alignment:.leading,spacing:14){content()}.frame(maxWidth:.infinity,alignment:.leading).padding(8)}label:{Text(title).font(.headline)}}
 private func field(_ title:String,_ value:Binding<String>)->some View {VStack(alignment:.leading){Text(title).font(.caption).foregroundStyle(.secondary);TextField(title,text:value,axis:.vertical).textFieldStyle(.roundedBorder).lineLimit(2...6)}}
 private func copy(_ text:String){
  #if os(macOS)
  NSPasteboard.general.clearContents();NSPasteboard.general.setString(text,forType:.string)
  #else
  UIPasteboard.general.string=text
  #endif
 }
 private func exportData(_ data:Data,name:String,type:UTType){export=ExportDocument(data);exportName=name;exportType=type;showExport=true}
 private var agreementCard:some View {card("Before you begin"){Text("Read the current terms, privacy policy and disclosure before accepting. AI sharing is optional.");policyLinks;Toggle("I have read and accept the current terms and disclosure.",isOn:$terms);Toggle("Allow sending a redacted case snapshot to the secure AI service.",isOn:$sharing);Button("Save agreement preferences"){store.run{try await store.accept(sharing)}}.disabled(!terms || store.busy)}}
 private var verificationCard:some View {card("Verify your email to unlock your case"){Text("Your reports and case tools stay encrypted on this device. Verify the signed-in email to continue to report import, disputes, and AI.");Button("Resend verification"){store.run{try await store.verify()}}.disabled(store.busy);Button("I verified my email — refresh"){store.run{try await store.refreshVerification()}}.disabled(store.busy)}}
 private var policyLinks:some View {HStack{Button("Terms"){policyURL("terms.html")};Button("Privacy"){policyURL("privacy.html")};Button("Disclosure"){policyURL("disclaimer.html")}}}
 private func policyURL(_ name:String){guard let raw=Bundle.main.object(forInfoDictionaryKey:"CreditAgentEndpoint") as? String,var parts=URLComponents(string:raw),parts.scheme=="https" else{store.notice="Configure the shared production gateway to view its current policies.";return};parts.path="/legal/"+name;if let url=parts.url{openURL(url)}}
 @ViewBuilder private var content:some View {
  switch panel ?? "Overview" {
  case "Reports":reports
  case "Analysis":analysis
  case "Disputes":disputes
  case "Guided filing":guidance
  case "AI guide":chat
  case "Goals & scores":goals
  case "Account":account
  default:overview
  }
 }
 private var overview:some View {VStack(alignment:.leading,spacing:20){card("Your case"){Text("\(doc.reports.count) reports · \(doc.accounts.count) accounts · \(doc.findings.count) items to verify · \(doc.disputes.count) disputes").font(.title3);Text("1. Obtain original reports. 2. Import and verify extraction. 3. Gather supporting records. 4. Prepare a reviewed dispute. 5. Track actual correspondence.");Link("Get your reports at AnnualCreditReport.com",destination:URL(string:"https://www.annualcreditreport.com/")!);Button("Import reports"){importingVault=false;showImport=true}};card("Three-bureau coverage"){ForEach(bureaus,id:\.self){b in Text("\(b.capitalized): \(doc.reports.contains{$0.bureau==b} ? "Imported" : "Needed")")};Text("Reports may not contain scores. A comparison flag is not proof of an error.").font(.footnote)};card("Follow-up check-ins"){ForEach(doc.filings.filter{$0.submittedAt != nil && $0.responseReceivedAt==nil}){f in Text("\(f.portal.capitalized): check correspondence \(dateText(f.submittedAt!+30*day)) and \(dateText(f.submittedAt!+45*day)).")};Text("These are recorded check-in dates, not promised response times or universal legal deadlines.").font(.footnote)}}}
 private var reports:some View {VStack(alignment:.leading,spacing:20){card("Import original PDF reports"){Text("PDFKit extraction and Vision OCR run on this device. No original PDF is uploaded. Limits: 50 MB, 200 embedded-text pages, or 30 scanned pages.");Button("Choose PDF reports"){importingVault=false;showImport=true}.disabled(store.busy)};ForEach(doc.reports){report in card(report.fileName){Text("\(report.bureau.capitalized) · \(report.accountCount) accounts · score \(report.score.map(String.init) ?? "not shown") · \(report.extractionMethod)")}};ForEach(doc.accounts){a in card(a.creditor){Text("\(a.bureau.capitalized) · ••••\(a.accountSuffix)");TextField("Creditor",text:accountText(a.id,\.creditor));TextField("Status",text:accountText(a.id,\.status));TextField("Balance",text:Binding(get:{store.document?.accounts.first{$0.id==a.id}?.balance.map(String.init) ?? ""},set:{value in store.edit{s in if let i=s.accounts.firstIndex(where:{$0.id==a.id}){s.accounts[i].balance=Int(value);if let findings=try? store.domain?.analyze(s){s.findings=findings}}}}));Text("Verify every extracted field against the dated original before drafting.").font(.footnote)}}}}
 private func accountText(_ id:String,_ path:WritableKeyPath<Account,String>)->Binding<String>{Binding(get:{store.document?.accounts.first{$0.id==id}?[keyPath:path] ?? ""},set:{value in store.edit{s in if let i=s.accounts.firstIndex(where:{$0.id==id}){s.accounts[i][keyPath:path]=String(value.prefix(64));if let findings=try? store.domain?.analyze(s){s.findings=findings}}}})}
 private var analysis:some View {VStack(alignment:.leading,spacing:20){if doc.findings.isEmpty{Text("No comparison flags yet. Import reports and verify extraction; absence of a flag does not establish accuracy.")};ForEach(doc.findings){f in card(f.title){Text("\(f.severity) · \(f.creditor)");Text(f.detail);Text(f.legalBasis.joined(separator:" · ")).font(.caption);Button("Prepare supported draft"){findingID=f.id;reason=""}}};if let finding=doc.findings.first(where:{$0.id==findingID}){card("Verified factual issue"){field("Describe the specific issue supported by your records",$reason);Button("Create draft"){store.edit{$0.disputes.append(Dispute(findingId:finding.id,creditor:finding.creditor,bureaus:finding.bureaus,reason:reason))};findingID="";reason="";panel="Disputes"}.disabled(reason.trimmingCharacters(in:.whitespacesAndNewlines).isEmpty)}}}}
 private var disputes:some View {VStack(alignment:.leading,spacing:20){if doc.disputes.isEmpty{Text("Review analysis and verify a factual issue before creating a dispute.")};ForEach(doc.disputes){d in card(d.creditor){Text(d.reason);Text("\(d.status) · sent \(dateText(d.sentAt))").font(.caption);ForEach(d.bureaus.filter{$0 != "unknown"},id:\.self){b in Button("\(b.capitalized) walkthrough"){selectFiling(d,b)}};Button("CFPB walkthrough"){selectFiling(d,"cfpb")}.disabled(!isCFPBReady(d));Button("Prepare physical letter"){disputeID=d.id;letterBureau=d.bureaus.first(where:{$0 != "unknown"}) ?? "equifax";letterFacts=d.reason;panel="Guided filing"};Picker("Status",selection:Binding(get:{store.document?.disputes.first{$0.id==d.id}?.status ?? "DRAFT"},set:{status in store.edit{s in if let i=s.disputes.firstIndex(where:{$0.id==d.id}){s.disputes[i].status=status}}})){ForEach(["DRAFT","READY","SENT","INVESTIGATING","RESOLVED"],id:\.self){Text($0)}}}}}}
 private func isCFPBReady(_ d:Dispute)->Bool{(try? store.domain?.call("cfpbReady",[jsonObject(d),jsonObject(doc.filings),jsonObject(doc.mailings),now()]).toBool()) ?? false}
 private func selectFiling(_ d:Dispute,_ p:String){guard p != "cfpb" || isCFPBReady(d) else{return};disputeID=d.id;portal=p;panel="Guided filing";store.answer="";if !doc.filings.contains(where:{$0.disputeId==d.id && $0.portal==p}){store.edit{$0.filings.append(Filing(disputeId:d.id,portal:p,problem:d.reason))}}}
 private func filingText(_ path:WritableKeyPath<Filing,String>)->Binding<String>{Binding(get:{filing?[keyPath:path] ?? ""},set:{value in editFiling{f in f[keyPath:path]=String(value.prefix(2000));f.reviewed=false}})}
 private func editFiling(_ transform:(inout Filing)->Void){guard let id=filing?.id else{return};store.edit{s in if let i=s.filings.firstIndex(where:{$0.id==id}){transform(&s.filings[i])}}}
 private var guidance:some View {VStack(alignment:.leading,spacing:20){card("Select a saved dispute"){Picker("Dispute",selection:$disputeID){Text("Choose a dispute").tag("");ForEach(doc.disputes){Text($0.creditor).tag($0.id)}};if let d=dispute{ForEach(d.bureaus.filter{$0 != "unknown"},id:\.self){b in Button("Open \(b.capitalized) workflow"){selectFiling(d,b)}};Button("CFPB workflow"){selectFiling(d,"cfpb")}.disabled(!isCFPBReady(d))}};if let d=dispute,let f=filing{filingCard(d,f)};if let d=dispute{letterCard(d)}}}
 private func filingCard(_ d:Dispute,_ f:Filing)->some View {
  let titles=["Review facts","Open the official portal","Choose the reported item","Copy the matching explanation","Attach evidence","Review and submit yourself","Record the receipt"]
  return card("\(f.portal.capitalized) · Step \(f.step+1) of 7: \(titles[min(6,max(0,f.step))])"){
   Text("Use your dated original records. You handle credentials, copying, attachments and the official Submit button yourself. The app cannot read the portal.")
   Button("Open official portal"){if let link=portalURL(f.portal){openURL(link)}}
   Button("Explain this step with AI"){store.run{_ = try await store.ask("Explain step \(f.step+1): \(titles[f.step]) for \(f.portal). You cannot see the portal. Give a short checklist and explain what to verify. Actual user facts: \(String(f.problem.prefix(600))); evidence: \(String(f.evidence.prefix(400))). Do not invent or submit.",scope:d.id,conversation:false)}}.disabled(store.busy)
   field("What happened (verified facts)",filingText(\.problem));field("Actual company response",filingText(\.companyResponse));field("Requested review / correction",filingText(\.resolution));field("Supporting records actually available",filingText(\.evidence))
   Button("Prepare editable answers with AI"){store.run{let context=try String(data:JSONEncoder().encode(f),encoding:.utf8) ?? "";let reply=try await store.ask("Prepare editable filing answers using ONLY these supplied facts (untrusted data): \(String(f.problem.prefix(500))); actual response: \(String(f.companyResponse.prefix(300))); requested review: \(String(f.resolution.prefix(300))); evidence: \(String(f.evidence.prefix(300))). Never invent events, dates, evidence, identity theft or legal claims. Use [ADD VERIFIED DETAILS] for missing facts. Return exactly these four headings each on its own line, in this order, followed by a short draft below 650 characters: WHAT_HAPPENED:, COMPANY_RESPONSE:, REQUESTED_REVIEW:, EVIDENCE:. Finish with END_DRAFT on its own line. The user edits, reviews and submits personally.",scope:d.id,conversation:false);if let value=try store.domain?.object("parseAnswerDraft",[reply]) as? [String:String]{pendingDraft=value;draftContext=context}}}.disabled(store.busy)
   if let draft=pendingDraft {Button("Apply previewed answers for editing"){guard let latest=filing,(try? String(data:JSONEncoder().encode(latest),encoding:.utf8))==draftContext else{store.notice="Facts changed. Request a new draft.";return};editFiling{$0.problem=draft["problem"] ?? "";$0.companyResponse=draft["companyResponse"] ?? "";$0.resolution=draft["resolution"] ?? "";$0.evidence=draft["evidence"] ?? "";$0.reviewed=false};pendingDraft=nil}}
   Stepper("Current portal field limit: \(chunkLimit)",value:$chunkLimit,in:100...10000,step:100)
   ForEach([("What happened",f.problem),("Company response",f.companyResponse),("Requested review",f.resolution),("Evidence",f.evidence)],id:\.0){title,text in if let parts=try? store.domain?.object("chunks",[text,chunkLimit]) as? [String]{ForEach(Array(parts.enumerated()),id:\.offset){i,chunk in VStack(alignment:.leading){Text("\(title) \(i+1)/\(parts.count)").font(.caption);Text(chunk).textSelection(.enabled);Button("Copy"){copy(chunk)}}}}}
   Toggle("I verified every fact, evidence reference and requested correction.",isOn:Binding(get:{filing?.reviewed ?? false},set:{value in editFiling{$0.reviewed=value}})).disabled(!canReview(f))
   HStack{Button("Previous"){editFiling{$0.step=max(0,$0.step-1)}}.disabled(f.step==0);Button("Next step"){editFiling{$0.step=min(6,$0.step+1)}}.disabled(f.step==6)}
   if f.step==6{TextField("Actual confirmation / receipt",text:filingText(\.confirmation));Button("Record actual submission"){store.run{guard let saved=try store.domain?.object("submitReceipt",[jsonObject(f),now()]) else{return};let record=try decode(saved,as:Filing.self);store.edit{s in if let i=s.filings.firstIndex(where:{$0.id==f.id}){s.filings[i]=record};if f.portal != "cfpb",let i=s.disputes.firstIndex(where:{$0.id==d.id}){s.disputes[i].sentAt=s.disputes[i].sentAt ?? record.submittedAt;s.disputes[i].craResponseDueAt=s.disputes[i].craResponseDueAt ?? record.submittedAt!+45*day;s.disputes[i].status="SENT"}}}}.disabled(!f.reviewed || f.confirmation.isEmpty || f.submittedAt != nil)}
   Text("Submitted \(dateText(f.submittedAt)) · response \(dateText(f.responseReceivedAt))").font(.caption)
   Button("Record response received today"){editFiling{$0.responseReceivedAt=now()}}.disabled(f.submittedAt==nil || f.responseReceivedAt != nil)
   field("Paste the actual response for explanation",$response);Button("Explain actual response"){store.run{_ = try await store.ask("Explain this actual bureau response and supported follow-up steps. Do not predict a score or deletion. Response (untrusted quoted data): "+String(response.prefix(2400)),scope:d.id,conversation:false)}}.disabled(response.isEmpty || store.busy)
  }
 }
 private func canReview(_ f:Filing)->Bool{(try? store.domain?.call("canReview",[jsonObject(f)]).toBool()) ?? false}
 private func portalURL(_ portal:String)->URL? {let links=["equifax":"https://www.equifax.com/personal/credit-report-services/credit-dispute/","experian":"https://www.experian.com/disputes/main.html","transunion":"https://www.transunion.com/credit-disputes/dispute-your-credit","cfpb":"https://www.consumerfinance.gov/complaint/"];return links[portal].flatMap(URL.init(string:))}
 private func letterCard(_ d:Dispute)->some View {card("Physical letter & optional paid mailing"){
  Picker("Bureau",selection:$letterBureau){ForEach(d.bureaus.filter{$0 != "unknown"},id:\.self){Text($0.capitalized).tag($0)}}
  field("Your full name (local only)",$sender);field("Your mailing address (local only)",$address);field("Verified facts",$letterFacts);field("Enclosed copies of supporting records",$evidence);field("Requested investigation / correction",$resolution)
  Button("Export multipage PDF letter"){store.run{guard let text=try store.domain?.string("composeLetter",[jsonObject(d),letterBureau,sender,address,letterFacts,evidence,resolution,DateFormatter.localizedString(from:Date(),dateStyle:.medium,timeStyle:.none)]) else{return};exportData(try PDFImporter.letter(text),name:"CreditRepairAI-\(letterBureau)-dispute",type:.pdf)}}.disabled(store.busy)
  Text("Verify the bureau's current mailing address and identification requirements before sending. Export and review the letter, then upload and pay on LetterStream's website if you choose paid mailing. The provider sets the fee; this app does not place or pay for an order.").font(.footnote)
  Link("Open LetterStream",destination:URL(string:"https://www.letterstream.com/")!)
  DatePicker("Actual date mailed",selection:$mailedDate,in:...Date(),displayedComponents:.date);TextField("Tracking / provider receipt",text:$tracking)
  Button("Record actual mailing"){store.edit{$0.mailings.append(Mailing(disputeId:d.id,bureau:letterBureau,mailedAt:(mailedDate.timeIntervalSince1970*1000).rounded(.down),trackingNumber:tracking))};tracking=""}.disabled(tracking.isEmpty)
  ForEach(Array(doc.mailings.enumerated()).filter{$0.element.disputeId==d.id},id:\.offset){i,m in VStack(alignment:.leading){Text("\(m.bureau.capitalized): mailed \(dateText(m.mailedAt)), delivered \(dateText(m.deliveredAt)), response \(dateText(m.responseReceivedAt))");Text(m.trackingNumber).font(.caption);Button("Record delivery today"){store.edit{$0.mailings[i].deliveredAt=now()}}.disabled(m.deliveredAt != nil);Button("Record response today"){store.edit{$0.mailings[i].responseReceivedAt=now()}}.disabled(m.responseReceivedAt != nil)}}
 }}
 private var chat:some View {card("Your interactive case guide"){Picker("Case scope",selection:$scope){Text("Whole case").tag("");ForEach(doc.disputes){Text($0.creditor).tag($0.id)}};ForEach(doc.chat.filter{$0.caseScopeId==(scope.isEmpty ? nil : scope)}){m in VStack(alignment:.leading){Text(m.fromUser ? "You" : "AI guide").font(.headline);Text(m.text).textSelection(.enabled)}.padding().background(m.fromUser ? Color.blue.opacity(0.08) : Color.mint.opacity(0.08)).clipShape(RoundedRectangle(cornerRadius:10))};field("Your question",$question);Button("Ask the guide"){let prompt=String(question.prefix(1400));store.run{_ = try await store.ask(prompt,scope:scope.isEmpty ? nil : scope);question=""}}.disabled(question.count<2 || store.busy);ForEach(["What should I verify first?","Help me prepare supporting evidence.","Explain my CFPB readiness."],id:\.self){prompt in Button(prompt){store.run{_ = try await store.ask(prompt,scope:scope.isEmpty ? nil : scope)}}.disabled(store.busy)}}}
 private var goals:some View {VStack(alignment:.leading,spacing:20){card("Your goal"){field("What are you working toward?",Binding(get:{doc.goal.description},set:{value in store.edit{$0.goal.description=String(value.prefix(1500));$0.goal.aiPlan=""}}));Stepper("Personal target: \(doc.goal.targetMonths) months (not a forecast)",value:Binding(get:{doc.goal.targetMonths},set:{value in store.edit{$0.goal.targetMonths=value}}),in:1...120);Button("Build and save an AI plan"){store.run{let plan=try await store.ask("Build a prioritized educational plan for this goal: \(doc.goal.description). The user's \(doc.goal.targetMonths)-month target is a preference, not a forecast. Use actual findings and dispute history; identify evidence gaps. Do not predict approvals, score changes, deletion or completion.",conversation:false);store.edit{$0.goal.aiPlan=plan}}}.disabled(doc.goal.description.isEmpty || store.busy);Text(doc.goal.aiPlan).textSelection(.enabled)};card("Manual score history"){Picker("Bureau",selection:$scoreBureau){ForEach(bureaus,id:\.self){Text($0.capitalized).tag($0)}};TextField("Score (300–850)",text:$score);Button("Record dated score"){if let value=Int(score),(300...850).contains(value){store.edit{$0.scores.append(Score(bureau:scoreBureau,score:value))};score=""}};ForEach(Array(doc.scores.enumerated()),id:\.offset){_,s in Text("\(s.bureau.capitalized) · \(s.score) · \(dateText(s.recordedAt))")};Text("A score change does not establish that a dispute caused it.").font(.caption)};card("Freeze checklist"){ForEach(bureaus,id:\.self){b in Toggle("I completed the \(b.capitalized) freeze myself.",isOn:Binding(get:{doc.frozenBureaus.contains(b)},set:{value in store.edit{s in s.frozenBureaus.removeAll{$0==b};if value{s.frozenBureaus.append(b)}}}))};Link("Official identity theft guidance",destination:URL(string:"https://www.identitytheft.gov/")!);Text("These are your records of actions, not bureau confirmation.").font(.caption)}}}
 private var account:some View {VStack(alignment:.leading,spacing:20){card("Account & privacy"){Text(store.user?.email ?? "");TextField("State/DC code (optional)",text:Binding(get:{doc.jurisdictionCode},set:{value in store.edit{$0.jurisdictionCode=String(value.uppercased().filter{$0.isLetter}.prefix(2))}}));Toggle("Allow redacted AI sharing",isOn:Binding(get:{doc.policyAcceptance?.aiSharing ?? false},set:{value in store.run{try await store.accept(value)}})).disabled(!currentAgreement || store.busy);policyLinks};card("Encrypted case transfer"){Text("Export and import using the same Firebase account and vault passphrase. This is an explicit encrypted transfer, not automatic cloud case synchronization. Use the same account and passphrase on each supported client.");Button("Export encrypted vault"){store.run{exportData(try store.exportVault(),name:"CreditRepairAI-encrypted-vault",type:.json)}};SecureField("Import vault passphrase",text:$vaultPassword);Button("Import encrypted vault"){importingVault=true;showImport=true}.disabled(vaultPassword.count<12 || store.busy)};card("Shared subscription access"){Text("Existing Android access is enforced by the same backend Firebase identity. New Apple checkout stays disabled in this test candidate.");Button("Check shared access"){store.run{let result=try await store.post("/billing/status",body:[:]);store.notice=(result["active"] as? Bool)==true ? "Verified subscription is active." : "No active subscription was returned. Scoped dispute access is checked with the AI request."}}};card("Delete account"){Text("Delete backend billing/consent records, Firebase login and this device's encrypted case. Reauthenticate if requested. This does not cancel a Google Play subscription. Export first if needed.");TextField("Type DELETE to confirm",text:$deleteConfirmation);Button("Delete account",role:.destructive){store.run{try await store.deleteAccount()}}.disabled(deleteConfirmation != "DELETE" || store.busy)}}}
 private func dateText(_ milliseconds:Double?)->String{guard let milliseconds else{return "not recorded"};return DateFormatter.localizedString(from:Date(timeIntervalSince1970:milliseconds/1000),dateStyle:.medium,timeStyle:.none)}
}
