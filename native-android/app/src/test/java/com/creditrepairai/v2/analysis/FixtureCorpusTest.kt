package com.creditrepairai.v2.analysis

import com.creditrepairai.v2.model.Bureau
import org.junit.Assert.*
import org.junit.Test

/** Checked-in text plus independent expected fields; all records are fictional. */
class FixtureCorpusTest {
    private fun resource(name: String) = javaClass.classLoader!!.getResourceAsStream("parser/corpus/$name")!!.bufferedReader().use { it.readText() }
    @Test(timeout = 15_000) fun allThreeBureausHaveSixLayoutsWithExactCoreFields() {
        val rows = resource("manifest.tsv").lineSequence().filter { it.isNotBlank() && !it.startsWith('#') }.map { it.split('\t') }.toList()
        assertEquals(18, rows.size)
        assertEquals(18, rows.map { it[0] }.distinct().size)
        assertEquals(mapOf("EQUIFAX" to 6, "EXPERIAN" to 6, "TRANSUNION" to 6), rows.groupingBy { it[1] }.eachCount())
        val parser = ReportTextParser()
        for (row in rows) {
            val text = resource(row[0])
            val bureau = Bureau.valueOf(row[1])
            assertEquals(row[0], bureau, parser.detectBureau("synthetic.pdf", text))
            assertEquals(row[0], row[2].toIntOrNull(), parser.parseScore(text))
            val accounts = parser.parseAccounts(text, bureau)
            val suffixes = if (row[3] == "-") emptyList() else row[3].split(',')
            val balances = if (row[4] == "-") emptyList() else row[4].split(',').map(String::toInt)
            assertEquals(row[0], suffixes, accounts.map { it.accountSuffix })
            assertEquals(row[0], balances, accounts.map { it.balance })
            assertEquals(row[0], listOf("Fictional Cedar Bank", "Fictional Birch Bank").take(suffixes.size), accounts.map { it.creditor })
        }
    }
}
