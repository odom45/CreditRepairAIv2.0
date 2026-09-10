package com.creditrepairai.v2.analysis

import com.creditrepairai.v2.legal.DisputeLetterGenerator
import com.creditrepairai.v2.model.Bureau
import com.creditrepairai.v2.model.CreditAccount
import com.creditrepairai.v2.model.CreditReport
import com.creditrepairai.v2.model.FindingType
import org.junit.Assert.assertTrue
import org.junit.Test

class CreditAnalyzerTest {
    private val reports = listOf(
        CreditReport(fileName = "tu.pdf", bureau = Bureau.TRANSUNION, accountCount = 1),
        CreditReport(fileName = "ex.pdf", bureau = Bureau.EXPERIAN, accountCount = 1),
        CreditReport(fileName = "eq.pdf", bureau = Bureau.EQUIFAX, accountCount = 0),
    )

    private val accounts = listOf(
        CreditAccount(creditor = "Example Bank", accountSuffix = "1234", bureau = Bureau.TRANSUNION, status = "Open", balance = 900),
        CreditAccount(creditor = "Example Bank", accountSuffix = "1234", bureau = Bureau.EXPERIAN, status = "Charge-off", balance = 400),
    )

    @Test
    fun detectsCrossBureauDifferences() {
        val types = CreditAnalyzer.analyze(accounts, reports).map { it.type }.toSet()
        assertTrue(FindingType.BALANCE_MISMATCH in types)
        assertTrue(FindingType.STATUS_MISMATCH in types)
        assertTrue(FindingType.MISSING_BUREAU in types)
    }

    @Test
    fun disputeDraftContainsFactAndFederalBasis() {
        val finding = CreditAnalyzer.analyze(accounts, reports).first { it.type == FindingType.BALANCE_MISMATCH }
        val dispute = DisputeLetterGenerator.fromFinding(finding)
        assertTrue(dispute.letter.contains("Example Bank"))
        assertTrue(dispute.letter.contains("15 U.S.C."))
        assertTrue(dispute.letter.contains("[YOUR FULL NAME]"))
    }
}
