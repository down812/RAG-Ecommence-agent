package com.jschaofan.ragagent.data.remote.api

import com.jschaofan.ragagent.core.network.ApiEnvelope
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * 聊天模块中的普通 HTTP 接口。
 *
 * 流式聊天仍由 ChatSseClient 处理；停止对话返回普通 JSON，因此交给 Retrofit。
 */
interface ChatApi {
    @POST("ai/chat/messages/{sessionId}/{messageId}/stop")
    suspend fun stopChat(
        @Path("sessionId") sessionId: String,
        @Path("messageId") messageId: String,
    ): ApiEnvelope<String>
}
