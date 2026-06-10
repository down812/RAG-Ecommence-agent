package com.jschaofan.ragagent.ui.chat.model

import com.jschaofan.ragagent.domain.chat.model.ChatMessage
import java.io.File

data class PreparedChatImage(
    val id: String,
    val file: File,
    val mediaType: String,
    val fingerprint: String,
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
    val pageError: String? = null,
) {
    val canSend: Boolean
        get() = (inputText.isNotBlank() || selectedImages.isNotEmpty()) &&
            !isGenerating &&
            !isPreparingImage
}
