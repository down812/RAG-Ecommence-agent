package com.jschaofan.ragagent.data.mapper

import com.jschaofan.ragagent.data.remote.dto.ChatResultDto
import com.jschaofan.ragagent.data.remote.dto.ChatSessionMessageDto
import com.jschaofan.ragagent.data.remote.dto.ChatSessionSummaryDto
import com.jschaofan.ragagent.data.remote.dto.ImageAnalysisDto
import com.jschaofan.ragagent.data.remote.dto.ImageSearchProductDto
import com.jschaofan.ragagent.data.remote.dto.QueryAnalysisDto
import com.jschaofan.ragagent.data.remote.dto.RecommendedProductDto
import com.jschaofan.ragagent.data.remote.dto.SearchCriteriaDto
import com.jschaofan.ragagent.data.remote.dto.SearchProductDto
import com.jschaofan.ragagent.domain.chat.model.ChatStructuredResult
import com.jschaofan.ragagent.domain.chat.model.MessageSender
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.serialization.json.JsonPrimitive

class ChatMappersTest {
    @Test
    fun `maps history summaries and messages`() {
        val session = ChatSessionSummaryDto(
            sessionId = "session-1",
            title = "推荐耳机",
            createdAt = JsonPrimitive(1000L),
        ).toDomain()
        val message = ChatSessionMessageDto(
            sessionId = "session-1",
            messageId = "message-1",
            content = "推荐耳机",
            messageType = "USER",
            createdAt = JsonPrimitive(2000L),
        ).toDomain()

        assertEquals("推荐耳机", session.title)
        assertEquals(1000L, session.createdAtEpochMillis)
        assertEquals(MessageSender.USER, message.sender)
        assertEquals("message-1", message.requestId)
        assertEquals(2000L, message.createdAtEpochMillis)
    }

    @Test
    fun `maps documented message types without case sensitivity`() {
        assertEquals(MessageSender.USER, "user".toMessageSender())
        assertEquals(MessageSender.USER, " USER ".toMessageSender())
        assertEquals(MessageSender.ASSISTANT, "assistant".toMessageSender())
        assertEquals(MessageSender.ASSISTANT, "AI".toMessageSender())
    }

    @Test
    fun `keeps missing or unsupported message type unknown`() {
        assertEquals(MessageSender.UNKNOWN, null.toMessageSender())
        assertEquals(MessageSender.UNKNOWN, "".toMessageSender())
        assertEquals(MessageSender.UNKNOWN, "system".toMessageSender())
    }

    @Test
    fun `maps recommendation result to domain model`() {
        val result = ChatResultDto(
            sessionId = "session-1",
            messageId = "message-1",
            responseType = "recommendation",
            answer = "推荐结果",
            timestamp = 1000L,
            queryAnalysis = QueryAnalysisDto(
                detectedCategory = "手机",
                budget = "4000元",
                specialRequirements = null,
            ),
            recommendations = listOf(
                RecommendedProductDto(
                    productId = 1L,
                    productName = "测试手机",
                    price = 3999.0,
                    reason = "满足预算",
                    applicableScenario = "日常使用",
                ),
            ),
        ).toDomain()

        assertTrue(result is ChatStructuredResult.Recommendation)
        result as ChatStructuredResult.Recommendation
        assertEquals("推荐结果", result.answer)
        assertEquals("手机", result.queryAnalysis?.detectedCategory)
        assertTrue(result.queryAnalysis?.specialRequirements.orEmpty().isEmpty())
        assertEquals(1L, result.products.first().productId)
    }

    @Test
    fun `maps search and image search results`() {
        val search = ChatResultDto(
            sessionId = "session-2",
            messageId = "message-2",
            responseType = "search_result",
            searchCriteria = SearchCriteriaDto(keyword = "耳机"),
            totalCount = null,
            products = listOf(
                SearchProductDto(
                    productId = 2L,
                    productName = "测试耳机",
                ),
            ),
        ).toDomain()
        val imageSearch = ChatResultDto(
            sessionId = "session-3",
            messageId = "message-3",
            responseType = "image_search",
            imageAnalysis = ImageAnalysisDto(detectedCategory = "外套"),
            imageSearchProducts = listOf(
                ImageSearchProductDto(
                    productId = 3L,
                    productName = "相似外套",
                    similarity = 0.9,
                ),
            ),
        ).toDomain()

        assertTrue(search is ChatStructuredResult.SearchResult)
        assertEquals(1, (search as ChatStructuredResult.SearchResult).totalCount)
        assertEquals("耳机", search.criteria?.keyword)

        assertTrue(imageSearch is ChatStructuredResult.ImageSearch)
        imageSearch as ChatStructuredResult.ImageSearch
        assertEquals("外套", imageSearch.analysis?.detectedCategory)
        assertEquals(0.9, imageSearch.products.first().similarity ?: 0.0, 0.0)
    }

    @Test
    fun `maps comparison markdown and preserves unknown response type`() {
        val comparison = ChatResultDto(
            sessionId = "session-4",
            messageId = "message-4",
            responseType = "comparison",
            answer = "| 对比项 | 商品A | 商品B |",
        ).toDomain()
        val unknown = ChatResultDto(
            sessionId = "session-5",
            messageId = "message-5",
            responseType = "future_type",
            answer = "未来类型的完整回答",
        ).toDomain()

        assertTrue(comparison is ChatStructuredResult.Comparison)
        assertEquals(
            "| 对比项 | 商品A | 商品B |",
            (comparison as ChatStructuredResult.Comparison).answer,
        )

        assertTrue(unknown is ChatStructuredResult.Unknown)
        unknown as ChatStructuredResult.Unknown
        assertEquals("future_type", unknown.responseType)
        assertEquals("未来类型的完整回答", unknown.answer)
    }
}
