package com.jschaofan.ragagent.domain.product.model

/**
 * 商品详情领域模型。状态使用布尔值表达，避免 UI 理解后端的 0/1 编码。
 */
data class ProductDetail(
    val id: Long,
    val productCode: String,
    val title: String,
    val brand: String?,
    val category: String?,
    val subCategory: String?,
    val basePrice: Double?,
    val mainImageUrl: String?,
    val isOnSale: Boolean,
    val salesCount: Int?,
    val favoriteCount: Int?,
    val skus: List<ProductSku>,
)

data class ProductSku(
    val id: Long,
    val skuCode: String,
    val price: Double?,
    val originalPrice: Double?,
    val stock: Int,
    val isAvailable: Boolean,
    val attributes: List<ProductSkuAttribute>,
)

data class ProductSkuAttribute(
    val name: String,
    val value: String,
)
