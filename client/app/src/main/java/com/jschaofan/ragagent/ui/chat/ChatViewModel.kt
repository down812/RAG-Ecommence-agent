package com.jschaofan.ragagent.ui.chat

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jschaofan.ragagent.domain.chat.model.ChatMessage
import com.jschaofan.ragagent.domain.chat.model.ChatMessageError
import com.jschaofan.ragagent.domain.chat.model.MessageSender
import com.jschaofan.ragagent.domain.chat.model.MessageStatus
import com.jschaofan.ragagent.domain.chat.repository.ChatFailure
import com.jschaofan.ragagent.domain.chat.repository.ChatFailureType
import com.jschaofan.ragagent.domain.chat.repository.ChatRepository
import com.jschaofan.ragagent.domain.chat.repository.ChatRepositoryEvent
import com.jschaofan.ragagent.domain.chat.repository.OutgoingChatAttachment
import com.jschaofan.ragagent.ui.chat.model.ChatUiState
import com.jschaofan.ragagent.ui.chat.model.PreparedChatImage
import com.jschaofan.ragagent.ui.chat.media.CameraCaptureTarget
import com.jschaofan.ragagent.ui.chat.media.ImageAttachmentProcessor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/**
 * 管理聊天页面状态，并把 Repository 的流式事件转换成消息列表变化。
 *
 * ViewModel 不解析 SSE，也不接触网络 DTO，只负责一轮对话在界面上的生命周期。
 */
