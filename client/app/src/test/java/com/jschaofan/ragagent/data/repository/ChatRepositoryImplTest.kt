package com.jschaofan.ragagent.data.repository

import com.jschaofan.ragagent.core.network.ApiEnvelope
import com.jschaofan.ragagent.data.remote.api.ChatApi
import com.jschaofan.ragagent.data.remote.dto.ChatResultDto
import com.jschaofan.ragagent.data.remote.dto.ChatSessionMessageDto
import com.jschaofan.ragagent.data.remote.dto.ChatSessionSummaryDto
import com.jschaofan.ragagent.data.remote.dto.ChatStreamRequest
import com.jschaofan.ragagent.data.remote.dto.EvaluateDto
import com.jschaofan.ragagent.data.remote.dto.EvaluateRequestDto
import com.jschaofan.ragagent.data.remote.dto.RecommendedProductDto
import com.jschaofan.ragagent.data.remote.dto.StreamErrorDto
import com.jschaofan.ragagent.data.remote.sse.ChatSseException
import com.jschaofan.ragagent.data.remote.sse.ChatStreamDataSource
import com.jschaofan.ragagent.data.remote.sse.ChatStreamEvent
import com.jschaofan.ragagent.domain.chat.model.ChatStructuredResult
import com.jschaofan.ragagent.domain.chat.repository.ChatFailureType
import com.jschaofan.ragagent.domain.chat.repository.ChatOperationResult
import com.jschaofan.ragagent.domain.chat.repository.ChatRepositoryEvent
import com.jschaofan.ragagent.domain.chat.repository.OutgoingChatAttachment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.IOException

class ChatRepositoryImplTest {
    @Test
    fun `creates ids and maps a complete streaming response`() {
        val dataSource = RecordingStreamDataSource(
            flowOf(
                ChatStreamEvent.Content("您好"),
                ChatStreamEvent.Content("，为您推荐"),
                ChatStreamEvent.Result(
                    ChatResultDto(
                        sessionId = "session-1",
                        messageId = "request-2",
                        responseType = "recommendation",
                        answer = "完整回答",
                        recommendations = listOf(
                            RecommendedProductDto(
                                productId = 1L,
                                productName = "测试商品",
                                reason = "符合需求",
                                applicableScenario = "日常使用",
                            ),
                        ),
                    ),
                ),
                ChatStreamEvent.Done,
            ),
        )
        val repository = createRepository(
            dataSource = dataSource,
            ids = ArrayDeque(listOf("session-1", "request-2")),
        )
        val sessionId = repository.createSessionId()

        val stream = repository.streamMessage(
            sessionId = sessionId,
            content = "推荐商品",
        )
        val events = runBlocking { stream.events.toList() }

        assertEquals("session-1", sessionId)
        assertEquals("request-2", stream.requestId)
        assertEquals("session-1", dataSource.lastRequest?.sessionId)
        assertEquals("request-2", dataSource.lastRequest?.messageId)
        assertEquals("推荐商品", dataSource.lastRequest?.content)
        assertTrue(events[0] is ChatRepositoryEvent.Content)
        assertTrue(events[1] is ChatRepositoryEvent.Content)
        assertTrue(events[2] is ChatRepositoryEvent.Result)
        assertTrue(events[3] is ChatRepositoryEvent.Done)

        val result = (events[2] as ChatRepositoryEvent.Result).value
        assertTrue(result is ChatStructuredResult.Recommendation)
        assertEquals(
            1L,
            (result as ChatStructuredResult.Recommendation).products.first().productId,
        )
    }

    @Test
    fun `maps outgoing attachments without exposing remote dto to caller`() {
        val file = File.createTempFile("chat-repository", ".jpg")
        val dataSource = RecordingStreamDataSource(flowOf(ChatStreamEvent.Done))
        val repository = createRepository(
            dataSource = dataSource,
            ids = ArrayDeque(listOf("request-1")),
        )

        try {
            repository.streamMessage(
                sessionId = "session-1",
                content = "识别图片",
                attachments = listOf(
                    OutgoingChatAttachment(
                        file = file,
                        mediaType = "image/jpeg",
                        fileName = "product.jpg",
                    ),
                ),
            )

            val attachment = dataSource.lastRequest?.files?.single()
            assertEquals(file, attachment?.file)
            assertEquals("image/jpeg", attachment?.mediaType)
            assertEquals("product.jpg", attachment?.fileName)
        } finally {
            file.delete()
        }
    }

