package com.jschaofan.ragagent.data.remote.sse

import java.io.IOException

/**
 * SSE 客户端可识别的传输错误，Repository 会将其转换成领域错误。
 */
sealed class ChatSseException(message: String) : IOException(message) {
    class Http(val statusCode: Int) :
        ChatSseException("SSE request failed with HTTP $statusCode.")

    data object EmptyBody : ChatSseException("SSE response body is empty.")
}
