package com.jschaofan.ragagent.data.repository

import com.jschaofan.ragagent.data.mapper.toDomain
import com.jschaofan.ragagent.data.remote.api.ChatApi
import com.jschaofan.ragagent.data.remote.dto.ChatAttachment
import com.jschaofan.ragagent.data.remote.dto.ChatStreamRequest
import com.jschaofan.ragagent.data.remote.dto.EvaluateRequestDto
import com.jschaofan.ragagent.domain.chat.model.ChatEvaluation
import com.jschaofan.ragagent.data.remote.sse.ChatSseException
import com.jschaofan.ragagent.data.remote.sse.ChatStreamDataSource
import com.jschaofan.ragagent.data.remote.sse.ChatStreamEvent
import com.jschaofan.ragagent.domain.chat.repository.ChatFailure
import com.jschaofan.ragagent.domain.chat.repository.ChatDataResult
import com.jschaofan.ragagent.domain.chat.repository.ChatFailureType
import com.jschaofan.ragagent.domain.chat.repository.ChatOperationResult
import com.jschaofan.ragagent.domain.chat.repository.ChatRepository
import com.jschaofan.ragagent.domain.chat.repository.ChatRepositoryEvent
import com.jschaofan.ragagent.domain.chat.repository.ChatStream
import com.jschaofan.ragagent.domain.chat.repository.OutgoingChatAttachment
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.transformWhile
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import java.util.UUID

/**
 * ChatRepository 的默认实现，负责连接领域层与远程接口。
 */
