package com.creditrepairai.v2.analysis

import com.creditrepairai.v2.model.Bureau
import com.creditrepairai.v2.model.CreditAccount
import org.junit.Assert.*
import org.junit.Test
import java.util.Random

class ParserPropertyTest {
    private val parser = ReportTextParser()
    private fun normalized(accounts: List<CreditAccount>) = accounts.map { it.copy(id = "fixed") }

    @Test(timeout = 30_000) fun seededMutationsAndArbitraryUnicodeNeverCrashAndAreDeterministic() {
        val seed = 0x435245444954L
        val random = Random(seed)
        val base = "Experian CREDIT REPORT\nSYNTHETIC ONLY\nFICO Score: 640\nCreditor: Synthetic Bank\nAcct: XXXX1234\nBalance: $1280\nStatus: Open\n"
        val quirks = listOf("\u0000", "\u00a0", "\u202f", "\u200b", "\u202e", "\ud800", "\udfff", "\r\n", "\f\n", "|", "💳", "漢字")
        repeat(1_000) { iteration ->
            val text = when (iteration % 8) {
                0 -> base.take(random.nextInt(base.length + 1))
                1 -> buildString { repeat(random.nextInt(4096)) { append(random.nextInt(65536).toChar()) } }
                2 -> base.replace(" ", quirks[random.nextInt(quirks.size)])
                3 -> base.repeat(random.nextInt(8))
                4 -> base.replace("1280", "9".repeat(random.nextInt(4096)))
                5 -> base.toMutableList().apply { repeat(20) { if (isNotEmpty()) removeAt(random.nextInt(size)) } }.joinToString("")
                6 -> base.chunked(1).joinToString(quirks[random.nextInt(quirks.size)])
                else -> (0 until random.nextInt(160)).joinToString("\n") { "Creditor: Synthetic $it\nAcct: XXXX1234\nBalance: $0" }
            }
            try {
                val bureau = parser.detectBureau("synthetic.pdf", text)
                val score = parser.parseScore(text)
                assertTrue("seed=$seed case=$iteration score", score == null || score in 300..850)
                val accounts = parser.parseAccounts(text, bureau)
                assertTrue("seed=$seed case=$iteration count", accounts.size <= ParserLimits.ACCOUNTS)
                accounts.forEach {
                    assertTrue(it.creditor.length in 3..64)
                    assertTrue(it.accountSuffix.length <= 4)
                    assertTrue(it.remarks.length <= 200)
                    listOfNotNull(it.balance, it.creditLimit, it.highBalance, it.pastDue).forEach { money -> assertTrue(money >= 0) }
                }
                assertEquals("seed=$seed case=$iteration", normalized(accounts), normalized(parser.parseAccounts(text, bureau)))
            } catch (expected: ParserLimitException) {
                // This is the only acceptable failure: explicit production resource rejection.
                assertTrue(expected.message!!.isNotBlank())
            }
        }
    }

    @Test(timeout = 15_000) fun generatedValidReportsPreserveEveryAccountAndMissingFieldsAcrossWhitespaceChanges() {
        val seed = 0x4d455441L
        val random = Random(seed)
        repeat(150) { iteration ->
            val bureau = listOf(Bureau.EQUIFAX, Bureau.EXPERIAN, Bureau.TRANSUNION)[iteration % 3]
            val count = random.nextInt(8) + 1
            val balances = List(count) { if (random.nextBoolean()) null else random.nextInt(100_000) }
            val text = bureau.displayName + " CREDIT REPORT\nYour Score: 640\n" + (0 until count).joinToString("\n") { i ->
                "Creditor: Synthetic Bank $i\nAcct: XXXX${1000 + i}\nStatus: Open\n" + (balances[i]?.let { "Balance: $$it\n" } ?: "")
            }
            val original = parser.parseAccounts(text, bureau)
            assertEquals(count, original.size)
            assertEquals(balances, original.map { it.balance })
            assertEquals((0 until count).map { (1000 + it).toString() }, original.map { it.accountSuffix })
            val variants = listOf(text.replace("\n", "\r\n"), text.replace(" ", "\u00a0"), text.replace(": ", ":\t"))
            variants.forEach { changed ->
                assertEquals("seed=$seed case=$iteration", normalized(original), normalized(parser.parseAccounts(changed, bureau)))
                assertEquals(640, parser.parseScore(changed))
            }
        }
    }

    @Test(timeout = 10_000) fun emptyReportAndExactResourceBoundaries() {
        assertTrue(parser.parseAccounts("", Bureau.UNKNOWN).isEmpty())
        assertNull(parser.parseScore(""))
        assertEquals(Bureau.UNKNOWN, parser.detectBureau("", ""))
        ParserLimits.text("a".repeat(ParserLimits.LINE_CHARACTERS))
        ParserLimits.text("\n".repeat(ParserLimits.TEXT_LINES - 1))
        ParserLimits.text(("a".repeat(999) + "\n").repeat(1000))
        val oversized = listOf("a".repeat(ParserLimits.LINE_CHARACTERS + 1), "\n".repeat(ParserLimits.TEXT_LINES), "x".repeat(ParserLimits.TEXT_CHARACTERS + 1))
        oversized.forEach { text ->
            assertThrows(ParserLimitException::class.java) { parser.parseAccounts(text, Bureau.UNKNOWN) }
            assertThrows(ParserLimitException::class.java) { parser.parseScore(text) }
        }
    }
}
