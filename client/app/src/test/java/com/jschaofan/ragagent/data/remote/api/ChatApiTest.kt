package com.jschaofan.ragagent.data.remote.api

import com.jschaofan.ragagent.core.network.MutableTokenProvider
import com.jschaofan.ragagent.core.network.NetworkModule
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatApiTest {
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
