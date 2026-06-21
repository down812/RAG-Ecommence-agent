package com.jschaofan.ragagent.data.remote.api

import com.jschaofan.ragagent.core.network.MutableTokenProvider
import com.jschaofan.ragagent.core.network.NetworkModule
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatApiTest {
    @Test
    fun `submits message evaluation`() {
        val server = MockWebServer()
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """{"code":0,"data":{"id":1,"sessionId":"session-1","messageId":"message-1","rating":-1,"comment":"不够准确"}}""",
                ),
        )
        server.start()

        try {
            val api = NetworkModule(
                tokenProvider = MutableTokenProvider("test-token"),
                baseUrl = server.url("/").toString(),
                isDebug = false,
            ).chatApi
            val result = runBlocking {
                api.submitEvaluation(
                    com.jschaofan.ragagent.data.remote.dto.EvaluateRequestDto(
                        sessionId = "session-1",
                        messageId = "message-1",
                        rating = -1,
                        comment = "不够准确",
                    ),
                )
            }
            val request = server.takeRequest()

            assertEquals("POST", request.method)
            assertEquals("/evaluate/add", request.requestUrl?.encodedPath)
            assertEquals(-1, result.data?.rating)
            assertTrue(request.body.readUtf8().contains("message-1"))
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `loads and deletes chat history endpoints`() {
        val server = MockWebServer()
        server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody(
            """{"code":0,"data":[{"sessionId":"session-1","title":"推荐耳机","createdAt":1000}]}""",
        ))
        server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody(
            """{"code":0,"data":[{"sessionId":"session-1","messageId":"message-1","content":"推荐耳机","messageType":"USER","createdAt":1000}]}""",
        ))
        server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody(
            """{"code":0,"data":"deleted"}""",
        ))
        server.start()

        try {
            val api = NetworkModule(
                tokenProvider = MutableTokenProvider("test-token"),
                baseUrl = server.url("/").toString(),
                isDebug = false,
            ).chatApi

            val sessions = runBlocking { api.getSessions() }
            val messages = runBlocking { api.getSession("session-1") }
            val deleted = runBlocking { api.deleteSession("session-1") }

            assertEquals("session-1", sessions.data?.single()?.sessionId)
            assertEquals("message-1", messages.data?.single()?.messageId)
            assertEquals("deleted", deleted.data)
            assertEquals("/ai/chat/sessions", server.takeRequest().requestUrl?.encodedPath)
            assertEquals("GET", server.takeRequest().method)
            assertEquals("DELETE", server.takeRequest().method)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `stops the documented session message pair`() {
        val server = MockWebServer()
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""{"code":0,"msg":"","data":"stopped"}"""),
        )
        server.start()

        try {
            val networkModule = NetworkModule(
                tokenProvider = MutableTokenProvider("test-token"),
                baseUrl = server.url("/").toString(),
                isDebug = false,
            )

            val result = runBlocking {
                networkModule.chatApi.stopChat(
                    sessionId = "session-1",
                    messageId = "message-1",
                )
            }
            val request = server.takeRequest()

            assertEquals("POST", request.method)
            assertEquals(
                "/ai/chat/messages/session-1/message-1/stop",
                request.requestUrl?.encodedPath,
            )
            assertEquals("test-token", request.getHeader("token"))
            assertEquals(0, result.code)
            assertEquals("stopped", result.data)
        } finally {
            server.shutdown()
        }
    }
}
