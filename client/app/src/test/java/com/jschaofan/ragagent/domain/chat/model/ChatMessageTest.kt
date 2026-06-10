package com.jschaofan.ragagent.domain.chat.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatMessageTest {
    @Test
    fun `appends streaming chunks without changing whitespace`() {
        val message = ChatMessage(
            id = "assistant-message",
            requestId = "request-1",
            sender = MessageSender.ASSISTANT,
            content = "OPPO Reno",
            status = MessageStatus.STREAMING,
            createdAtEpochMillis = 1000L,
        )

        val updated = message
            .appendContent(" 16 Pro")
            .appendContent("\n价格：3299元")

        assertEquals("OPPO Reno 16 Pro\n价格：3299元", updated.content)
        assertEquals(MessageStatus.STREAMING, updated.status)
        assertEquals("request-1", updated.requestId)
    }
}
