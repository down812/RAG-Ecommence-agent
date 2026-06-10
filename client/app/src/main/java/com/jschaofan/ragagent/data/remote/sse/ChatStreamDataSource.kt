package com.jschaofan.ragagent.data.remote.sse

import com.jschaofan.ragagent.data.remote.dto.ChatStreamRequest
import kotlinx.coroutines.flow.Flow

/**
 * SSE 数据源抽象，使 Repository 测试不需要启动真实网络连接。
 */
fun interface ChatStreamDataSource {
    fun stream(request: ChatStreamRequest): Flow<ChatStreamEvent>
}
