@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.jschaofan.ragagent.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonElement
import java.io.File

@Serializable
data class ChatSessionSummaryDto(
    val sessionId: String,
    val title: String? = null,
    val createdAt: JsonElement? = null,
)

@Serializable
data class ChatSessionMessageDto(
    val sessionId: String,
    val messageId: String,
    val content: String = "",
    val result: ChatResultDto? = null,
    val messageType: String? = null,
    val createdAt: JsonElement? = null,
)

/**
 * App 内部使用的聊天请求模型。
 *
 * ChatSseClient 会把它转换成后端当前要求的 Query 参数加 multipart/form-data 格式。
 */
data class ChatStreamRequest(
    val sessionId: String,
    val messageId: String,
    val content: String,
    val files: List<ChatAttachment> = emptyList(),
)

/**
 * 待上传的聊天附件。
 *
 * 相册或相机返回的 Uri 会在多模态模块中先复制到缓存文件，再由 OkHttp 从文件流式上传，
 * 避免一次性把大图片全部读入内存。
 */
data class ChatAttachment(
    val file: File,
    val mediaType: String,
    val fileName: String = file.name,
)

/**
 * 文本分片传输后，后端发送的结构化结果。
 *
 * 搜索和对比字段的协议尚未完全确定，因此暂时保留为 JsonElement。
 */
@Serializable
data class ChatResultDto(
    val sessionId: String,
    // 图片搜索结果当前可能返回 null，业务关联仍使用客户端发起请求时的 messageId。
    val messageId: String? = null,
    val responseType: String,
    // 回答正文已通过 content 事件流式返回，最终 result 中可能不再重复携带。
    val answer: String? = null,
    val sources: List<SourceDto> = emptyList(),
    val timestamp: Long? = null,
    val queryAnalysis: QueryAnalysisDto? = null,
    // 非当前 responseType 的字段，旧版后端可能显式返回 null。
    val recommendations: List<RecommendedProductDto>? = null,
    val searchCriteria: SearchCriteriaDto? = null,
    val totalCount: Int? = null,
    val products: List<SearchProductDto>? = null,
    val imageAnalysis: ImageAnalysisDto? = null,
    val imageSearchProducts: List<ImageSearchProductDto>? = null,
    // 后端尚未提供 ComparisonVO，先保留原始 JSON，避免提前猜测协议。
    val comparedProducts: JsonElement? = null,
    val comparisonMatrix: JsonElement? = null,
    val comparisonRecommendations: JsonElement? = null,
)

@Serializable
data class SourceDto(
    val title: String,
    val sourceType: String,
    val content: String? = null,
    val ragSource: RagSourceDto? = null,
)

@Serializable
data class RagSourceDto(
    val productInfo: String,
    @JsonNames("marketing_description")
    val marketingDescription: String,
    @JsonNames("official_faq")
    val officialFAQ: List<FaqDto> = emptyList(),
    @JsonNames("user_reviews")
    val userReviews: List<UserReviewDto> = emptyList(),
)

@Serializable
data class FaqDto(
    val question: String,
    val answer: String,
)

@Serializable
data class UserReviewDto(
    val nickname: String,
    // 后端实际可能返回 1~5 的数字，旧数据也可能是字符串，先按 JSON 原始值兼容。
    val rating: JsonPrimitive,
    val content: String,
)

@Serializable
data class QueryAnalysisDto(
    val detectedCategory: String? = null,
    val budget: String? = null,
    val specialRequirements: List<String>? = null,
)

@Serializable
data class RecommendedProductDto(
    val productId: Long,
    val productName: String,
    val price: Double? = null,
    val brand: String? = null,
    val category: String? = null,
    val mainImageUrl: String? = null,
    val keyFeatures: List<String> = emptyList(),
    val reason: String,
    val applicableScenario: String,
    val rating: String? = null,
    val salesCount: Int? = null,
)

@Serializable
data class SearchCriteriaDto(
    val keyword: String? = null,
    val brand: String? = null,
    val priceRange: String? = null,
    val category: String? = null,
)

@Serializable
data class SearchProductDto(
    val productId: Long,
    val productCode: String? = null,
    val productName: String,
    val brand: String? = null,
    val category: String? = null,
    val price: Double? = null,
    val mainImageUrl: String? = null,
    val status: String? = null,
    val salesCount: Int? = null,
    val stockStatus: String? = null,
    val highlight: String? = null,
)

@Serializable
data class ImageAnalysisDto(
    val detectedCategory: String? = null,
    val detectedBrand: String? = null,
    val visualFeatures: List<String> = emptyList(),
    val colorDescription: String? = null,
    val shapeDescription: String? = null,
    val textOnProduct: String? = null,
)

@Serializable
data class ImageSearchProductDto(
    val productId: Long,
    val productCode: String? = null,
    val productName: String,
    val brand: String? = null,
    val category: String? = null,
    val price: Double? = null,
    val mainImageUrl: String? = null,
    val salesCount: Int? = null,
    val similarity: Double? = null,
    val matchReason: String? = null,
)

/**
 * content、result、error、done 事件共用的原始 JSON 外壳。
 *
 * 不同事件中的 `data` 类型不同，所以第一步必须先按 JsonElement 接收。
 */
@Serializable
internal data class ChatStreamEnvelope(
    val type: String,
    val data: JsonElement? = null,
)

@Serializable
data class StreamErrorDto(
    val code: String? = null,
    val message: String,
    val retryable: Boolean = false,
)
