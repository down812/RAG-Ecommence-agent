package com.jschaofan.ragagent.data.remote.sse

import com.jschaofan.ragagent.data.remote.dto.ChatResultDto
import com.jschaofan.ragagent.data.remote.dto.StreamErrorDto

/**
 * Repository 接收的网络流式事件。
 *
 * Content 必须原样拼接，因为后端可能从任意位置拆词；Result 携带商品卡片和引用，
 * Done 用于结束加载状态。Repository 会继续把其中的 DTO 转换为领域模型。
 */
sealed interface ChatStreamEvent {
    data class Content(val text: String) : ChatStreamEvent

    data class Result(val value: ChatResultDto) : ChatStreamEvent

    data class Error(val value: StreamErrorDto) : ChatStreamEvent

    // 保留后端未来新增的事件，避免旧版 App 因未知类型直接崩溃。
    data class Unknown(
        val event: String,
        val type: String?,
        val rawData: String,
    ) : ChatStreamEvent

    data object Done : ChatStreamEvent
}
