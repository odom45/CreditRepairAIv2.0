package com.creditrepairai.v2.analysis

import com.creditrepairai.v2.model.Bureau
import com.creditrepairai.v2.model.CreditAccount
import java.util.Locale

/** Pure text parser used by Android import and permission-free JVM fixtures. */
class ReportTextParser {
    internal fun detectBureau(fileName: String, text: String): Bureau {
        val source = "${fileName.take(512)}\n${text.take(8_000)}".lowercase(Locale.US)
        val compact = source.replace(Regex("\\s+"), "")
        return when {
            "transunion" in compact -> Bureau.TRANSUNION
            "experian" in compact -> Bureau.EXPERIAN
            "equifax" in compact -> Bureau.EQUIFAX
            else -> Bureau.UNKNOWN
        }
    }

    internal fun parseScore(text: String): Int? {
        ParserLimits.text(text)
        val scorePatterns = listOf(
            Regex("(?i)\\b(?:credit\\s+score|fico[®™ ]*(?:score)?|vantage(?:score)?[®™ ]*(?:score)?)[^0-9]{0,25}([3-8][0-9]{2})(?![0-9])"),
            Regex("(?i)(?:your|current) score[^0-9]{0,15}([3-8][0-9]{2})(?![0-9])"),
        )
        return scorePatterns.firstNotNullOfOrNull { pattern ->
            pattern.find(text.replace('\u00a0', ' ').replace('\u202f', ' '))?.groupValues?.getOrNull(1)?.toIntOrNull()?.takeIf { it in 300..850 }
        }
    }

    internal fun parseAccounts(text: String, bureau: Bureau): List<CreditAccount> {
        ParserLimits.text(text)
        val rawLines = text.lineSequence()
            .flatMap { it.split('|').asSequence() }
            .map { it.replace('\u00a0', ' ').replace('\u202f', ' ').replace(Regex("\\s+"), " ").trim()
                .replace(Regex("(?i)^date\\s+[o0]?pened\\b"), "Date Opened") }
            .filter { it.isNotBlank() }.toList()
        val lines = mutableListOf<String>()
        var cursor = 0
        while (cursor < rawLines.size) {
            val line = rawLines[cursor]
            val next = rawLines.getOrNull(cursor + 1)
            if (EMPTY_FIELD.matches(line) && next != null && !FIELD_PREFIX.containsMatchIn(next)) {
                lines.add("${line.trimEnd(':', '#', '-', ' ')}: $next")
                cursor += 2
            } else {
                lines.add(line)
                cursor += 1
            }
        }
        val candidateIndexes = lines.indices.filter { index ->
            val line = lines[index]
            ACCOUNT_HEADING.containsMatchIn(line) ||
                (line.length in 3..64 && line.any(Char::isLetter) && !line.contains(':') &&
                    line.uppercase(Locale.US) !in IGNORED_HEADINGS && !FIELD_PREFIX.containsMatchIn(line) && !MASKED_IDENTIFIER.matches(line) &&
                    lines.getOrNull(index + 1)?.let { ACCOUNT_NUMBER.containsMatchIn(it) } == true)
        }

        if (candidateIndexes.size > ParserLimits.ACCOUNTS) throw ParserLimitException(
            "Report exceeds 150 account sections. Import smaller report sections; no accounts were silently skipped.",
        )
        return candidateIndexes.mapIndexedNotNull { position, index ->
            val end = candidateIndexes.getOrNull(position + 1) ?: lines.size
            val window = lines.subList(index, end)
            val headingMatch = ACCOUNT_HEADING.find(window.first())
            val creditor = sanitizeCreditor(
                headingMatch?.groupValues?.getOrNull(1)?.takeIf(String::isNotBlank)
                    ?: window.first(),
            )
            if (creditor.length < 3 || creditor.uppercase(Locale.US) in IGNORED_HEADINGS) return@mapIndexedNotNull null

            val accountLine = window.firstOrNull { ACCOUNT_NUMBER.containsMatchIn(it) }.orEmpty()
            val suffix = Regex("(?i)^(?:account(?:\\s*(?:number|no\\.?|#))?|acct)\\s*[:#-]+\\s*([*Xx\\s-]*[A-Z0-9-]{3,})")
                .find(accountLine)?.groupValues?.getOrNull(1).orEmpty().filter(Char::isLetterOrDigit).takeLast(4)
            val status = extractValue(window, "account status") ?: extractValue(window, "status") ?: window.firstOrNull { NEGATIVE_STATUS.containsMatchIn(it) }
                ?.let { NEGATIVE_STATUS.find(it)?.value } ?: "Unknown"
            val balance = window.firstNotNullOfOrNull { line ->
                BALANCE.find(line)?.groupValues?.getOrNull(1)?.let(::parseMoney)
            }
            val paymentStatus = extractValue(window, "payment status") ?: extractValue(window, "pay status") ?: ""
            val opened = normalizeDate(extractValue(window, "date opened") ?: extractValue(window, "opened") ?: "")
            val accountType = extractValue(window, "account type") ?: extractValue(window, "type") ?: ""
            val responsibility = extractValue(window, "responsibility") ?: extractValue(window, "ownership") ?: ""
            val dateReported = normalizeDate(extractValue(window, "date reported") ?: extractValue(window, "last reported") ?: "")
            val originalCreditor = extractValue(window, "original creditor") ?: ""
            val pastDue = extractMoneyValue(window, "past due")
            val creditLimit = extractMoneyValue(window, "credit limit") ?: extractMoneyValue(window, "limit")
            val highBalance = extractMoneyValue(window, "high balance") ?: extractMoneyValue(window, "highest balance")
            val remarks = extractValue(window, "remarks") ?: extractValue(window, "comments") ?: ""

            CreditAccount(
                creditor = creditor,
                accountSuffix = suffix,
                bureau = bureau,
                status = status.take(80),
                balance = balance,
                paymentStatus = paymentStatus.take(80),
                openedDate = opened.take(40),
                accountType = accountType.take(80),
                responsibility = responsibility.take(80),
                dateReported = dateReported.take(40),
                originalCreditor = originalCreditor.take(80),
                pastDue = pastDue,
                creditLimit = creditLimit,
                highBalance = highBalance,
                remarks = remarks.take(200),
            )
        }
    }

