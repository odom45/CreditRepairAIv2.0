import Foundation
import CryptoKit
import CommonCrypto
struct Envelope:Codable { var format="creditrepairai-vault-v1"; var salt:String; var iv:String; var ciphertext:String }
enum Vault {
 static func derive(_ password:String,salt:Data)throws->SymmetricKey {
  guard password.count>=12,salt.count==16 else {throw ClientError("Use a vault passphrase with at least 12 characters.")}
  let bytes=Array(password.utf8);var key=Data(count:32)
  let status=key.withUnsafeMutableBytes { out in salt.withUnsafeBytes { saltBuffer in bytes.withUnsafeBytes { passwordBuffer in
   CCKeyDerivationPBKDF(CCPBKDFAlgorithm(kCCPBKDF2),passwordBuffer.bindMemory(to:Int8.self).baseAddress,bytes.count,saltBuffer.bindMemory(to:UInt8.self).baseAddress,salt.count,CCPseudoRandomAlgorithm(kCCPRFHmacAlgSHA256),310000,out.bindMemory(to:UInt8.self).baseAddress,32)
  } } }
  guard status==kCCSuccess else {throw ClientError("Vault key could not be derived.")};return SymmetricKey(data:key)
 }
 static func randomSalt()throws->Data { var bytes=[UInt8](repeating:0,count:16);guard SecRandomCopyBytes(kSecRandomDefault,16,&bytes)==errSecSuccess else {throw ClientError("Secure randomness unavailable.")};return Data(bytes) }
 static func seal(_ state:CaseDocument,key:SymmetricKey,salt:Data,owner:String)throws->Envelope {
  let data=try JSONEncoder().encode(state);guard data.count<=20_000_000 else {throw ClientError("Case exceeds 20 MB.")}
  let sealed=try AES.GCM.seal(data,using:key,authenticating:Data(("creditrepairai-v1:"+owner).utf8))
  return Envelope(salt:salt.base64EncodedString(),iv:Data(sealed.nonce).base64EncodedString(),ciphertext:(sealed.ciphertext+sealed.tag).base64EncodedString())
 }
 static func open(_ envelope:Envelope,password:String,owner:String)throws->(CaseDocument,SymmetricKey,Data) {
  guard envelope.format=="creditrepairai-vault-v1",envelope.ciphertext.count<=28_000_000,let salt=Data(base64Encoded:envelope.salt),salt.count==16,let iv=Data(base64Encoded:envelope.iv),iv.count==12,let cipher=Data(base64Encoded:envelope.ciphertext),cipher.count>=16 else {throw ClientError("Invalid encrypted vault.")}
  let key=try derive(password,salt:salt)
  let box=try AES.GCM.SealedBox(nonce:AES.GCM.Nonce(data:iv),ciphertext:cipher.dropLast(16),tag:cipher.suffix(16))
  let data=try AES.GCM.open(box,using:key,authenticating:Data(("creditrepairai-v1:"+owner).utf8))
  guard data.count<=20_000_000 else {throw ClientError("Case exceeds the safe size limit.")}
  var object=try JSONSerialization.jsonObject(with:data) as? [String:Any] ?? [:]
  // Android enum names are uppercase. Canonical wire bureau/portal values are lowercase.
  for name in ["accounts","reports","findings","disputes","filings","mailings","scores"] {
   if let rows=object[name] as? [[String:Any]] {object[name]=rows.map { row -> [String:Any] in var row=row;if let b=row["bureau"] as? String {row["bureau"]=b.lowercased()};if let p=row["portal"] as? String {row["portal"]=p.lowercased()};if let b=row["bureaus"] as? [String] {row["bureaus"]=b.map{$0.lowercased()}};return row}}
  }
  let state=try decode(object,as:CaseDocument.self)
  guard state.version==1,state.accounts.count<=10000,state.filings.allSatisfy({(0...6).contains($0.step)}),state.accounts.allSatisfy({$0.accountSuffix.count<=4}) else {throw ClientError("Unsupported or invalid case records.")}
  return (state,key,salt)
 }
 static func location(_ owner:String)throws->URL {
  let folder=try FileManager.default.url(for:.applicationSupportDirectory,in:.userDomainMask,appropriateFor:nil,create:true).appendingPathComponent("CreditRepairAI",isDirectory:true)
  try FileManager.default.createDirectory(at:folder,withIntermediateDirectories:true)
  let name=SHA256.hash(data:Data(owner.utf8)).map{String(format:"%02x",$0)}.joined()
  var url=folder.appendingPathComponent(name+".json");var values=URLResourceValues();values.isExcludedFromBackup=true;try? url.setResourceValues(values);return url
 }
 static func save(_ envelope:Envelope,owner:String)throws {
  let url=try location(owner);let data=try JSONEncoder().encode(envelope)
  #if os(iOS)
  try data.write(to:url,options:[.atomic,.completeFileProtection])
  #else
  try data.write(to:url,options:.atomic);try FileManager.default.setAttributes([.posixPermissions:0o600],ofItemAtPath:url.path)
  #endif
  var values=URLResourceValues();values.isExcludedFromBackup=true;var saved=url;try saved.setResourceValues(values)
 }
}
