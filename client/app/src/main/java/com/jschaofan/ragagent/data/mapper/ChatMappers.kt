package com.jschaofan.ragagent.data.mapper

import com.jschaofan.ragagent.data.remote.dto.ChatResultDto
import com.jschaofan.ragagent.data.remote.dto.ChatSessionMessageDto
import com.jschaofan.ragagent.data.remote.dto.ChatSessionSummaryDto
import com.jschaofan.ragagent.data.remote.dto.FaqDto
import com.jschaofan.ragagent.data.remote.dto.ImageAnalysisDto
import com.jschaofan.ragagent.data.remote.dto.ImageSearchProductDto
import com.jschaofan.ragagent.data.remote.dto.QueryAnalysisDto
import com.jschaofan.ragagent.data.remote.dto.RagSourceDto
import com.jschaofan.ragagent.data.remote.dto.RecommendedProductDto
import com.jschaofan.ragagent.data.remote.dto.SearchCriteriaDto
import com.jschaofan.ragagent.data.remote.dto.SearchProductDto
import com.jschaofan.ragagent.data.remote.dto.SourceDto
import com.jschaofan.ragagent.data.remote.dto.UserReviewDto
import com.jschaofan.ragagent.domain.chat.model.ChatSource
import com.jschaofan.ragagent.domain.chat.model.ChatMessage
import com.jschaofan.ragagent.domain.chat.model.ChatSession
import com.jschaofan.ragagent.domain.chat.model.ChatStructuredResult
import com.jschaofan.ragagent.domain.chat.model.Faq
import com.jschaofan.ragagent.domain.chat.model.ImageAnalysis
import com.jschaofan.ragagent.domain.chat.model.ImageSearchProduct
import com.jschaofan.ragagent.domain.chat.model.MessageSender
import com.jschaofan.ragagent.domain.chat.model.MessageStatus
import com.jschaofan.ragagent.domain.chat.model.QueryAnalysis
import com.jschaofan.ragagent.domain.chat.model.RagSource
import com.jschaofan.ragagent.domain.chat.model.RecommendedProduct
import com.jschaofan.ragagent.domain.chat.model.SearchCriteria
import com.jschaofan.ragagent.domain.chat.model.SearchProduct
import com.jschaofan.ragagent.domain.chat.model.UserReview
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.text.SimpleDateFormat
import java.util.Locale

fun ChatSessionSummaryDto.toDomain() = ChatSession(
    sessionId = sessionId,
    title = title,
    createdAtEpochMillis = createdAt.toEpochMillis(),
)

fun ChatSessionMessageDto.toDomain(): ChatMessage {
    val sender = messageType.toMessageSender()
    return ChatMessage(
        id = "$messageId-${sender.name.lowercase()}",
        requestId = messageId,
        sender = sender,
        content = content,
        status = MessageStatus.COMPLETED,
        structuredResult = result?.toDomain(),
        createdAtEpochMillis = createdAt.toEpochMillis() ?: 0L,
    )
}

private fun JsonElement?.toEpochMillis(): Long? {
    val primitive = this as? JsonPrimitive ?: return null
    primitive.longOrNull?.let { return it }
    val value = runCatching { primitive.jsonPrimitive.content }.getOrNull() ?: return null
    val patterns = listOf("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm")
    return patterns.firstNotNullOfOrNull { pattern ->
        runCatching { SimpleDateFormat(pattern, Locale.ROOT).parse(value)?.time }.getOrNull()
    }
}

/**
 * 将后端 messageType 转换成客户端发送方类型。
 *
 * 后端文档尚未列出固定枚举，因此只接受已经明确的 user、assistant 和 ai，
 * 其他值保留为 UNKNOWN，避免把未知消息错误显示为 AI 回复。
 */
fun String?.toMessageSender(): MessageSender = when (this?.trim()?.lowercase()) {
    "user" -> MessageSender.USER
    "assistant", "ai" -> MessageSender.ASSISTANT
    else -> MessageSender.UNKNOWN
}

/**
 * 按 responseType 将统一的网络 DTO 转换成具体领域结果。
 */