class ChatRepositoryImpl(
    private val streamDataSource: ChatStreamDataSource,
    private val chatApi: ChatApi,
    private val idGenerator: ChatIdGenerator = UuidChatIdGenerator,
) : ChatRepository {
    override fun createSessionId(): String = idGenerator.createId()

    override fun streamMessage(
        sessionId: String,
        content: String,
        attachments: List<OutgoingChatAttachment>,
    ): ChatStream {
        val requestId = idGenerator.createId()
        val request = ChatStreamRequest(
            sessionId = sessionId,
            messageId = requestId,
            content = content,
            files = attachments.map { attachment -> attachment.toRemote() },
        )

        val events = streamDataSource.stream(request)
            .transformWhile { event ->
                val repositoryEvent = event.toRepositoryEvent()
                emit(repositoryEvent)
                // Error 和 Done 都表示本轮回答结束，避免后续事件覆盖最终消息状态。
                repositoryEvent !is ChatRepositoryEvent.Error &&
                    repositoryEvent !is ChatRepositoryEvent.Done
            }
            .catch { throwable ->
                // 协程取消必须继续向上传递，才能真正关闭底层 SSE 连接。
                if (throwable is CancellationException) throw throwable
                emit(ChatRepositoryEvent.Error(throwable.toChatFailure()))
            }

        return ChatStream(
            requestId = requestId,
            events = events,
        )
    }

    override suspend fun stopMessage(
        sessionId: String,
        requestId: String,
    ): ChatOperationResult {
        return try {
            val response = chatApi.stopChat(
                sessionId = sessionId,
                messageId = requestId,
            )
            if (response.code == SUCCESS_CODE) {
                ChatOperationResult.Success
            } else {
                ChatOperationResult.Failure(
                    ChatFailure(
                        type = ChatFailureType.SERVER,
                        message = response.msg.orEmpty().ifBlank { DEFAULT_SERVER_ERROR },
                        code = response.code.toString(),
                    ),
                )
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            ChatOperationResult.Failure(throwable.toChatFailure())
        }
    }

    override suspend fun getSessions() = executeDataRequest {
        chatApi.getSessions().toDataResult { sessions -> sessions.map { it.toDomain() } }
    }

    override suspend fun getSessionMessages(sessionId: String) = executeDataRequest {
        chatApi.getSession(sessionId).toDataResult { messages -> messages.map { it.toDomain() } }
    }

    override suspend fun deleteSession(sessionId: String): ChatOperationResult {
        return try {
            val response = chatApi.deleteSession(sessionId)
            if (response.code == SUCCESS_CODE) ChatOperationResult.Success else response.toOperationFailure()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            ChatOperationResult.Failure(throwable.toChatFailure())
        }
    }

    override suspend fun submitEvaluation(
        sessionId: String,
        messageId: String,
        rating: Int,
        comment: String?,
    ) = executeDataRequest {
        chatApi.submitEvaluation(
            EvaluateRequestDto(
                sessionId = sessionId,
                messageId = messageId,
                rating = rating,
                comment = comment?.trim()?.takeIf(String::isNotEmpty),
            ),
        ).toDataResult { evaluation ->
            ChatEvaluation(
                messageId = evaluation.messageId,
                rating = evaluation.rating,
                comment = evaluation.comment,
            )
        }
    }

    private suspend fun <T> executeDataRequest(block: suspend () -> ChatDataResult<T>): ChatDataResult<T> =
        try {
            block()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            ChatDataResult.Failure(throwable.toChatFailure())
        }

    private fun <T, R> com.jschaofan.ragagent.core.network.ApiEnvelope<T>.toDataResult(
        transform: (T) -> R,
    ): ChatDataResult<R> = if (code == SUCCESS_CODE && data != null) {
        ChatDataResult.Success(transform(data))
    } else {
        ChatDataResult.Failure(serverFailure())
    }

    private fun com.jschaofan.ragagent.core.network.ApiEnvelope<*>.toOperationFailure() =
        ChatOperationResult.Failure(serverFailure())

    private fun com.jschaofan.ragagent.core.network.ApiEnvelope<*>.serverFailure() = ChatFailure(
        type = ChatFailureType.SERVER,
        message = msg.orEmpty().ifBlank { DEFAULT_SERVER_ERROR },
        code = code.toString(),
    )

    private fun ChatStreamEvent.toRepositoryEvent(): ChatRepositoryEvent = when (this) {
        is ChatStreamEvent.Content -> ChatRepositoryEvent.Content(text)
        is ChatStreamEvent.Result -> ChatRepositoryEvent.Result(value.toDomain())
        is ChatStreamEvent.Error -> ChatRepositoryEvent.Error(
            ChatFailure(
                type = ChatFailureType.SERVER,
                message = value.message,
                code = value.code,
                retryable = value.retryable,
            ),
        )

        is ChatStreamEvent.Unknown -> ChatRepositoryEvent.Unknown(
            type = type,
            rawData = rawData,
        )

        ChatStreamEvent.Done -> ChatRepositoryEvent.Done
    }

    private fun Throwable.toChatFailure(): ChatFailure = when (this) {
        is ChatSseException.Http -> {
            if (statusCode == HTTP_UNAUTHORIZED) {
                ChatFailure(
                    type = ChatFailureType.UNAUTHORIZED,
                    message = "登录状态已失效，请重新登录",
                    code = statusCode.toString(),
                )
            } else {
                ChatFailure(
                    type = ChatFailureType.SERVER,
                    message = "服务请求失败（HTTP $statusCode）",
                    code = statusCode.toString(),
                    retryable = statusCode >= HTTP_SERVER_ERROR_START,
                )
            }
        }

        ChatSseException.EmptyBody -> ChatFailure(
            type = ChatFailureType.INVALID_RESPONSE,
            message = "服务返回了空响应",
            retryable = true,
        )

        is HttpException -> {
            if (code() == HTTP_UNAUTHORIZED) {
                ChatFailure(
                    type = ChatFailureType.UNAUTHORIZED,
                    message = "登录状态已失效，请重新登录",
                    code = code().toString(),
                )
            } else {
                ChatFailure(
                    type = ChatFailureType.SERVER,
                    message = "服务请求失败（HTTP ${code()}）",
                    code = code().toString(),
                    retryable = code() >= HTTP_SERVER_ERROR_START,
                )
            }
        }

        is SerializationException -> ChatFailure(
            type = ChatFailureType.INVALID_RESPONSE,
            message = "服务返回的数据格式无法解析",
            retryable = true,
        )

        is IOException -> ChatFailure(
            type = ChatFailureType.NETWORK,
            message = "网络连接失败，请检查网络后重试",
            retryable = true,
        )

        else -> ChatFailure(
            type = ChatFailureType.UNKNOWN,
            message = message ?: "发生未知错误",
        )
    }

    private fun OutgoingChatAttachment.toRemote() = ChatAttachment(
        file = file,
        mediaType = mediaType,
        fileName = fileName,
    )

    private companion object {
        const val SUCCESS_CODE = 0
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_SERVER_ERROR_START = 500
        const val DEFAULT_SERVER_ERROR = "服务处理失败"
    }
}

fun interface ChatIdGenerator {
    fun createId(): String
}

data object UuidChatIdGenerator : ChatIdGenerator {
    override fun createId(): String = UUID.randomUUID().toString()
}
