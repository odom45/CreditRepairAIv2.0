package com.creditrepairai.v2.analysis

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

class ParserLimitsTest {
    @Test(timeout = 5_000) fun writerRejectsBeforeAppendingBeyondLimit() {
        val writer = LimitedTextWriter(10)
        writer.write("0123456789")
        assertThrows(ParserLimitException::class.java) { writer.write("x") }
        assertEquals("0123456789", writer.toString())
    }
    @Test(timeout = 5_000) fun oversizedStreamIsRejectedBeforeWritingPastLimit() {
        val output = ByteArrayOutputStream()
        assertThrows(ParserLimitException::class.java) {
            ParserLimits.copyPdf(ByteArrayInputStream("%PDF-123456".toByteArray()), output, 10)
        }
        assertTrue(output.size() <= 10)
        val exact = ByteArrayOutputStream()
        ParserLimits.copyPdf(ByteArrayInputStream("%PDF-12345".toByteArray()), exact, 10)
        assertEquals("%PDF-12345", exact.toString("US-ASCII"))
    }
    @Test(timeout = 5_000) fun zeroByteBulkReadsCannotLoopForever() {
        val bytes = ByteArrayInputStream("%PDF-12345".toByteArray())
        val zeroReads = object : InputStream() {
            override fun read() = bytes.read()
            override fun read(buffer: ByteArray, offset: Int, length: Int) = 0
        }
        val output = ByteArrayOutputStream()
        ParserLimits.copyPdf(zeroReads, output, 10)
        assertEquals("%PDF-12345", output.toString("US-ASCII"))
    }
    @Test(timeout = 5_000) fun malformedAndTruncatedHeadersAreControlledRejections() {
        for (input in listOf("", "%PD", "not a PDF")) {
            assertThrows(IllegalArgumentException::class.java) {
                ParserLimits.copyPdf(ByteArrayInputStream(input.toByteArray()), ByteArrayOutputStream())
            }
        }
    }
}
