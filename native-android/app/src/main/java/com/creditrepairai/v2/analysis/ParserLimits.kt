package com.creditrepairai.v2.analysis

import java.io.Writer

/** Explicit rejection, rather than silently parsing a truncated consumer report. */
internal class ParserLimitException(message: String) : IllegalArgumentException(message)

internal object ParserLimits {
    const val MAX_PDF_BYTES = 50_000_000
    const val MAX_TEXT_CHARS = 1_000_000
    const val MAX_LINE_CHARS = 4_096
    const val MAX_FIELDS = 50_000

    fun check(condition: Boolean, message: String) {
        if (!condition) throw ParserLimitException(message)
    }

    fun validateText(text: String) {
        check(text.length <= MAX_TEXT_CHARS, "Report text exceeds the safe character limit.")
        var lineLength = 0
        var fields = 1
        // Validate before regex normalization, splitting, or building account lists.
        for (character in text) {
            if (character == '\n' || character == '\r' || character == '|') {
                lineLength = 0
                check(++fields <= MAX_FIELDS, "Report exceeds the safe field limit.")
            } else {
                check(++lineLength <= MAX_LINE_CHARS, "Report line exceeds the safe character limit.")
            }
        }
    }
}

/** PDFTextStripper writes here so the limit is enforced during extraction. */
internal class BoundedReportWriter : Writer() {
    private val content = StringBuilder()
    override fun write(buffer: CharArray, offset: Int, count: Int) {
        ParserLimits.check(content.length.toLong() + count <= ParserLimits.MAX_TEXT_CHARS,
            "PDF text exceeds the safe character limit.")
        content.append(buffer, offset, count)
    }
    override fun flush() = Unit
    override fun close() = Unit
    override fun toString() = content.toString()
}
