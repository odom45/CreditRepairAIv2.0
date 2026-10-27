package com.creditrepairai.v2.analysis

import java.io.InputStream
import java.io.OutputStream
import java.io.Writer

/** Expected, user-visible rejection. Fuzz tests must not hide other exceptions or VM errors. */
internal class ParserLimitException(message: String) : IllegalArgumentException(message)

internal object ParserLimits {
    const val PDF_BYTES = 50_000_000
    const val TEXT_CHARACTERS = 1_000_000
    const val TEXT_LINES = 30_000
    const val LINE_CHARACTERS = 8_192
    const val ACCOUNTS = 150
    const val OCR_WORDS = 40_000

    fun text(text: String) {
        if (text.length > TEXT_CHARACTERS) throw ParserLimitException("Report exceeds readable text limit. Import smaller report sections; no text was silently skipped.")
        var lines = 1
        var length = 0
        for (character in text) {
            if (character == '\n' || character == '|') {
                if (++lines > TEXT_LINES) throw ParserLimitException("Report exceeds text line limit. Import smaller report sections.")
                length = 0
            } else if (++length > LINE_CHARACTERS) {
                throw ParserLimitException("Report contains an oversized text line. Export a standard bureau layout.")
            }
        }
    }

    /** Stream to a private snapshot; even streams returning zero must make progress. */
    fun copyPdf(input: InputStream, output: OutputStream, limit: Int = PDF_BYTES) {
        val header = ByteArray(5)
        for (index in header.indices) {
            val byte = input.read()
            require(byte >= 0) { "Selected file is not a PDF." }
            header[index] = byte.toByte()
        }
        require(header.toString(Charsets.US_ASCII) == "%PDF-") { "Selected file is not a PDF." }
        if (limit < header.size) throw ParserLimitException("PDF is larger than the import limit.")
        output.write(header)
        var total = header.size
        val buffer = ByteArray(8192)
        while (true) {
            var count = input.read(buffer)
            if (count < 0) break
            if (count == 0) {
                val byte = input.read()
                if (byte < 0) break
                buffer[0] = byte.toByte()
                count = 1
            }
            if (count > limit - total) throw ParserLimitException("PDF is larger than 50 MB. Export a smaller report.")
            output.write(buffer, 0, count)
            total += count
        }
    }
}

/** PDFTextStripper writes into this cap instead of constructing an unlimited result String. */
internal class LimitedTextWriter(private val limit: Int = ParserLimits.TEXT_CHARACTERS) : Writer() {
    private val value = StringBuilder()
    override fun write(buffer: CharArray, offset: Int, length: Int) {
        if (length > limit - value.length) throw ParserLimitException("PDF exceeds readable text limit. Import smaller report sections.")
        value.append(buffer, offset, length)
    }
    override fun flush() = Unit
    override fun close() = Unit
    override fun toString() = value.toString()
}
