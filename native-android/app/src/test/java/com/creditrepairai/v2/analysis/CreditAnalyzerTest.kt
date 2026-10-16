package com.creditrepairai.v2.analysis

import com.creditrepairai.v2.legal.DisputeLetterGenerator
import com.creditrepairai.v2.model.Bureau
import com.creditrepairai.v2.model.CreditAccount
import com.creditrepairai.v2.model.CreditReport
import com.creditrepairai.v2.model.FindingType
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test fun separateAccountsFromSameCreditorKeepEveryFinding() {
        val records = listOf("1111", "2222").flatMap { suffix ->
            listOf(
                CreditAccount(creditor = "Example Bank", accountSuffix = suffix, bureau = Bureau.TRANSUNION, balance = 900),
                CreditAccount(creditor = "Example Bank", accountSuffix = suffix, bureau = Bureau.EXPERIAN, balance = 400),
            )
        }
        val findings = CreditAnalyzer.analyze(records, reports).filter { it.type == FindingType.BALANCE_MISMATCH }
        assertEquals(2, findings.size)
        assertEquals(records.map { it.id }.toSet(), findings.flatMap { it.accountIds }.toSet())
    }

    @Test fun missingSuffixKeepsFullOpenedDateWhenComparingAccounts() {
        val records = listOf(
            CreditAccount(creditor = "Example Bank", openedDate = "01/2020", accountType = "Revolving", bureau = Bureau.TRANSUNION, balance = 900),
            CreditAccount(creditor = "Example Bank", openedDate = "02/2020", accountType = "Revolving", bureau = Bureau.EXPERIAN, balance = 400),
        )
        assertFalse(CreditAnalyzer.analyze(records, reports).any { it.type == FindingType.BALANCE_MISMATCH })
    }

    @Test fun missingIdentifiersDoNotTurnUnrelatedAccountsIntoDuplicates() {
        val records = listOf(900, 400).map { balance ->
            CreditAccount(creditor = "Example Bank", bureau = Bureau.TRANSUNION, accountType = "Revolving", balance = balance)
        }
        assertFalse(CreditAnalyzer.analyze(records, reports).any {
            it.type == FindingType.POSSIBLE_DUPLICATE || it.type == FindingType.BALANCE_MISMATCH
        })
    }
}
