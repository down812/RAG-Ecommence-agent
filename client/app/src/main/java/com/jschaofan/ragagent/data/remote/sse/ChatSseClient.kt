package com.jschaofan.ragagent.data.remote.sse

import com.jschaofan.ragagent.data.remote.dto.ChatAttachment
import com.jschaofan.ragagent.data.remote.dto.ChatStreamRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException

/**
 * 发起后端聊天请求，并把 SSE 响应包装成冷 Flow。
 *
 * 调用方开始 collect 后才真正请求网络；取消 collect 时也会取消底层 OkHttp 请求，
 * 后续“停止生成”功能会依赖这一机制。
 */
class ChatSseClient(
    private val client: okhttp3.OkHttpClient,
    private val json: Json,
    baseUrl: String,
) : ChatStreamDataSource {
    private val chatUrl = baseUrl.ensureTrailingSlash()
        .toHttpUrl()
        .newBuilder()
        .addPathSegments(CHAT_PATH)
        .build()

    override fun stream(request: ChatStreamRequest): Flow<ChatStreamEvent> = callbackFlow {
        // 当前后端协议要求把会话信息放在 Query 参数中。
        val url = chatUrl.newBuilder()
            .addQueryParameter("sessionId", request.sessionId)
            .addQueryParameter("messageId", request.messageId)
            .addQueryParameter("content", request.content)
            .build()
        val body = buildMultipartBody(request.files)
        val httpRequest = Request.Builder()
            .url(url)
            .post(body)
            .header("Accept", SSE_MEDIA_TYPE)
            .header("Cache-Control", "no-cache")
            .build()
        val call = client.newCall(httpRequest)

        call.enqueue(
            object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (!call.isCanceled()) {
                        close(e)
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (!response.isSuccessful) {
                            close(ChatSseException.Http(response.code))
                            return
                        }

                        val source = response.body?.source()
                        if (source == null) {
                            close(ChatSseException.EmptyBody)
                            return
                        }

                        val parser = SseFrameParser()
                        val decoder = ChatStreamDecoder(json)

                        try {
                            // SSE 连接会持续保持，因此需要边读取、边分帧、边发送给上层。
                            while (!source.exhausted() && !call.isCanceled()) {
                                parser.acceptLine(source.readUtf8LineStrict())?.let { frame ->
                                    trySend(decoder.decode(frame))
                                }
                            }
                            parser.finish()?.let { frame ->
                                trySend(decoder.decode(frame))
                            }
                            close()
                        } catch (exception: Exception) {
                            if (!call.isCanceled()) {
                                close(exception)
                            }
                        }
                    }
                }
            },
        )

        awaitClose {
            // Flow 关闭时必须释放底层连接，不能只停止 UI 更新。
            call.cancel()
        }
    }

    private fun buildMultipartBody(files: List<ChatAttachment>): RequestBody {
        if (files.isEmpty()) {
            // files 是选填参数：纯文本聊天不发送 multipart，也不携带空 files 字段。
            return ByteArray(0).toRequestBody()
        }

        return MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .apply {
                files.forEach { attachment ->
                    addFormDataPart(
                        FILES_PART_NAME,
                        attachment.fileName,
                        attachment.file.asRequestBody(attachment.mediaType.toMediaType()),
                    )
                }
            }
            .build()
    }

    private fun String.ensureTrailingSlash(): String = if (endsWith('/')) this else "$this/"

    private companion object {
        const val CHAT_PATH = "ai/chat/messages"
        const val FILES_PART_NAME = "files"
        const val SSE_MEDIA_TYPE = "text/event-stream"
    }
}
