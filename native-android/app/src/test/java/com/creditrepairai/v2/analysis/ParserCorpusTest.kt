package com.creditrepairai.v2.analysis

import com.creditrepairai.v2.model.Bureau
import com.creditrepairai.v2.model.CreditAccount
import org.junit.Assert.*
import org.junit.Test

/** Expectations are fixed here; they are never obtained from the parser under test. */
class ParserCorpusTest {
    private fun resource(name: String) = javaClass.classLoader!!
        .getResourceAsStream("parser/expanded/$name")!!.bufferedReader().use { it.readText() }

    @Test(timeout = 15_000) fun allBureausAndSixNewLayoutsHaveExactIndependentExpectations() {
        val rows = resource("manifest.tsv").lineSequence().drop(1).filter(String::isNotBlank).map { it.split('\t') }.toList()
        assertEquals(18, rows.size)
        assertEquals(18, rows.map { it[0] }.distinct().size)
        for (bureau in listOf(Bureau.EQUIFAX, Bureau.EXPERIAN, Bureau.TRANSUNION)) {
            assertEquals(6, rows.count { it[1] == bureau.name })
        }
        for ((file, bureauName, layout, score, count) in rows) {
            val parser = ReportTextParser()
            val text = resource(file)
            val bureau = Bureau.valueOf(bureauName)
            assertEquals(file, bureau, parser.detectBureau("synthetic.pdf", text))
            assertEquals(file, score.toIntOrNull(), parser.parseScore(text))
            val accounts = parser.parseAccounts(text, bureau).map { it.copy(id = "fixed") }
            val expected = (0 until count.toInt()).map { index ->
                val sparse = layout == "sparse"
                CreditAccount(id = "fixed", creditor = listOf("SYNTHETIC NORTH BANK", "SYNTHETIC HARBOR AUTO")[index],
                    accountSuffix = listOf("1234", "5678")[index], bureau = bureau,
                    balance = if (sparse) null else listOf(1280, 680)[index],
                    status = if (sparse) "Unknown" else listOf("Open", "Closed")[index],
                    paymentStatus = if (sparse) "" else listOf("Current", "Paid")[index],
                    openedDate = if (sparse) "" else "01/2020", dateReported = if (sparse) "" else "09/2026",
                    accountType = if (sparse) "" else "Revolving", responsibility = if (sparse) "" else "Individual",
                    creditLimit = if (sparse) null else 5000, highBalance = if (sparse) null else 1400,
                    pastDue = if (sparse) null else 0, originalCreditor = if (sparse) "" else "SYNTHETIC ORIGINAL BANK",
                    remarks = if (sparse) "" else "Synthetic record only")
            }
            assertEquals(file, expected, accounts)
        }
    }
}
