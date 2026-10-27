package com.creditrepairai.v2.analysis

import com.creditrepairai.v2.model.Bureau
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class ParserPropertyTest {
    private val parser = ReportTextParser()
    private val seeds = listOf(7, 42, 20261026)
    private fun parseAndCheck(text: String) {
        try {
            val bureau = parser.detectBureau("synthetic.pdf", text)
            val score = parser.parseScore(text)
            assertTrue(score == null || score in 300..850)
            val accounts = parser.parseAccounts(text, bureau)
            assertTrue(accounts.size <= 150)
            for (account in accounts) {
                assertEquals(bureau, account.bureau)
                assertTrue(account.creditor.length in 3..64)
                assertTrue(account.accountSuffix.length <= 4)
                assertTrue(listOf(account.balance, account.pastDue, account.creditLimit, account.highBalance).all { it == null || it >= 0 })
                assertTrue(account.status.length <= 80 && account.paymentStatus.length <= 80 && account.remarks.length <= 200)
            }
            // UUIDs are deliberately fresh; the extracted fields must be deterministic.
            assertEquals(accounts.map { it.copy(id = "") }, parser.parseAccounts(text, bureau).map { it.copy(id = "") })
        } catch (_: ParserLimitException) {
            // Only the explicit resource-limit rejection is an expected fuzz outcome.
        }
    }

    @Test(timeout = 60_000) fun seededMutationsNeverCrashAndPreserveOutputBounds() {
        val fixtures = listOf("equifax", "experian", "transunion").map { name ->
            javaClass.classLoader!!.getResourceAsStream("parser/$name-fictional.txt")!!.bufferedReader().use { it.readText() }
        }
        val quirks = listOf("", "\u0000", "\u00a0", "\u202f", "\u200b", "\u202e", "\ud83d\ude80", "\ud800", "\ufffd", "|", "\r\n", "\f", ":", "$", "9".repeat(128))
        for (seed in seeds) {
            val random = Random(seed)
            repeat(800) { iteration ->
                val original = fixtures[random.nextInt(fixtures.size)]
                val cut = random.nextInt(original.length + 1)
                val text = when (iteration % 6) {
                    0 -> original.take(cut) // truncated extracted page
                    1 -> original.take(cut) + quirks.random(random) + original.drop(cut)
                    2 -> original.lines().shuffled(random).joinToString("\n")
                    3 -> original.repeat(random.nextInt(0, 8))
                    4 -> original.map { if (random.nextInt(5) == 0) random.nextInt(0x10000).toChar() else it }.joinToString("")
                    else -> List(random.nextInt(256)) { quirks.random(random) }.joinToString("")
                }
                try { parseAndCheck(text) } catch (failure: Throwable) {
                    throw AssertionError("Reproduce with seed=$seed iteration=$iteration", failure)
                }
            }
        }
    }

    @Test(timeout = 15_000) fun limitsRejectBeforeExpensiveNormalizationAndAllocation() {
        fun rejected(text: String) {
            assertTrue(runCatching { parser.parseAccounts(text, Bureau.UNKNOWN) }.exceptionOrNull() is ParserLimitException)
            assertTrue(runCatching { parser.parseScore(text) }.exceptionOrNull() is ParserLimitException)
        }
        rejected("x".repeat(ParserLimits.MAX_TEXT_CHARS + 1))
        rejected("9".repeat(ParserLimits.MAX_LINE_CHARS + 1))
        rejected("|".repeat(ParserLimits.MAX_FIELDS))
        parseAndCheck("x".repeat(ParserLimits.MAX_LINE_CHARS))
        parseAndCheck(("x".repeat(999) + "\n").repeat(1000)) // exact character limit
        parseAndCheck("\n".repeat(ParserLimits.MAX_FIELDS - 1))
        val writer = BoundedReportWriter()
        val block = CharArray(1000) { 'x' }
        repeat(1000) { writer.write(block, 0, block.size) }
        assertEquals(ParserLimits.MAX_TEXT_CHARS, writer.toString().length)
        assertTrue(runCatching { writer.write(charArrayOf('x'), 0, 1) }.exceptionOrNull() is ParserLimitException)
        assertEquals(ParserLimits.MAX_TEXT_CHARS, writer.toString().length)
    }

    @Test(timeout = 15_000) fun generatedAccountsKeepBoundariesAcrossBureausAndWhitespaceVariants() {
        assertTrue(parser.parseAccounts("", Bureau.UNKNOWN).isEmpty())
        assertNull(parser.parseScore(""))
        for (bureau in listOf(Bureau.EQUIFAX, Bureau.EXPERIAN, Bureau.TRANSUNION)) {
            for (seed in seeds) {
                val random = Random(seed)
                for (count in listOf(0, 1, 2, 20, 150)) {
                    val text = bureau.displayName + "\nCredit Score: 640\n" + (0 until count).joinToString("\n\f\n") { i ->
                        val whitespace = listOf(" ", "\t", "\u00a0", "\u202f").random(random)
                        "Creditor: Fictional Bank $i\nAccount Number: XXXX${1000 + i}\nBalance:$whitespace${i * 17}\nStatus: Open"
                    }
                    val accounts = parser.parseAccounts(text, bureau)
                    assertEquals(count, accounts.size)
                    accounts.forEachIndexed { index, account ->
                        assertEquals("Fictional Bank $index", account.creditor)
                        assertEquals((1000 + index).toString(), account.accountSuffix)
                        assertEquals(index * 17, account.balance)
                    }
                }
            }
        }
    }
}
