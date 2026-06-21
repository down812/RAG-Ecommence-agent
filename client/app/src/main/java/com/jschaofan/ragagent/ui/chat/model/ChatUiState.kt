package com.jschaofan.ragagent.ui.chat.model

import com.jschaofan.ragagent.domain.chat.model.ChatMessage
import com.jschaofan.ragagent.domain.chat.model.ChatSession
import java.io.File

data class PreparedChatImage(
    val id: String,
    val file: File,
    val mediaType: String,
    val fingerprint: String,
)

data class MessageEvaluationState(
    val rating: Int? = null,
    val comment: String? = null,
    val isSubmitting: Boolean = false,
)

/**
 * 聊天页面的完整可观察状态，后续由 ChatViewModel 统一维护。
 */
data class ChatUiState(
    val sessionId: String,
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val selectedImages: List<PreparedChatImage> = emptyList(),
    val isPreparingImage: Boolean = false,
    val isGenerating: Boolean = false,
    val activeRequestId: String? = null,
    val activeAssistantMessageId: String? = null,
    val sessions: List<ChatSession> = emptyList(),
    val isLoadingSessions: Boolean = false,
    val isLoadingSession: Boolean = false,
    val deletingSessionId: String? = null,
    val evaluations: Map<String, MessageEvaluationState> = emptyMap(),
    val pageError: String? = null,
) {
    val canSend: Boolean
        get() = (inputText.isNotBlank() || selectedImages.isNotEmpty()) &&
            !isGenerating &&
            !isPreparingImage &&
            !isLoadingSession
}
