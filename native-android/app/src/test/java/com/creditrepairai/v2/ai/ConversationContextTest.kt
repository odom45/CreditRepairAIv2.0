package com.creditrepairai.v2.ai

import com.creditrepairai.v2.model.ChatMessage
import org.junit.Assert.*
import org.junit.Test

class ConversationContextTest {
    @Test fun followUpContainsOnlyRecentMessagesForTheSelectedCase() {
        val chat = listOf(
            ChatMessage(fromUser = true, text = "Other dispute private details", caseScopeId = "other"),
            ChatMessage(fromUser = false, text = "Compare the reported balance with dated statements.", caseScopeId = "selected"),
            ChatMessage(fromUser = true, text = "What documents do you mean?", caseScopeId = "selected"))
        val result = ConversationContext.question("What documents do you mean?", chat, "selected")
        assertTrue(result.contains("dated statements"))
        assertFalse(result.contains("Other dispute private details"))
        assertTrue(result.endsWith("Current user question:\nWhat documents do you mean?"))
        assertEquals(1, Regex("What documents do you mean\\?").findAll(result).count())
    }
    @Test fun contextIsBoundedAndRedactedWithoutTruncatingTheCurrentQuestion() {
        val question = "Please explain this " + "q".repeat(2000)
        val chat = (0..20).map { ChatMessage(fromUser = true, text = "Fictional SSN 000-12-3456 " + "x".repeat(1000)) }
        val result = ConversationContext.question(question, chat, null)
        assertTrue(result.length <= 4000)
        assertTrue(result.endsWith(question))
        assertFalse(result.contains("000-12-3456"))
        assertEquals("q".repeat(4000), ConversationContext.question("q".repeat(4000), chat, null))
    }
}
