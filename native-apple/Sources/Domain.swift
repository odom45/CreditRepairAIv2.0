import Foundation
import JavaScriptCore
final class Domain {
 private let context:JSContext
 init(bundle:Bundle = .main)throws {
  guard let context=JSContext(),let url=bundle.url(forResource:"domain",withExtension:"js") else {throw ClientError("Shared domain bundle is missing.")}
  self.context=context
  let uuid:@convention(block)()->String={UUID().uuidString}
  context.setObject(uuid,forKeyedSubscript:"nativeUUID" as NSString)
  context.evaluateScript("globalThis.crypto={randomUUID:()=>nativeUUID()};")
  context.evaluateScript(try String(contentsOf:url,encoding:.utf8))
  if let exception=context.exception {throw ClientError(exception.toString())}
 }
 func call(_ name:String,_ arguments:[Any])throws->JSValue {
  context.exception=nil
  guard let fn=context.objectForKeyedSubscript("CreditDomain")?.objectForKeyedSubscript(name),let result=fn.call(withArguments:arguments) else {throw ClientError("Shared domain operation is unavailable.")}
  if let exception=context.exception {throw ClientError(exception.toString())}
  return result
 }
 func object(_ name:String,_ arguments:[Any])throws->Any {guard let value=try call(name,arguments).toObject() else {throw ClientError("Invalid domain result.")};return value}
 func string(_ name:String,_ arguments:[Any])throws->String {try call(name,arguments).toString()}
 func parse(_ text:String,name:String,method:String)throws->Parsed {try decode(object("parseText",[text,name,method]),as:Parsed.self)}
 func analyze(_ document:CaseDocument)throws->[Finding] {try decode(object("analyze",[jsonObject(document.accounts),jsonObject(document.reports)]),as:[Finding].self)}
 func snapshot(_ document:CaseDocument,scope:String?)throws->Any {
  let args:[Any]=[try jsonObject(document),scope as Any? ?? NSNull()]
  return try object("sanitizeSnapshot",[object("snapshot",args)])
 }
 var policyVersion:String {context.objectForKeyedSubscript("CreditDomain").objectForKeyedSubscript("POLICY_VERSION").toString()}
 var policyDigest:String {context.objectForKeyedSubscript("CreditDomain").objectForKeyedSubscript("POLICY_DIGEST").toString()}
}
