package com.creditrepairai.v2.letters

import androidx.test.core.app.ApplicationProvider
import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.creditrepairai.v2.data.AppRepository
import com.creditrepairai.v2.model.*
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream

class LetterPdfInstrumentedTest {
    @Test fun longLetterHasReadableRecipientAndLastLineAcrossMultiplePages() {
        PDFBoxResourceLoader.init(ApplicationProvider.getApplicationContext<Context>())
        val text = "Fictional Reader\nEquifax Information Services LLC\nP.O. Box 740256\nAtlanta, GA 30374\n\n" +
            (1..140).joinToString("\n") { "Fictional supporting detail $it, to verify against dated records." } + "\nFINAL-SIGNATURE-SENTINEL"
        val output = ByteArrayOutputStream()
        val pages = LetterPdfRenderer.write(output, text)
        assertTrue(pages > 1)
        PDDocument.load(output.toByteArray()).use { pdf ->
            assertEquals(pages, pdf.numberOfPages)
            val extracted = PDFTextStripper().getText(pdf)
            assertTrue(extracted.contains("P.O. Box 740256"))
            assertTrue(extracted.contains("FINAL-SIGNATURE-SENTINEL"))
        }
        val selected = ByteArrayOutputStream()
        LetterPdfRenderer.write(selected, text) { it == pages - 1 }
        PDDocument.load(selected.toByteArray()).use { pdf ->
            assertEquals(1, pdf.numberOfPages)
            assertTrue(PDFTextStripper().getText(pdf).contains("FINAL-SIGNATURE-SENTINEL"))
        }
    }
    @Test fun encryptedStoragePreservesPerBureauMailingAndConversationScope() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = AppRepository(context)
        repository.selectOwner("fictional-mail-owner")
        try {
            val state = AppState(mailings = listOf(MailingRecord("fictional-case", Bureau.EQUIFAX, 1000, "SYNTHETIC-TRACKING", 2000)),
                chat = listOf(ChatMessage(fromUser = true, text = "Fictional question", caseScopeId = "fictional-case")))
            repository.save(state)
            val restored = AppRepository(context).load()
            assertEquals(state.mailings, restored.mailings)
            assertEquals("fictional-case", restored.chat.single().caseScopeId)
        } finally { repository.clear(preserveOwner = false) }
    }

    @Test fun agentSnapshotHasMailStatusButNeverTheLocalTrackingNumber() {
        val dispute = Dispute(findingId = "fictional", creditor = "Fictional Bank", bureaus = listOf(Bureau.EQUIFAX), reason = "Fictional balance review", letter = "Local letter")
        val state = AppState(disputes = listOf(dispute), mailings = listOf(MailingRecord(dispute.id, Bureau.EQUIFAX, 1000, "PRIVATE-LOCAL-TRACKING", 2000, 3000)))
        val gateway = com.creditrepairai.v2.ai.CreditAgentGateway("", com.creditrepairai.v2.ai.SessionTokenProvider { null })
        val method = gateway.javaClass.getDeclaredMethod("toSafeAgentJson", AppState::class.java).apply { isAccessible = true }
        val json = method.invoke(gateway, state) as org.json.JSONObject
        assertFalse(json.toString().contains("PRIVATE-LOCAL-TRACKING"))
        val sent = json.getJSONArray("disputes").getJSONObject(0)
        assertEquals(1000L, sent.getLong("sentAt"))
        assertTrue(sent.getBoolean("responseReceived"))
        assertEquals("equifax", sent.getJSONArray("mailingHistory").getJSONObject(0).getString("bureau"))
    }
}
