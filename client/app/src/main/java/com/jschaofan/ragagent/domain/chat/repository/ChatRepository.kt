package com.jschaofan.ragagent.domain.chat.repository

import com.jschaofan.ragagent.domain.chat.model.ChatStructuredResult
import com.jschaofan.ragagent.domain.chat.model.ChatMessage
import com.jschaofan.ragagent.domain.chat.model.ChatSession
import com.jschaofan.ragagent.domain.chat.model.ChatEvaluation
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * 聊天领域层的数据入口。
 *
 * ViewModel 只依赖这个接口，不直接接触 OkHttp、Retrofit、SSE DTO 或后端错误格式。
 */
interface ChatRepository {
    /**
     * 创建客户端会话 ID。当前协议允许客户端生成 sessionId。
     */
    fun createSessionId(): String

    /**
     * 创建并返回一条流式聊天请求。
     *
     * requestId 会立即返回，方便 ViewModel 在网络响应前就建立一轮问答消息。
     */
    fun streamMessage(
        sessionId: String,
        content: String,
        attachments: List<OutgoingChatAttachment> = emptyList(),
    ): ChatStream

    /**
     * 通知后端停止指定回答。本地 SSE 连接由调用方取消 Flow 收集来关闭。
     */
    suspend fun stopMessage(
        sessionId: String,
        requestId: String,
    ): ChatOperationResult

    suspend fun getSessions(): ChatDataResult<List<ChatSession>>

    suspend fun getSessionMessages(sessionId: String): ChatDataResult<List<ChatMessage>>

    suspend fun deleteSession(sessionId: String): ChatOperationResult

    suspend fun submitEvaluation(
        sessionId: String,
        messageId: String,
        rating: Int,
        comment: String? = null,
    ): ChatDataResult<ChatEvaluation>
}

sealed interface ChatDataResult<out T> {
    data class Success<T>(val value: T) : ChatDataResult<T>
    data class Failure(val failure: ChatFailure) : ChatDataResult<Nothing>
}

data class ChatStream(
    val requestId: String,
    val events: Flow<ChatRepositoryEvent>,
)

data class OutgoingChatAttachment(
    val file: File,
    val mediaType: String,
    val fileName: String = file.name,
)

sealed interface ChatRepositoryEvent {
    data class Content(val text: String) : ChatRepositoryEvent

    data class Result(val value: ChatStructuredResult) : ChatRepositoryEvent

    data class Error(val failure: ChatFailure) : ChatRepositoryEvent

    data class Unknown(
        val type: String?,
        val rawData: String,
    ) : ChatRepositoryEvent

    data object Done : ChatRepositoryEvent
}

sealed interface ChatOperationResult {
    data object Success : ChatOperationResult

    data class Failure(val failure: ChatFailure) : ChatOperationResult
}

/**
 * Repository 对上层暴露的统一错误，不泄漏 Retrofit 或 OkHttp 异常类型。
 */
data class ChatFailure(
    val type: ChatFailureType,
    val message: String,
    val code: String? = null,
    val retryable: Boolean = false,
)

enum class ChatFailureType {
    UNAUTHORIZED,
    NETWORK,
    SERVER,
    INVALID_RESPONSE,
    UNKNOWN,
}
