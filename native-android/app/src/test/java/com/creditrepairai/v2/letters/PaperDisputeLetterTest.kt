package com.creditrepairai.v2.letters

import com.creditrepairai.v2.model.*
import com.creditrepairai.v2.guidance.FilingGuide
import org.junit.Assert.*
import org.junit.Test

class PaperDisputeLetterTest {
    private val dispute = Dispute(findingId = "synthetic", creditor = "Fictional Bank", bureaus = listOf(Bureau.EQUIFAX, Bureau.EXPERIAN), reason = "Verify balance", letter = "Synthetic draft")
    @Test fun aLetterHasOnlyTheChosenRecipientAndUserSuppliedFacts() {
        val text = PaperDisputeLetter.compose(dispute, Bureau.EQUIFAX, "Fictional Reader", "123 Test Street\nTest City, WA 00000", "October 26, 2026", "My dated statement shows a zero balance.", "Statement copy", "Investigate the reported balance.")
        assertTrue(text.contains("Equifax Information Services LLC"))
        assertTrue(text.contains("P.O. Box 740256"))
        assertFalse(text.contains("Experian"))
        assertTrue(text.contains("zero balance"))
        assertTrue(text.contains("Signature:"))
        assertFalse(text.contains("automatically delete"))
        assertTrue(runCatching { PaperDisputeLetter.recipient(Bureau.UNKNOWN) }.isFailure)
        assertTrue(runCatching { PaperDisputeLetter.compose(dispute, Bureau.TRANSUNION, "Name", "Address", "Date", "Facts", "Evidence", "Review") }.isFailure)
    }
    @Test fun postalEscalationUsesConfirmedDeliveryOrAnActualResponse() {
        val now = 100L * 86400000
        val record = MailingRecord(dispute.id, Bureau.EQUIFAX, now - 50L * 86400000)
        assertFalse(FilingGuide.cfpbReady(dispute, emptyList(), now, listOf(record)))
        assertFalse(FilingGuide.cfpbReady(dispute, emptyList(), now, listOf(record.copy(deliveredAt = now - 40L * 86400000))))
        assertTrue(FilingGuide.cfpbReady(dispute, emptyList(), now, listOf(record.copy(deliveredAt = now - 45L * 86400000))))
        assertTrue(FilingGuide.cfpbReady(dispute, emptyList(), now, listOf(record.copy(responseReceivedAt = now))))
        assertFalse(FilingGuide.cfpbReady(dispute, emptyList(), now, listOf(record.copy(disputeId = "other", responseReceivedAt = now))))
        assertEquals(listOf("EQUIFAX", "EXPERIAN"), FilingGuide.portals(dispute).map { it.name })
    }
}
