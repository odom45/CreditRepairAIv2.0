package com.creditrepairai.v2.legal

import com.creditrepairai.v2.model.Dispute
import com.creditrepairai.v2.model.Finding
import java.text.DateFormat
import java.util.Date

object DisputeLetterGenerator {
    fun fromFinding(finding: Finding): Dispute {
        val bureauNames = finding.bureaus.joinToString { it.displayName }
        val reason = buildString {
            append("I am disputing inaccurate or incomplete information reported for ${finding.creditor}. ")
            append(finding.detail)
            append(" Please conduct a reasonable reinvestigation, review the supporting records I provide, and delete or correct any information that cannot be verified as complete and accurate.")
        }
        val lawText = finding.legalBasis.joinToString(", ")
        val letter = """
            ${DateFormat.getDateInstance(DateFormat.LONG).format(Date())}

            [YOUR FULL NAME]
            [YOUR CURRENT ADDRESS]
            [CITY, STATE ZIP]

            To: $bureauNames

            Re: Credit report dispute — ${finding.creditor}

            I am writing to dispute information in my consumer file that I believe is inaccurate or incomplete.

            $reason

            The issue I identified is: ${finding.title}. ${finding.detail}

            Please investigate this matter under $lawText, forward all relevant information I provide to the furnisher as required, and send me the written results and an updated copy of my report. I have enclosed copies of supporting records; originals are not enclosed.

            Sincerely,
            [YOUR SIGNATURE]
            [YOUR FULL NAME]

            Enclosures: [LIST COPIES OF SUPPORTING DOCUMENTS]
        """.trimIndent()
        return Dispute(
            findingId = finding.id,
            creditor = finding.creditor,
            bureaus = finding.bureaus,
            reason = reason,
            letter = letter,
        )
    }

    fun cfpbNarrative(
        dispute: Dispute,
        problem: String = dispute.reason,
        companyResponse: String = "",
        requestedResolution: String = "Correct or delete information that is inaccurate, incomplete, or cannot be verified, and provide an updated consumer report with a written explanation of the investigation result.",
        evidence: String = "Credit report pages, prior correspondence, proof of delivery, and records supporting the disputed facts.",
    ): String = """
        PRODUCT: Credit reporting
        COMPANY OR ITEM: ${dispute.creditor}
        REPORTING COMPANY: ${dispute.bureaus.joinToString { it.displayName }}

        WHAT HAPPENED:
        ${problem.trim()}

        WHAT THE COMPANY SAID OR DID:
        ${companyResponse.trim().ifBlank { "No response has been entered. Add the response received or state truthfully that none was received after the applicable waiting period." }}

        REQUESTED RESOLUTION:
        ${requestedResolution.trim()}

        SUPPORTING DOCUMENTS TO ATTACH AS COPIES:
        ${evidence.trim()}

        PRIVACY CHECK: Remove SSNs, full account numbers, unnecessary birth-date information, and unrelated sensitive records. Review every statement before submitting through the official CFPB portal.
    """.trimIndent()
}