fun ChatResultDto.toDomain(): ChatStructuredResult {
    val domainSources = sources.map(SourceDto::toDomain)

    return when (responseType) {
        RESPONSE_TYPE_RECOMMENDATION -> ChatStructuredResult.Recommendation(
            answer = answer.orEmpty(),
            sources = domainSources,
            timestamp = timestamp,
            queryAnalysis = queryAnalysis?.toDomain(),
            products = recommendations.orEmpty().map(RecommendedProductDto::toDomain),
        )

        RESPONSE_TYPE_SEARCH_RESULT -> ChatStructuredResult.SearchResult(
            answer = answer.orEmpty(),
            sources = domainSources,
            timestamp = timestamp,
            criteria = searchCriteria?.toDomain(),
            totalCount = totalCount ?: products.orEmpty().size,
            products = products.orEmpty().map(SearchProductDto::toDomain),
        )

        RESPONSE_TYPE_IMAGE_SEARCH -> ChatStructuredResult.ImageSearch(
            answer = answer.orEmpty(),
            sources = domainSources,
            timestamp = timestamp,
            analysis = imageAnalysis?.toDomain(),
            products = imageSearchProducts.orEmpty().map(ImageSearchProductDto::toDomain),
        )

        RESPONSE_TYPE_COMPARISON -> ChatStructuredResult.Comparison(
            answer = answer.orEmpty(),
            sources = domainSources,
            timestamp = timestamp,
        )

        else -> ChatStructuredResult.Unknown(
            responseType = responseType,
            answer = answer.orEmpty(),
            sources = domainSources,
            timestamp = timestamp,
        )
    }
}

private fun SourceDto.toDomain() = ChatSource(
    title = title,
    sourceType = sourceType,
    content = content,
    ragSource = ragSource?.toDomain(),
)

private fun RagSourceDto.toDomain() = RagSource(
    productInfo = productInfo,
    marketingDescription = marketingDescription,
    officialFaq = officialFAQ.map(FaqDto::toDomain),
    userReviews = userReviews.map(UserReviewDto::toDomain),
)

private fun FaqDto.toDomain() = Faq(
    question = question,
    answer = answer,
)

private fun UserReviewDto.toDomain() = UserReview(
    nickname = nickname,
    rating = rating.content,
    content = content,
)

private fun QueryAnalysisDto.toDomain() = QueryAnalysis(
    detectedCategory = detectedCategory,
    budget = budget,
    specialRequirements = specialRequirements.orEmpty(),
)

private fun RecommendedProductDto.toDomain() = RecommendedProduct(
    productId = productId,
    productName = productName,
    price = price,
    brand = brand,
    category = category,
    mainImageUrl = mainImageUrl,
    keyFeatures = keyFeatures,
    reason = reason,
    applicableScenario = applicableScenario,
    rating = rating,
    salesCount = salesCount,
)

private fun SearchCriteriaDto.toDomain() = SearchCriteria(
    keyword = keyword,
    brand = brand,
    priceRange = priceRange,
    category = category,
)

private fun SearchProductDto.toDomain() = SearchProduct(
    productId = productId,
    productCode = productCode,
    productName = productName,
    brand = brand,
    category = category,
    price = price,
    mainImageUrl = mainImageUrl,
    status = status,
    salesCount = salesCount,
    stockStatus = stockStatus,
    highlight = highlight,
)

private fun ImageAnalysisDto.toDomain() = ImageAnalysis(
    detectedCategory = detectedCategory,
    detectedBrand = detectedBrand,
    visualFeatures = visualFeatures,
    colorDescription = colorDescription,
    shapeDescription = shapeDescription,
    textOnProduct = textOnProduct,
)

private fun ImageSearchProductDto.toDomain() = ImageSearchProduct(
    productId = productId,
    productCode = productCode,
    productName = productName,
    brand = brand,
    category = category,
    price = price,
    mainImageUrl = mainImageUrl,
    salesCount = salesCount,
    similarity = similarity,
    matchReason = matchReason,
)

private const val RESPONSE_TYPE_RECOMMENDATION = "recommendation"
private const val RESPONSE_TYPE_SEARCH_RESULT = "search_result"
private const val RESPONSE_TYPE_IMAGE_SEARCH = "image_search"
private const val RESPONSE_TYPE_COMPARISON = "comparison"
