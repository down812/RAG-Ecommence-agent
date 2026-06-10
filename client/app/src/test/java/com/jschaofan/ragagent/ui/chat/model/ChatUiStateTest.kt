package com.jschaofan.ragagent.ui.chat.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatUiStateTest {
    @Test
    fun `can send only non blank input while idle`() {
        assertFalse(ChatUiState(sessionId = "1").canSend)
        assertFalse(ChatUiState(sessionId = "1", inputText = "   ").canSend)
        assertFalse(
            ChatUiState(
                sessionId = "1",
                inputText = "推荐手机",
                isGenerating = true,
            ).canSend,
        )
        assertTrue(ChatUiState(sessionId = "1", inputText = "推荐手机").canSend)
    }
}
