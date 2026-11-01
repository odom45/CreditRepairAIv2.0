import XCTest
@testable import CreditRepairAI
final class DomainTests:XCTestCase {
 func testAndroidCorpusParity()throws {
  let bundle=Bundle(for:Self.self),domain=try Domain(bundle:bundle)
  guard let folder=bundle.resourceURL?.appendingPathComponent("Fixtures/corpus") else{throw ClientError("Fixtures missing")}
  let manifest=try String(contentsOf:folder.appendingPathComponent("manifest.tsv"),encoding:.utf8)
  for row in manifest.split(separator:"\n").filter({!$0.hasPrefix("#")}) {
   let fields=row.split(separator:"\t").map(String.init),name=fields[0]
   let text=try String(contentsOf:folder.appendingPathComponent(name),encoding:.utf8)
   let parsed=try domain.parse(text,name:name,method:"test")
   XCTAssertEqual(parsed.report.bureau,fields[1].lowercased(),name)
   XCTAssertEqual(parsed.report.score,Int(fields[2]),name)
   XCTAssertEqual(parsed.accounts.map{$0.accountSuffix},fields[3]=="-" ? [] : fields[3].split(separator:",").map(String.init),name)
   XCTAssertEqual(parsed.accounts.compactMap{$0.balance},fields[4]=="-" ? [] : fields[4].split(separator:",").compactMap{Int($0)},name)
  }
 }
 func testMutatedTextAndBounds()throws {
  let domain=try Domain(bundle:Bundle(for:Self.self));let text="Experian\nCreditor: Fictional Cedar Bank\nAccount Number: XXXX1234\nStatus: Open\nBalance: 1280\nDate Opened: 01/2020\n"
  var seed:UInt32=0x6319
  for _ in 0..<2400 {seed=1664525 &* seed &+ 1013904223;var characters=Array(text);characters[Int(seed)%characters.count]="|";let parsed=try domain.parse(String(characters),name:"synthetic.txt",method:"test");XCTAssertLessThanOrEqual(parsed.accounts.count,150);XCTAssertTrue(parsed.accounts.allSatisfy{$0.accountSuffix.count<=4})}
  XCTAssertThrowsError(try domain.parse(String(repeating:"a",count:4097),name:"too-long",method:"test"))
 }
 func testVaultInteroperabilityAndTampering()throws {
  let bundle=Bundle(for:Self.self);let url=try XCTUnwrap(bundle.resourceURL?.appendingPathComponent("Fixtures/web-vault.json"))
  let envelope=try JSONDecoder().decode(Envelope.self,from:Data(contentsOf:url))
  let result=try Vault.open(envelope,password:"synthetic interoperability passphrase",owner:"interop-synthetic-owner")
  XCTAssertEqual(result.0.goal.description,"Synthetic shared case")
  XCTAssertThrowsError(try Vault.open(envelope,password:"wrong passphrase here",owner:"interop-synthetic-owner"))
  XCTAssertThrowsError(try Vault.open(envelope,password:"synthetic interoperability passphrase",owner:"other-owner"))
  let resealed=try Vault.seal(result.0,key:result.1,salt:result.2,owner:"interop-synthetic-owner")
  XCTAssertNotEqual(resealed.iv,envelope.iv)
  XCTAssertEqual(try Vault.open(resealed,password:"synthetic interoperability passphrase",owner:"interop-synthetic-owner").0.goal.description,"Synthetic shared case")
 }
 func testLocalPDFExtractionAndMalformedInput()throws {
  let text="Equifax SYNTHETIC CREDIT REPORT\nInvented records only.\nCredit Score: 640\nCreditor: Fictional Cedar Bank\nAccount Number: XXXX1234\nStatus: Open\nBalance: 1280\nDate Opened: 01/2020\n"
  let data=try PDFImporter.letter(text),url=FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString+".pdf");defer{try? FileManager.default.removeItem(at:url)};try data.write(to:url)
  let extracted=try PDFImporter.extract(url),domain=try Domain(bundle:Bundle(for:Self.self)),parsed=try domain.parse(extracted.0,name:"equifax.pdf",method:extracted.1)
  XCTAssertEqual(parsed.accounts.first?.balance,1280)
  try Data("not PDF".utf8).write(to:url);XCTAssertThrowsError(try PDFImporter.extract(url))
 }
}
