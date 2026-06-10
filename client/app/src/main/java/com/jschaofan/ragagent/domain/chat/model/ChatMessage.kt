package com.jschaofan.ragagent.domain.chat.model

/**
 * 聊天页面使用的消息模型，不依赖 Retrofit、DTO 或 Compose。
 *
 * 用户消息和对应的 AI 回复拥有不同的 [id]，但共享同一个 [requestId]，
 * 这样可以把一轮问答关联起来，并使用 requestId 调用后端停止或评价接口。
 */
data class ChatMessage(
    val id: String,
    val requestId: String,
    val sender: MessageSender,
    val content: String,
    val imagePaths: List<String> = emptyList(),
    val status: MessageStatus,
    val structuredResult: ChatStructuredResult? = null,
    val error: ChatMessageError? = null,
    val createdAtEpochMillis: Long,
) {
    /**
     * 流式文本必须原样追加，不能 trim，否则后端分片中的空格可能丢失。
     */
    fun appendContent(chunk: String): ChatMessage = copy(content = content + chunk)
}

data class ChatMessageError(
    val message: String,
    val code: String? = null,
    val retryable: Boolean = false,
)
