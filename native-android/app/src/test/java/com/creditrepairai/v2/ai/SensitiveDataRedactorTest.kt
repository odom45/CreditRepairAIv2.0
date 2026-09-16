package com.creditrepairai.v2.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SensitiveDataRedactorTest {
    @Test
    fun redactsHighRiskIdentifiersAndPreservesLastFour() {
        val input = "DOB: 01/02/1980 SSN 123-45-6789 email me@example.com phone (212) 555-0100 account number 998877665544"
        val output = SensitiveDataRedactor.redact(input)

        assertFalse(output.contains("01/02/1980"))
        assertFalse(output.contains("123-45-6789"))
        assertFalse(output.contains("me@example.com"))
        assertFalse(output.contains("212"))
        assertTrue(output.contains("••••5544"))
        assertFalse(SensitiveDataRedactor.containsHighRiskIdentifier(output))
    }

    @Test
    fun leavesOrdinaryCaseQuestionReadable() {
        val input = "Why do Experian and Equifax show different balances for account 1234?"
        assertEquals(input, SensitiveDataRedactor.redact(input))
    }
}
