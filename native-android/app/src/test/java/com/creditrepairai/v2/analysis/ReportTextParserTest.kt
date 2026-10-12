package com.creditrepairai.v2.analysis
import com.creditrepairai.v2.model.Bureau
import org.junit.Assert.*
import org.junit.Test
class ReportTextParserTest {
    private val parser = ReportTextParser()
    private fun fixture(name: String) = javaClass.classLoader!!.getResourceAsStream("parser/$name-fictional.txt")!!.bufferedReader().use { it.readText() }
    @Test fun parsesThreeFictionalLayoutsAndNeverInventsMissingScore() {
        val ex = fixture("experian")
        assertEquals(Bureau.EXPERIAN, parser.detectBureau("report.pdf", ex))
        assertEquals(620, parser.parseScore(ex))
        val accounts = parser.parseAccounts(ex, Bureau.EXPERIAN)
        assertEquals(2, accounts.size)
        assertEquals(1280, accounts[0].balance)
        assertEquals("1234", accounts[0].accountSuffix)
        assertEquals(0, accounts[1].balance)
        assertEquals(5000, accounts[0].creditLimit)
        val eq = fixture("equifax")
        assertEquals(630, parser.parseScore(eq))
        assertEquals(680, parser.parseAccounts(eq, Bureau.EQUIFAX).single().balance)
        val tu = fixture("transunion")
        assertNull(parser.parseScore(tu))
        assertEquals(Bureau.TRANSUNION, parser.detectBureau("report.pdf", tu))
        assertEquals(680, parser.parseAccounts(tu, Bureau.TRANSUNION).single().balance)
    }
    @Test fun missingFieldsDoNotBleedAcrossAccounts() {
        val accounts = parser.parseAccounts("Creditor: Fictional One\nAcct: XXXX1111\nCreditor: Fictional Two\nAcct: XXXX2222\nBalance: $200", Bureau.EQUIFAX)
        assertNull(accounts[0].balance)
        assertEquals(200, accounts[1].balance)
    }
    @Test fun unknownBureauAndOutOfRangeScoresAreNotInvented() {
        assertEquals(Bureau.UNKNOWN, parser.detectBureau("report.pdf", "Unrecognized text"))
        assertNull(parser.parseScore("Your score: 899"))
        assertTrue(parser.parseAccounts("No readable accounts", Bureau.UNKNOWN).isEmpty())
    }
    @Test fun splitValuesInlineFieldsAndTitleCaseKeepAccountBoundaries() {
        val text = "Northstar Bank\nAccount Number:\nXXXX 1234\nAccount Status:\nOpen\nBalance: $1,280 | Credit Limit: $5,000\nCreditor: Harbor Collections\nAcct: XXXX5678\nStatus: Collection\nHigh Balance: $900"
        val records = parser.parseAccounts(text, Bureau.EQUIFAX)
        assertEquals(2, records.size)
        assertEquals("1234", records[0].accountSuffix)
        assertEquals("Open", records[0].status)
        assertEquals(1280, records[0].balance)
        assertEquals(5000, records[0].creditLimit)
        assertNull(records[1].balance) // A high balance is not a current balance.
        assertEquals(900, records[1].highBalance)
    }
    @Test fun creditLimitIsNotAScoreAndLongNumbersAreNotTruncatedToScores() {
        assertNull(parser.parseScore("Credit Limit: 5000\nBalance: 640"))
        assertNull(parser.parseScore("Your score: 6400"))
        assertEquals(640, parser.parseScore("FICO Score: 640"))
    }
}
