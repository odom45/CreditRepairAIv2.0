package com.creditrepairai.v2.ai

import java.io.Reader

/** Read at most the configured character limit plus one overflow sentinel. */
internal fun Reader.readBoundedResponse(limit: Int = 64_000): String {
    require(limit in 1..1_000_000)
    val buffer = CharArray(limit + 1)
    var count = 0
    while (count < buffer.size) {
        val read = read(buffer, count, buffer.size - count)
        if (read < 0) break
        if (read == 0) {
            val next = read()
            if (next < 0) break
            buffer[count++] = next.toChar()
        } else count += read
    }
    require(count <= limit) { "Secure response exceeded its size limit." }
    return String(buffer, 0, count)
}
