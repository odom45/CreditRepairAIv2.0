package com.creditrepairai.v2.letters

import com.creditrepairai.v2.model.Bureau
import com.creditrepairai.v2.model.Dispute

/** Public bureau addresses only; personal sender details are never placed in agent snapshots. */
data class BureauMailAddress(val address: String, val source: String)
object PaperDisputeLetter {
    const val ADDRESS_CHECKED = "2026-10-26"
    fun recipient(bureau: Bureau): BureauMailAddress = when (bureau) {
        Bureau.EXPERIAN -> BureauMailAddress("Experian\nDispute by Mail\nP.O. Box 4500\nAllen, TX 75013", "https://www.experian.com/help/dispute-credit/")
        Bureau.EQUIFAX -> BureauMailAddress("Equifax Information Services LLC\nP.O. Box 740256\nAtlanta, GA 30374", "https://assets.equifax.com/assets/personal/Dispute.pdf")
        Bureau.TRANSUNION -> BureauMailAddress("TransUnion Consumer Solutions\nP.O. Box 2000\nChester, PA 19016-2000", "https://www.transunion.com/credit-disputes/dispute-your-credit/mail-or-phone")
        Bureau.UNKNOWN -> error("Verify the reporting bureau before preparing a mailed dispute.")
    }
    fun compose(dispute: Dispute, bureau: Bureau, name: String, address: String, date: String,
                facts: String, evidence: String, resolution: String): String {
        require(bureau in dispute.bureaus && bureau != Bureau.UNKNOWN) { "Choose a bureau that reports this disputed item." }
        require(name.isNotBlank() && name.length <= 160 && address.isNotBlank() && address.length <= 600)
        require(date.isNotBlank() && date.length <= 80)
        require(facts.isNotBlank() && facts.length <= 10000 && evidence.isNotBlank() && evidence.length <= 2000)
        require(resolution.isNotBlank() && resolution.length <= 2000)
        return """
            ${name.trim()}
            ${address.trim()}

            ${date.trim()}

            ${recipient(bureau).address}

            Re: Credit report dispute - ${dispute.creditor}

            To the consumer dispute department:

            I am requesting an investigation of the following information in my ${bureau.displayName} consumer report.

            ${facts.trim()}

            Requested review/correction:
            ${resolution.trim()}

            Enclosed copies of supporting records:
            ${evidence.trim()}

            Please review the information and supporting records, correct any inaccurate or incomplete reporting you identify, and send me your written investigation results and an updated report when applicable.

            Sincerely,

            Signature: ____________________________________
            ${name.trim()}
        """.trimIndent().lines().joinToString("\n") { it.trimStart() }
    }
}
