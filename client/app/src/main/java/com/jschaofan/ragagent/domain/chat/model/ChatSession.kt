package com.jschaofan.ragagent.domain.chat.model

/**
 * 当前会话的领域模型。sessionId 由客户端生成，并在同一会话内持续复用。
 */
data class ChatSession(
    val sessionId: String,
    val title: String? = null,
    val messages: List<ChatMessage> = emptyList(),
)
