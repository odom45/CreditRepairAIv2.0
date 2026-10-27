package com.creditrepairai.v2.letters

import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.OutputStream

/** US-letter pages. Bounded input, word wrapping, explicit pagination and signature space. */
object LetterPdfRenderer {
    fun write(output: OutputStream, letter: String, includePage: (Int) -> Boolean = { true }): Int {
        require(letter.isNotBlank() && letter.length <= 20000) { "Letter is empty or exceeds 20,000 characters." }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 11f }
        val lines = mutableListOf<String>()
        for (paragraph in letter.replace("\r\n", "\n").replace('\r', '\n').split('\n')) {
            var rest = paragraph
            if (rest.isEmpty()) lines.add("")
            while (rest.isNotEmpty()) {
                val fit = paint.breakText(rest, true, 504f, null).coerceAtLeast(1)
                val end = if (fit < rest.length) rest.lastIndexOf(' ', fit).takeIf { it > 0 } ?: fit else fit
                lines.add(rest.take(end)); rest = rest.drop(end).trimStart()
            }
        }
        val pages = lines.chunked(40)
        val document = PdfDocument()
        try {
            for ((index, pageLines) in pages.withIndex()) {
                if (!includePage(index)) continue
                val page = document.startPage(PdfDocument.PageInfo.Builder(612, 792, index + 1).create())
                page.canvas.drawColor(Color.WHITE)
                pageLines.forEachIndexed { line, text -> page.canvas.drawText(text, 54f, 66f + 16f * line, paint) }
                page.canvas.drawText("${index + 1} / ${pages.size}", 540f, 754f, paint)
                document.finishPage(page)
            }
            document.writeTo(output)
        } finally { document.close() }
        return pages.size
    }
}