    private fun extractValue(lines: List<String>, label: String): String? {
        val flexibleLabel = label.split(' ').joinToString("\\s*") { Regex.escape(it) }
        val pattern = Regex("(?i)^${flexibleLabel}\\s*[:#-]?\\s*(.+)$")
        return lines.firstNotNullOfOrNull { pattern.find(it)?.groupValues?.getOrNull(1)?.trim() }
    }

    private fun extractMoneyValue(lines: List<String>, label: String): Int? {
        val flexibleLabel = label.split(' ').joinToString("\\s*") { Regex.escape(it) }
        val pattern = Regex("(?i)^${flexibleLabel}\\s*[:#-]?\\s*\\$?([0-9][0-9,. ]*)")
        return lines.firstNotNullOfOrNull { line ->
            pattern.find(line)?.groupValues?.getOrNull(1)?.let(::parseMoney)
        }
    }

    private fun parseMoney(raw: String): Int? = runCatching {
        // Int money cannot need thousands of digits; avoid regex recursion on hostile numerals.
        if (raw.length > 32) return@runCatching null
        val number = raw.replace("$", "").replace(" ", "").trim()
        val whole = when {
            Regex("[0-9]{1,3}(?:\\.[0-9]{3})+").matches(number) -> number.replace(".", "")
            Regex("(?:[0-9]+|[0-9]{1,3}(?:,[0-9]{3})+)(?:\\.[0-9]{1,2})?").matches(number) -> number.substringBefore('.').replace(",", "")
            else -> return@runCatching null
        }
        whole.toLongOrNull()
            ?.takeIf { it in 0..Int.MAX_VALUE.toLong() }?.toInt()
    }.getOrNull()

    // Rejoin only digits and separators already present; never infer a date digit.
    private fun normalizeDate(value: String) = value
        .replace(Regex("(?<=\\d)\\s+(?=\\d)"), "")
        .replace(Regex("\\s*/\\s*"), "/")

    private fun sanitizeCreditor(value: String): String = value
        .replace(ACCOUNT_HEADING, "")
        .replace(Regex("[^A-Za-z0-9 &.'/-]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim(' ', ':', '-')
        .take(64)

    private fun normalizeName(value: String) = value.lowercase(Locale.US).replace(Regex("[^a-z0-9]"), "")

    companion object {
        private val ACCOUNT_HEADING = Regex("(?i)^(?:creditor|account name|company|subscriber)\\s*[:#-]?\\s*(.*)$")
        private val ACCOUNT_NUMBER = Regex("(?i)^(?:account(?:\\s*(?:number|no\\.?|#))?|acct)\\s*[:#-]")
        private val BALANCE = Regex("(?i)^(?:current\\s*)?balance\\s*[:#-]?\\s*\\$?([0-9][0-9,. ]*)")
        private const val LABELS = "account number|account no[.]?|account #|acct|account status|payment status|pay status|date opened|date reported|last reported|original creditor|account type|credit limit|high balance|highest balance|current balance|responsibility|ownership|past due|balance|status|opened|limit|type|remarks|comments|creditor|account name|company|subscriber"
        private val LABEL_PATTERN = LABELS.replace(" ", "\\s*")
        private val EMPTY_FIELD = Regex("(?i)^(?:${LABEL_PATTERN})\\s*[:#-]?$")
        private val FIELD_PREFIX = Regex("(?i)^(?:${LABEL_PATTERN})\\s*[:#-]")
        private val MASKED_IDENTIFIER = Regex("[*Xx -]*[0-9]{3,}")
        private val NEGATIVE_STATUS = Regex("(?i)charge[ -]?off|collection|repossession|late|delinquent|past due|closed|paid")
        private val IGNORED_HEADINGS = setOf(
            "ACCOUNT INFORMATION", "ACCOUNT HISTORY", "CREDIT REPORT", "PERSONAL INFORMATION", "SUMMARY",
        )
    }
}