    @Test
    fun `maps backend and transport errors to domain failures`() {
        val backendRepository = createRepository(
            dataSource = RecordingStreamDataSource(
                flowOf(
                    ChatStreamEvent.Error(
                        StreamErrorDto(
                            code = "MODEL_TIMEOUT",
                            message = "模型超时",
                            retryable = true,
                        ),
                    ),
                    // Repository 收到 Error 后必须终止，不能再把 Done 交给 ViewModel。
                    ChatStreamEvent.Done,
                ),
            ),
            ids = ArrayDeque(listOf("request-backend")),
        )
        val unauthorizedRepository = createRepository(
            dataSource = RecordingStreamDataSource(
                flow { throw ChatSseException.Http(401) },
            ),
            ids = ArrayDeque(listOf("request-401")),
        )
        val networkRepository = createRepository(
            dataSource = RecordingStreamDataSource(
                flow { throw IOException("offline") },
            ),
            ids = ArrayDeque(listOf("request-network")),
        )

        val backendEvent = runBlocking {
            backendRepository.streamMessage("session", "问题").events.toList().single()
        } as ChatRepositoryEvent.Error
        val unauthorizedEvent = runBlocking {
            unauthorizedRepository.streamMessage("session", "问题").events.toList().single()
        } as ChatRepositoryEvent.Error
        val networkEvent = runBlocking {
            networkRepository.streamMessage("session", "问题").events.toList().single()
        } as ChatRepositoryEvent.Error

        assertEquals(ChatFailureType.SERVER, backendEvent.failure.type)
        assertEquals("MODEL_TIMEOUT", backendEvent.failure.code)
        assertTrue(backendEvent.failure.retryable)
        assertEquals(ChatFailureType.UNAUTHORIZED, unauthorizedEvent.failure.type)
        assertEquals(ChatFailureType.NETWORK, networkEvent.failure.type)
        assertTrue(networkEvent.failure.retryable)
    }

    @Test
    fun `preserves unknown stream events`() {
        val repository = createRepository(
            dataSource = RecordingStreamDataSource(
                flowOf(
                    ChatStreamEvent.Unknown(
                        event = "heartbeat",
                        type = "future_event",
                        rawData = "{}",
                    ),
                ),
            ),
            ids = ArrayDeque(listOf("request-unknown")),
        )

        val event = runBlocking {
            repository.streamMessage("session", "问题").events.toList().single()
        }

        assertTrue(event is ChatRepositoryEvent.Unknown)
        event as ChatRepositoryEvent.Unknown
        assertEquals("future_event", event.type)
        assertEquals("{}", event.rawData)
    }

    @Test
    fun `returns success or business failure when stopping message`() {
        val successApi = FakeChatApi(ApiEnvelope(code = 0, data = "stopped"))
        val failureApi = FakeChatApi(ApiEnvelope(code = 4001, msg = "消息不存在"))
        val successRepository = createRepository(chatApi = successApi)
        val failureRepository = createRepository(chatApi = failureApi)

        val success = runBlocking {
            successRepository.stopMessage("session-1", "message-1")
        }
        val failure = runBlocking {
            failureRepository.stopMessage("session-2", "message-2")
        }

        assertTrue(success is ChatOperationResult.Success)
        assertTrue(failure is ChatOperationResult.Failure)
        failure as ChatOperationResult.Failure
        assertEquals(ChatFailureType.SERVER, failure.failure.type)
        assertEquals("4001", failure.failure.code)
        assertEquals("消息不存在", failure.failure.message)
        assertEquals("session-2", failureApi.lastSessionId)
        assertEquals("message-2", failureApi.lastMessageId)
    }

    @Test
    fun `uses a unique uuid generator by default`() {
        val repository = ChatRepositoryImpl(
            streamDataSource = RecordingStreamDataSource(flowOf(ChatStreamEvent.Done)),
            chatApi = FakeChatApi(ApiEnvelope(code = 0)),
        )

        val first = repository.createSessionId()
        val second = repository.createSessionId()

        assertNotEquals(first, second)
        assertEquals(36, first.length)
        assertEquals(36, second.length)
    }

    private fun createRepository(
        dataSource: RecordingStreamDataSource = RecordingStreamDataSource(
            flowOf(ChatStreamEvent.Done),
        ),
        chatApi: ChatApi = FakeChatApi(ApiEnvelope(code = 0)),
        ids: ArrayDeque<String> = ArrayDeque(listOf("request-default")),
    ): ChatRepositoryImpl {
        return ChatRepositoryImpl(
            streamDataSource = dataSource,
            chatApi = chatApi,
            idGenerator = ChatIdGenerator { ids.removeFirst() },
        )
    }

    private class RecordingStreamDataSource(
        private val events: Flow<ChatStreamEvent>,
    ) : ChatStreamDataSource {
        var lastRequest: ChatStreamRequest? = null

        override fun stream(request: ChatStreamRequest): Flow<ChatStreamEvent> {
            lastRequest = request
            return events
        }
    }

    private class FakeChatApi(
        private val response: ApiEnvelope<String>? = null,
        private val throwable: Throwable? = null,
    ) : ChatApi {
        var lastSessionId: String? = null
        var lastMessageId: String? = null

        override suspend fun submitEvaluation(
            request: EvaluateRequestDto,
        ): ApiEnvelope<EvaluateDto> = error("Unused in this test")

        override suspend fun getSessions(): ApiEnvelope<List<ChatSessionSummaryDto>> =
            error("Unused in this test")

        override suspend fun getSession(
            sessionId: String,
        ): ApiEnvelope<List<ChatSessionMessageDto>> = error("Unused in this test")

        override suspend fun deleteSession(sessionId: String): ApiEnvelope<String> =
            error("Unused in this test")

        override suspend fun stopChat(
            sessionId: String,
            messageId: String,
        ): ApiEnvelope<String> {
            lastSessionId = sessionId
            lastMessageId = messageId
            throwable?.let { throw it }
            return checkNotNull(response)
        }
    }
}
