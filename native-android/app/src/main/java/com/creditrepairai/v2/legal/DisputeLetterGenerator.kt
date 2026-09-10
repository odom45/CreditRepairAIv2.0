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

    fun cfpbNarrative(dispute: Dispute): String = """
        I am submitting this complaint about credit-report information associated with ${dispute.creditor} and reported by ${dispute.bureaus.joinToString { it.displayName }}.

        ${dispute.reason}

        I previously reviewed the information and am requesting a reasonable investigation and a clear written explanation of the result. My requested resolution is correction or deletion of information that is inaccurate, incomplete, or cannot be verified, plus an updated consumer report.

        I will attach copies of my report, identification, prior correspondence, and records supporting the specific facts above. I understand this draft must be reviewed and edited for accuracy before submission.
    """.trimIndent()
}