class ChatViewModel(
    private val repository: ChatRepository,
    private val imageAttachmentProcessor: ImageAttachmentProcessor,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        ChatUiState(sessionId = repository.createSessionId()),
    )
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var streamJob: Job? = null

    fun onInputChanged(value: String) {
        _uiState.update { state ->
            state.copy(
                inputText = value,
                pageError = null,
            )
        }
    }

    fun createCameraTarget(): CameraCaptureTarget =
        imageAttachmentProcessor.createCameraTarget()

    fun onImagesSelected(uris: List<Uri>) {
        val availableCount = MAX_IMAGE_COUNT - _uiState.value.selectedImages.size
        if (availableCount <= 0) {
            showPageError("最多只能选择 $MAX_IMAGE_COUNT 张图片")
            return
        }
        val selectedUris = uris.take(availableCount)
        if (selectedUris.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { state -> state.copy(isPreparingImage = true, pageError = null) }
            selectedUris.forEach { uri ->
                imageAttachmentProcessor.prepare(uri)
                    .onSuccess { preparedImage ->
                        addPreparedImage(preparedImage)
                    }
                    .onFailure { error ->
                        showPageError(error.message ?: "图片处理失败，请重新选择")
                    }
            }
            _uiState.update { state -> state.copy(isPreparingImage = false) }
        }
    }

    fun onCameraResult(
        target: CameraCaptureTarget,
        success: Boolean,
    ) {
        if (!success) {
            imageAttachmentProcessor.discardCameraTarget(target)
            return
        }

        viewModelScope.launch {
            _uiState.update { state -> state.copy(isPreparingImage = true, pageError = null) }
            imageAttachmentProcessor.prepare(target.uri)
                .onSuccess { preparedImage ->
                    addPreparedImage(preparedImage)
                }
                .onFailure { error ->
                    showPageError(error.message ?: "照片处理失败，请重新拍摄")
                }
            imageAttachmentProcessor.discardCameraTarget(target)
            _uiState.update { state -> state.copy(isPreparingImage = false) }
        }
    }

    fun removeImage(imageId: String) {
        val removedImage = _uiState.value.selectedImages.firstOrNull { it.id == imageId }
        removedImage?.file?.delete()
        _uiState.update { state ->
            state.copy(
                selectedImages = state.selectedImages.filterNot { it.id == imageId },
            )
        }
    }

    fun onCameraUnavailable(target: CameraCaptureTarget?) {
        target?.let(imageAttachmentProcessor::discardCameraTarget)
        showPageError("当前设备没有可用的相机应用")
    }

    /**
     * 用压缩文件指纹去重，并在加入状态前再次检查数量，防止异步处理造成越界。
     */
    private fun addPreparedImage(image: PreparedChatImage) {
        val state = _uiState.value
        when {
            state.selectedImages.any { it.fingerprint == image.fingerprint } -> {
                image.file.delete()
                showPageError("这张图片已经选择过了")
            }

            state.selectedImages.size >= MAX_IMAGE_COUNT -> {
                image.file.delete()
                showPageError("最多只能选择 $MAX_IMAGE_COUNT 张图片")
            }

            else -> {
                _uiState.update { current ->
                    current.copy(selectedImages = current.selectedImages + image)
                }
            }
        }
    }

    /**
     * 发送当前输入。用户消息立即加入列表，AI 占位消息随后接收流式分片。
     */
    fun sendMessage(
        attachments: List<OutgoingChatAttachment> = emptyList(),
    ) {
        val state = _uiState.value
        val content = state.inputText.trim().ifBlank {
            if (state.selectedImages.isNotEmpty()) IMAGE_SEARCH_PROMPT else ""
        }
        if (
            content.isEmpty() ||
            state.isGenerating ||
            state.isPreparingImage
        ) {
            return
        }
        val imageAttachments = state.selectedImages.map { image ->
            OutgoingChatAttachment(
                file = image.file,
                mediaType = image.mediaType,
            )
        }

        startMessage(
            content = content,
            attachments = attachments + imageAttachments,
            appendUserMessage = true,
            displayImagePaths = state.selectedImages.map { it.file.absolutePath },
        )
    }

    /**
     * 失败重试复用原用户问题，并在原 AI 消息位置重新生成，避免重复展示用户消息。
     */
    fun retryMessage(assistantMessageId: String) {
        val state = _uiState.value
        if (state.isGenerating) return

        val failedMessage = state.messages.firstOrNull { message ->
            message.id == assistantMessageId &&
                message.sender == MessageSender.ASSISTANT &&
                message.status == MessageStatus.FAILED &&
                message.error?.retryable == true
        } ?: return
        val userMessage = state.messages.lastOrNull { message ->
            message.requestId == failedMessage.requestId &&
                message.sender == MessageSender.USER
        } ?: return

        startMessage(
            content = userMessage.content,
            attachments = userMessage.imagePaths.map { path ->
                OutgoingChatAttachment(
                    file = File(path),
                    mediaType = IMAGE_MEDIA_TYPE,
                )
            },
            appendUserMessage = false,
            reusedAssistantMessageId = assistantMessageId,
        )
    }

    private fun startMessage(
        content: String,
        attachments: List<OutgoingChatAttachment>,
        appendUserMessage: Boolean,
        reusedAssistantMessageId: String? = null,
        displayImagePaths: List<String> = emptyList(),
    ) {
        val state = _uiState.value
        val stream = repository.streamMessage(
            sessionId = state.sessionId,
            content = content,
            attachments = attachments,
        )
        val requestId = stream.requestId
        val assistantMessageId = reusedAssistantMessageId ?: "$requestId-assistant"
        val conversationRequestId = reusedAssistantMessageId
            ?.let { id -> state.messages.firstOrNull { it.id == id }?.requestId }
            ?: requestId
        val now = System.currentTimeMillis()
        val assistantMessage = ChatMessage(
            id = assistantMessageId,
            // 重试时保留原问答关联 ID；真正用于停止后端任务的是 activeRequestId。
            requestId = conversationRequestId,
            sender = MessageSender.ASSISTANT,
            content = "",
            status = MessageStatus.STREAMING,
            createdAtEpochMillis = now,
        )

        _uiState.update { current ->
            val updatedMessages = if (appendUserMessage) {
                val userMessage = ChatMessage(
                    id = "$requestId-user",
                    requestId = requestId,
                    sender = MessageSender.USER,
                    content = content,
                    imagePaths = displayImagePaths,
                    status = MessageStatus.COMPLETED,
                    createdAtEpochMillis = now,
                )
                current.messages + userMessage + assistantMessage
            } else {
                current.messages.map { message ->
                    if (message.id == assistantMessageId) assistantMessage else message
                }
            }

            current.copy(
                messages = updatedMessages,
                inputText = "",
                selectedImages = emptyList(),
                isGenerating = true,
                activeRequestId = requestId,
                activeAssistantMessageId = assistantMessageId,
                pageError = null,
            )
        }

        streamJob = viewModelScope.launch {
            try {
                stream.events.collect { event ->
                    handleStreamEvent(
                        assistantMessageId = assistantMessageId,
                        event = event,
                    )
                }
                val latestState = _uiState.value
                if (
                    latestState.isGenerating &&
                    latestState.activeRequestId == requestId
                ) {
                    markAssistantFailed(
                        assistantMessageId = assistantMessageId,
                        failure = ChatFailure(
                            type = ChatFailureType.NETWORK,
                            message = "连接提前结束，请重试",
                            retryable = true,
                        ),
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                // Repository 通常会返回 Error 事件，这里兜底处理意外异常。
                markAssistantFailed(
                    assistantMessageId = assistantMessageId,
                    failure = ChatFailure(
                        type = ChatFailureType.UNKNOWN,
                        message = throwable.message ?: "生成回复时发生未知错误",
                        retryable = true,
                    ),
                )
            }
        }
    }

    /**
     * 先取消本地 Flow 立即停止界面更新，再通知后端终止生成任务。
     * 后端停止失败只记录日志，不显示错误提示给用户。
     */
    fun stopGenerating() {
        val state = _uiState.value
        val requestId = state.activeRequestId ?: return
        val assistantMessageId = state.activeAssistantMessageId ?: return
        if (!state.isGenerating) return

        streamJob?.cancel()
        streamJob = null
        updateMessage(assistantMessageId) { message ->
            message.copy(status = MessageStatus.STOPPED)
        }
        finishGeneration()

        viewModelScope.launch {
            runCatching {
                repository.stopMessage(
                    sessionId = state.sessionId,
                    requestId = requestId,
                )
            }.onFailure { error ->
                // 后端停止失败不影响用户侧已成功停止的体验，只记录日志
                android.util.Log.w("ChatViewModel", "Stop notification failed: ${error.message}")
            }
        }
    }

    fun clearPageError() {
        _uiState.update { state -> state.copy(pageError = null) }
    }

    private fun showPageError(message: String) {
        _uiState.update { state -> state.copy(pageError = message) }
    }

    private fun handleStreamEvent(
        assistantMessageId: String,
        event: ChatRepositoryEvent,
    ) {
        when (event) {
            is ChatRepositoryEvent.Content -> {
                updateMessage(assistantMessageId) { message ->
                    message.appendContent(event.text)
                }
            }

            is ChatRepositoryEvent.Result -> {
                updateMessage(assistantMessageId) { message ->
                    message.copy(
                        // 部分响应只返回最终 answer，流式正文为空时使用它作为降级文本。
                        content = message.content.ifBlank { event.value.answer },
                        structuredResult = event.value,
                    )
                }
            }

            is ChatRepositoryEvent.Error -> {
                markAssistantFailed(
                    assistantMessageId = assistantMessageId,
                    failure = event.failure,
                )
            }

            ChatRepositoryEvent.Done -> completeAssistantMessage(assistantMessageId)

            // 未知事件留给后续协议扩展，不改变当前消息状态。
            is ChatRepositoryEvent.Unknown -> Unit
        }
    }

    private fun completeAssistantMessage(assistantMessageId: String) {
        val message = _uiState.value.messages.firstOrNull { it.id == assistantMessageId }
        if (message == null || message.content.isBlank()) {
            markAssistantFailed(
                assistantMessageId = assistantMessageId,
                failure = ChatFailure(
                    type = ChatFailureType.INVALID_RESPONSE,
                    message = "服务没有返回有效内容，请重试",
                    retryable = true,
                ),
            )
            return
        }

        updateMessage(assistantMessageId) { current ->
            current.copy(status = MessageStatus.COMPLETED)
        }
        finishGeneration()
    }

    private fun markAssistantFailed(
        assistantMessageId: String,
        failure: ChatFailure,
    ) {
        updateMessage(assistantMessageId) { message ->
            message.copy(
                status = MessageStatus.FAILED,
                error = ChatMessageError(
                    message = failure.message,
                    code = failure.code,
                    retryable = failure.retryable,
                ),
            )
        }
        finishGeneration()
    }

    private fun finishGeneration() {
        streamJob = null
        _uiState.update { state ->
            state.copy(
                isGenerating = false,
                activeRequestId = null,
                activeAssistantMessageId = null,
            )
        }
    }

    private fun updateMessage(
        messageId: String,
        transform: (ChatMessage) -> ChatMessage,
    ) {
        _uiState.update { state ->
            state.copy(
                messages = state.messages.map { message ->
                    if (message.id == messageId) transform(message) else message
                },
            )
        }
    }

    class Factory(
        private val repository: ChatRepository,
        private val imageAttachmentProcessor: ImageAttachmentProcessor,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ChatViewModel::class.java)) {
                "Unsupported ViewModel class: ${modelClass.name}"
            }
            return ChatViewModel(
                repository = repository,
                imageAttachmentProcessor = imageAttachmentProcessor,
            ) as T
        }
    }

    override fun onCleared() {
        _uiState.value.selectedImages.forEach { image -> image.file.delete() }
        super.onCleared()
    }

    private companion object {
        const val MAX_IMAGE_COUNT = 3
        const val IMAGE_SEARCH_PROMPT = "请根据图片查找相似商品"
        const val IMAGE_MEDIA_TYPE = "image/jpeg"
    }
}
