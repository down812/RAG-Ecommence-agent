package com.jschaofan.ragagent.data.remote.sse

import com.jschaofan.ragagent.data.remote.dto.ChatAttachment
import com.jschaofan.ragagent.data.remote.dto.ChatStreamRequest
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class ChatSseClientTest {
    @Test
    fun `sends an empty body for text only chat`() {
        val server = MockWebServer()
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "text/event-stream")
                .setBody(
                    """
                        event:done
                        data:{"type":"done","data":null}

                    """.trimIndent(),
                ),
        )
        server.start()

        try {
            val client = ChatSseClient(
                client = OkHttpClient(),
                json = Json { ignoreUnknownKeys = true },
                baseUrl = server.url("/").toString(),
            )

            runBlocking {
                client.stream(
                    ChatStreamRequest(
                        sessionId = "session-text",
                        messageId = "message-text",
                        content = "只发送文字",
                    ),
                ).toList()
            }
            val request = server.takeRequest()

            assertEquals(null, request.getHeader("Content-Type"))
            assertEquals(0L, request.bodySize)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `sends documented request and streams all backend events`() {
        val server = MockWebServer()
        val imageFile = Files.createTempFile("chat-image", ".jpg").toFile().apply {
            writeBytes(byteArrayOf(1, 2, 3, 4))
        }
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "text/event-stream")
                .setBody(SSE_RESPONSE),
        )
        server.start()

        try {
            val client = ChatSseClient(
                client = OkHttpClient(),
                json = Json { ignoreUnknownKeys = true },
                baseUrl = server.url("/").toString(),
            )

            val events = runBlocking {
                client.stream(
                    ChatStreamRequest(
                        sessionId = "2",
                        messageId = "001",
                        content = "推荐手机",
                        files = listOf(
                            ChatAttachment(
                                file = imageFile,
                                mediaType = "image/jpeg",
                                fileName = "phone.jpg",
                            ),
                        ),
                    ),
                ).toList()
            }
            val request = server.takeRequest()

            assertEquals("POST", request.method)
            assertEquals("/ai/chat/messages", request.requestUrl?.encodedPath)
            assertEquals("2", request.requestUrl?.queryParameter("sessionId"))
            assertEquals("001", request.requestUrl?.queryParameter("messageId"))
            assertEquals("推荐手机", request.requestUrl?.queryParameter("content"))
            assertEquals("text/event-stream", request.getHeader("Accept"))
            assertTrue(request.getHeader("Content-Type")?.startsWith("multipart/form-data") == true)
            val requestBody = request.body.readUtf8()
            assertTrue(requestBody.contains("""name="files""""))
            assertTrue(requestBody.contains("""filename="phone.jpg""""))
            assertTrue(requestBody.contains("Content-Type: image/jpeg"))

            assertEquals(3, events.size)
            assertTrue(events[0] is ChatStreamEvent.Content)
            assertTrue(events[1] is ChatStreamEvent.Result)
            assertSame(ChatStreamEvent.Done, events[2])
        } finally {
            server.shutdown()
            imageFile.delete()
        }
    }

    private companion object {
        val SSE_RESPONSE = """
            event:content
            data:{"type":"content","data":"您好"}

            event:result
            data:{"type":"result","data":{"sessionId":"2","messageId":"001","responseType":"recommendation","sourcesStr":[],"sources":[],"recommendations":[]}}

            event:done
            data:{"type":"done","data":null}

        """.trimIndent()
    }
}
