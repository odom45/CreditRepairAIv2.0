import Foundation
struct Report: Codable, Identifiable { var id:String; var fileName:String; var bureau:String; var score:Int?; var accountCount:Int; var importedAt:Double; var extractionMethod:String }
struct Account: Codable, Identifiable { var id:String; var creditor:String; var accountSuffix:String; var bureau:String; var status:String; var balance:Int?; var paymentStatus:String; var openedDate:String; var accountType:String; var responsibility:String; var dateReported:String; var originalCreditor:String; var pastDue:Int?; var creditLimit:Int?; var highBalance:Int?; var remarks:String }
struct Finding: Codable, Identifiable { var id:String; var type:String; var title:String; var detail:String; var severity:String; var creditor:String; var bureaus:[String]; var accountIds:[String]; var legalBasis:[String] }
struct Dispute: Codable, Identifiable { var id=UUID().uuidString; var findingId:String; var creditor:String; var bureaus:[String]; var reason:String; var letter=""; var status="DRAFT"; var createdAt=now(); var sentAt:Double?; var craResponseDueAt:Double? }
struct Filing: Codable, Identifiable { var id=UUID().uuidString; var disputeId:String; var portal:String; var step=0; var reviewed=false; var problem=""; var companyResponse=""; var resolution=""; var evidence=""; var confirmation=""; var submittedAt:Double?; var responseReceivedAt:Double? }
struct Mailing: Codable { var disputeId:String; var bureau:String; var mailedAt:Double; var trackingNumber:String; var deliveredAt:Double?; var responseReceivedAt:Double? }
struct Score: Codable { var bureau:String; var score:Int; var recordedAt=now() }
struct Chat: Codable, Identifiable { var id=UUID().uuidString; var fromUser:Bool; var text:String; var caseScopeId:String?; var createdAt=now() }
struct Goal: Codable { var description=""; var targetMonths=12; var aiPlan="" }
struct Agreement: Codable { var version:String; var digest:String; var aiSharing:Bool; var acceptedAt:Double }
struct CaseDocument: Codable { var version=1; var jurisdictionCode=""; var onboardingComplete=false; var reports:[Report]=[]; var accounts:[Account]=[]; var findings:[Finding]=[]; var disputes:[Dispute]=[]; var filings:[Filing]=[]; var mailings:[Mailing]=[]; var scores:[Score]=[]; var chat:[Chat]=[]; var goal=Goal(); var frozenBureaus:[String]=[]; var policyAcceptance:Agreement? }
struct Parsed: Codable { var report:Report; var accounts:[Account] }
func now()->Double { (Date().timeIntervalSince1970*1000).rounded(.down) }
let bureaus=["equifax","experian","transunion"]
let day:Double=86_400_000
struct ClientError: LocalizedError { let message:String; var errorDescription:String? {message}; init(_ message:String){self.message=message} }
func jsonObject<T:Encodable>(_ value:T)throws->Any { try JSONSerialization.jsonObject(with: JSONEncoder().encode(value)) }
func decode<T:Decodable>(_ value:Any,as:T.Type)throws->T { try JSONDecoder().decode(T.self,from:JSONSerialization.data(withJSONObject:value,options:[.fragmentsAllowed])) }
