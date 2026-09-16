package com.creditrepairai.v2.ai

/**
 * Removes high-risk identifiers before any text can cross the device boundary.
 * The report parser never retains the full PDF text; this is a second layer for
 * user-entered questions and structured case notes.
 */
object SensitiveDataRedactor {
    private val ssn = Regex("(?<!\\d)\\d{3}[- ]?\\d{2}[- ]?\\d{4}(?!\\d)")
    private val email = Regex("(?i)\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b")
    private val phone = Regex("(?<!\\d)(?:\\+?1[-. ]?)?(?:\\(\\d{3}\\)|\\d{3})[-. ]?\\d{3}[-. ]?\\d{4}(?!\\d)")
    private val dateOfBirth = Regex("(?i)\\b(?:dob|date of birth|born)\\s*[:#-]?\\s*(?:\\d{1,2}[/-]){2}\\d{2,4}\\b")
    private val fullAccountNumber = Regex("(?i)\\b(?:account|acct)\\s*(?:number|no\\.?|#)?\\s*[:#-]?\\s*([A-Z0-9-]{6,})")
    private val longNumber = Regex("(?<!\\d)\\d{9,19}(?!\\d)")

    fun redact(value: String): String = value
        .replace(ssn, "[SSN REDACTED]")
        .replace(email, "[EMAIL REDACTED]")
        .replace(phone, "[PHONE REDACTED]")
        .replace(dateOfBirth, "DOB: [REDACTED]")
        .replace(fullAccountNumber) { match ->
            val raw = match.groupValues[1].filter(Char::isLetterOrDigit)
            "account ••••${raw.takeLast(4)}"
        }
        .replace(longNumber, "[LONG NUMBER REDACTED]")
        .take(MAX_TEXT_LENGTH)

    internal fun containsHighRiskIdentifier(value: String): Boolean =
        ssn.containsMatchIn(value) || email.containsMatchIn(value) || phone.containsMatchIn(value) ||
            dateOfBirth.containsMatchIn(value) || longNumber.containsMatchIn(value)

    private const val MAX_TEXT_LENGTH = 4_000
}
