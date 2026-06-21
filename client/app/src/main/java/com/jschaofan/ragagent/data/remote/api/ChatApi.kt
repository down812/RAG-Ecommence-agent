package com.jschaofan.ragagent.data.remote.api

import com.jschaofan.ragagent.core.network.ApiEnvelope
import com.jschaofan.ragagent.data.remote.dto.ChatSessionMessageDto
import com.jschaofan.ragagent.data.remote.dto.ChatSessionSummaryDto
import com.jschaofan.ragagent.data.remote.dto.EvaluateDto
import com.jschaofan.ragagent.data.remote.dto.EvaluateRequestDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * 聊天模块中的普通 HTTP 接口。
 *
 * 流式聊天仍由 ChatSseClient 处理；停止对话返回普通 JSON，因此交给 Retrofit。
 */
interface ChatApi {
    @POST("evaluate/add")
    suspend fun submitEvaluation(
        @Body request: EvaluateRequestDto,
    ): ApiEnvelope<EvaluateDto>

    @GET("ai/chat/sessions")
    suspend fun getSessions(): ApiEnvelope<List<ChatSessionSummaryDto>>

    @GET("ai/chat/sessions/{sessionId}")
    suspend fun getSession(
        @Path("sessionId") sessionId: String,
    ): ApiEnvelope<List<ChatSessionMessageDto>>

    @DELETE("ai/chat/sessions/{sessionId}")
    suspend fun deleteSession(
        @Path("sessionId") sessionId: String,
    ): ApiEnvelope<String>

    @POST("ai/chat/messages/{sessionId}/{messageId}/stop")
    suspend fun stopChat(
        @Path("sessionId") sessionId: String,
        @Path("messageId") messageId: String,
    ): ApiEnvelope<String>
}
