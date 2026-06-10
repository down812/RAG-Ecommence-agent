package com.jschaofan.ragagent.data.remote.sse

import android.util.Log
import com.jschaofan.ragagent.BuildConfig
import com.jschaofan.ragagent.data.remote.dto.ChatResultDto
import com.jschaofan.ragagent.data.remote.dto.ChatStreamEnvelope
import com.jschaofan.ragagent.data.remote.dto.StreamErrorDto
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive

/**
 * 将完整 SSE 帧中的 JSON 转换成强类型聊天事件。
 */
class ChatStreamDecoder(
    private val json: Json,
) {
    fun decode(frame: SseFrame): ChatStreamEvent {
        val envelope = try {
            json.decodeFromString<ChatStreamEnvelope>(frame.data)
        } catch (_: SerializationException) {
            // 无法识别的数据保留为 Unknown，不因此中断整条流。
            return ChatStreamEvent.Unknown(
                event = frame.event,
                type = null,
                rawData = frame.data,
            )
        }

        return when (envelope.type) {
            // content.data 是文本分片，不能 trim 或修改，否则拼接后可能丢失空格。
            TYPE_CONTENT -> ChatStreamEvent.Content(
                text = envelope.data?.jsonPrimitive?.content.orEmpty(),
            )

            TYPE_RESULT -> {
                val data = requireNotNull(envelope.data) {
                    "The result event must contain data."
                }
                try {
                    ChatStreamEvent.Result(json.decodeFromJsonElement<ChatResultDto>(data))
                } catch (exception: SerializationException) {
                    // 仅调试包记录真实协议，便于联调字段变化；不记录 token 等请求头。
                    if (BuildConfig.DEBUG) {
                        Log.e(LOG_TAG, "Result decoding failed: $data", exception)
                    }
                    throw exception
                }
            }

            TYPE_ERROR -> {
                val data = requireNotNull(envelope.data) {
                    "The error event must contain data."
                }
                ChatStreamEvent.Error(json.decodeFromJsonElement<StreamErrorDto>(data))
            }

            TYPE_DONE -> ChatStreamEvent.Done
            else -> ChatStreamEvent.Unknown(
                event = frame.event,
                type = envelope.type,
                rawData = frame.data,
            )
        }
    }

    private companion object {
        const val TYPE_CONTENT = "content"
        const val TYPE_RESULT = "result"
        const val TYPE_ERROR = "error"
        const val TYPE_DONE = "done"
        const val LOG_TAG = "ChatStreamDecoder"
    }
}
