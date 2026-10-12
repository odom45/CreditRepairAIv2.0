package com.creditrepairai.v2.ai

import java.io.Reader
import java.io.StringReader
import org.junit.Assert.*
import org.junit.Test

class BoundedResponseTest {
    @Test fun acceptsEmptyAndExactLimitWithoutChangingText() {
        assertEquals("", StringReader("").readBoundedResponse(3))
        assertEquals("abc", StringReader("abc").readBoundedResponse(3))
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsOversizeInsteadOfParsingTruncatedJson() {
        StringReader("{\"answer\":\"too long\"}").readBoundedResponse(10)
    }
    @Test fun infiniteResponseStopsAtOverflowSentinel() {
        var consumed = 0
        val source = object : Reader() {
            override fun read(buffer: CharArray, offset: Int, length: Int): Int {
                val count = minOf(2, length)
                repeat(count) { buffer[offset + it] = 'x' }
                consumed += count
                return count
            }
            override fun close() = Unit
        }
        assertThrows(IllegalArgumentException::class.java) { source.readBoundedResponse(10) }
        assertEquals(11, consumed)
    }
}
