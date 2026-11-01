import Foundation
import PDFKit
import Vision
import CoreGraphics
import CoreText
#if os(macOS)
import AppKit
#else
import UIKit
#endif
final class PDFImporter {
 static func extract(_ url:URL)throws->(String,String) {
  let access=url.startAccessingSecurityScopedResource();defer{if access{url.stopAccessingSecurityScopedResource()}}
  let size=try url.resourceValues(forKeys:[.fileSizeKey]).fileSize ?? 0
  guard size>4,size<=50_000_000 else{throw ClientError("Choose a PDF smaller than 50 MB.")}
  let bytes=try Data(contentsOf:url,options:.mappedIfSafe)
  guard bytes.prefix(5)==Data("%PDF-".utf8),let document=PDFDocument(data:bytes),document.pageCount<=200 else{throw ClientError("Invalid PDF or more than 200 pages.")}
  let deadline=Date().addingTimeInterval(180);var text=""
  for i in 0..<document.pageCount {guard Date()<deadline else{throw ClientError("PDF processing timed out.")};text+=(document.page(at:i)?.string ?? "")+"\n";if text.count>1_000_000{throw ClientError("PDF text exceeds the safe size limit.")}}
  if text.filter({!$0.isWhitespace}).count>=200{return(text,"Embedded PDF text")}
  guard document.pageCount<=30 else{throw ClientError("Scanned PDF exceeds 30 pages; no pages were silently skipped.")};text=""
  for i in 0..<document.pageCount {
   guard Date()<deadline,let page=document.page(at:i) else{throw ClientError("Scanned PDF processing timed out.")}
   let bounds=page.bounds(for:.mediaBox),scale=2048/bounds.width,height=Int(ceil(bounds.height*scale))
   guard bounds.width>0,height>0,height<=4096,let context=CGContext(data:nil,width:2048,height:height,bitsPerComponent:8,bytesPerRow:0,space:CGColorSpaceCreateDeviceRGB(),bitmapInfo:CGImageAlphaInfo.premultipliedLast.rawValue) else{throw ClientError("Page exceeds safe OCR dimensions.")}
   context.setFillColor(CGColor(gray:1,alpha:1));context.fill(CGRect(x:0,y:0,width:2048,height:height));context.scaleBy(x:scale,y:scale);context.translateBy(x:-bounds.origin.x,y:-bounds.origin.y);page.draw(with:.mediaBox,to:context)
   guard let image=context.makeImage() else{throw ClientError("Could not render OCR page.")}
   let request=VNRecognizeTextRequest();request.recognitionLevel = .accurate;request.recognitionLanguages=["en-US"];request.usesLanguageCorrection=false
   try VNImageRequestHandler(cgImage:image,options:[:]).perform([request])
   let rows=(request.results ?? []).sorted { a,b in abs(a.boundingBox.midY-b.boundingBox.midY)>0.008 ? a.boundingBox.midY>b.boundingBox.midY : a.boundingBox.minX<b.boundingBox.minX }
   text+=rows.compactMap{$0.topCandidates(1).first?.string}.joined(separator:"\n")+"\n"
   guard text.count<=1_000_000 else{throw ClientError("OCR text exceeds safe size limit.")}
  }
  guard text.filter({!$0.isWhitespace}).count>=80 else{throw ClientError("Insufficient readable text. Try an original downloaded report.")};return(text,"On-device Vision OCR")
 }
 static func letter(_ text:String)throws->Data {
  let output=NSMutableData();guard let consumer=CGDataConsumer(data:output as CFMutableData) else{throw ClientError("PDF output unavailable.")}
  var bounds=CGRect(x:0,y:0,width:612,height:792);guard let context=CGContext(consumer:consumer,mediaBox:&bounds,nil) else{throw ClientError("PDF renderer unavailable.")}
  let style=NSMutableParagraphStyle();style.lineSpacing=4
  let string=NSAttributedString(string:text,attributes:[NSAttributedString.Key(kCTFontAttributeName as String):CTFontCreateWithName("Helvetica" as CFString,11,nil),.paragraphStyle:style])
  let setter=CTFramesetterCreateWithAttributedString(string);var offset=0
  while offset<string.length {
   context.beginPDFPage(nil);let path=CGPath(rect:CGRect(x:54,y:54,width:504,height:684),transform:nil)
   let frame=CTFramesetterCreateFrame(setter,CFRange(location:offset,length:0),path,nil);CTFrameDraw(frame,context);let count=CTFrameGetVisibleStringRange(frame).length;context.endPDFPage();guard count>0 else{throw ClientError("Letter could not be paginated.")};offset+=count
  }
  context.closePDF();return output as Data
 }
}
