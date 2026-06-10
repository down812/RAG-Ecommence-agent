package com.jschaofan.ragagent.domain.chat.model

/**
 * AI 最终返回的结构化结果。
 *
 * 文本回答仍保存在 ChatMessage.content 中；这里保存商品卡片、搜索条件和来源等
 * 可供后续原生组件展示的数据。
 */
sealed interface ChatStructuredResult {
    val answer: String
    val sources: List<ChatSource>
    val timestamp: Long?

    data class Recommendation(
        override val answer: String,
        override val sources: List<ChatSource>,
        override val timestamp: Long?,
        val queryAnalysis: QueryAnalysis?,
        val products: List<RecommendedProduct>,
    ) : ChatStructuredResult

    data class SearchResult(
        override val answer: String,
        override val sources: List<ChatSource>,
        override val timestamp: Long?,
        val criteria: SearchCriteria?,
        val totalCount: Int,
        val products: List<SearchProduct>,
    ) : ChatStructuredResult

    data class ImageSearch(
        override val answer: String,
        override val sources: List<ChatSource>,
        override val timestamp: Long?,
        val analysis: ImageAnalysis?,
        val products: List<ImageSearchProduct>,
    ) : ChatStructuredResult

    /**
     * 当前后端以 Markdown 表格返回商品对比，因此直接保存完整 Markdown。
     */
    data class Comparison(
        override val answer: String,
        override val sources: List<ChatSource>,
        override val timestamp: Long?,
    ) : ChatStructuredResult

    /**
     * 后端新增 responseType 时保留基础结果，避免旧版客户端丢失完整回答。
     */
    data class Unknown(
        val responseType: String,
        override val answer: String,
        override val sources: List<ChatSource>,
        override val timestamp: Long?,
    ) : ChatStructuredResult
}

data class QueryAnalysis(
    val detectedCategory: String?,
    val budget: String?,
    val specialRequirements: List<String>,
)

data class RecommendedProduct(
    val productId: Long,
    val productName: String,
    val price: Double?,
    val brand: String?,
    val category: String?,
    val mainImageUrl: String?,
    val keyFeatures: List<String>,
    val reason: String,
    val applicableScenario: String,
    val rating: String?,
    val salesCount: Int?,
)

data class SearchCriteria(
    val keyword: String?,
    val brand: String?,
    val priceRange: String?,
    val category: String?,
)

data class SearchProduct(
    val productId: Long,
    val productCode: String?,
    val productName: String,
    val brand: String?,
    val category: String?,
    val price: Double?,
    val mainImageUrl: String?,
    val status: String?,
    val salesCount: Int?,
    val stockStatus: String?,
    val highlight: String?,
)

data class ImageAnalysis(
    val detectedCategory: String?,
    val detectedBrand: String?,
    val visualFeatures: List<String>,
    val colorDescription: String?,
    val shapeDescription: String?,
    val textOnProduct: String?,
)

data class ImageSearchProduct(
    val productId: Long,
    val productCode: String?,
    val productName: String,
    val brand: String?,
    val category: String?,
    val price: Double?,
    val mainImageUrl: String?,
    val salesCount: Int?,
    val similarity: Double?,
    val matchReason: String?,
)
