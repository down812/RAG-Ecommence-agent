package com.jschaofan.ragagent.data.remote.sse

/**
 * 一条完整的 SSE 帧，此时其中的 JSON 还没有转换成聊天业务事件。
 */
data class SseFrame(
    val event: String,
    val data: String,
    val id: String? = null,
)
