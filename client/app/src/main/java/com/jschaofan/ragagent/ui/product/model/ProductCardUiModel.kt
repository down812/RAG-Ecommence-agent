package com.jschaofan.ragagent.ui.product.model

import com.jschaofan.ragagent.domain.chat.model.ChatStructuredResult
import com.jschaofan.ragagent.domain.chat.model.ImageSearchProduct
import com.jschaofan.ragagent.domain.chat.model.RecommendedProduct
import com.jschaofan.ragagent.domain.chat.model.SearchProduct

/**
 * 商品卡片统一使用的 UI 模型。
 *
 * 后端三类响应的商品字段不同，先在这里收敛，避免 Compose 组件依赖具体响应类型。
 */
data class ProductCardUiModel(
    val id: Long,
    val name: String,
    val imageUrl: String?,
    val price: Double?,
    val brand: String?,
    val category: String?,
    val description: String?,
    val tags: List<String>,
    val salesCount: Int?,
    val badge: String?,
    val isOnSale: Boolean = true,
)

fun ChatStructuredResult.toProductCards(): List<ProductCardUiModel> = when (this) {
    is ChatStructuredResult.Recommendation -> products.map(RecommendedProduct::toCardUiModel)
    is ChatStructuredResult.SearchResult -> products.map(SearchProduct::toCardUiModel)
    is ChatStructuredResult.ImageSearch -> products.map(ImageSearchProduct::toCardUiModel)
    is ChatStructuredResult.Comparison,
    is ChatStructuredResult.Unknown -> emptyList()
}

private fun RecommendedProduct.toCardUiModel() = ProductCardUiModel(
    id = productId,
    name = productName,
    imageUrl = mainImageUrl,
    price = price,
    brand = brand,
    category = category,
    description = reason.ifBlank { applicableScenario },
    tags = keyFeatures.filter(String::isNotBlank).take(MAX_TAG_COUNT),
    salesCount = salesCount,
    badge = rating?.takeIf(String::isNotBlank)?.let { rating -> "评分 $rating" },
)

private fun SearchProduct.toCardUiModel() = ProductCardUiModel(
    id = productId,
    name = productName,
    imageUrl = mainImageUrl,
    price = price,
    brand = brand,
    category = category,
    description = highlight?.takeIf(String::isNotBlank),
    tags = listOfNotNull(
        stockStatus?.takeIf(String::isNotBlank),
        status?.takeIf(String::isNotBlank),
    ).take(MAX_TAG_COUNT),
    salesCount = salesCount,
    badge = productCode?.takeIf(String::isNotBlank),
)

private fun ImageSearchProduct.toCardUiModel() = ProductCardUiModel(
    id = productId,
    name = productName,
    imageUrl = mainImageUrl,
    price = price,
    brand = brand,
    category = category,
    description = matchReason?.takeIf(String::isNotBlank),
    tags = emptyList(),
    salesCount = salesCount,
    badge = similarity?.let { value ->
        "相似度 ${(value.asRatio() * 100).toInt()}%"
    },
)

private fun Double.asRatio(): Double = if (this > 1.0) this / 100.0 else this

private const val MAX_TAG_COUNT = 3
