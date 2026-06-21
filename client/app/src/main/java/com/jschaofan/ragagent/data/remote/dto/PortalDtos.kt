package com.jschaofan.ragagent.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class PageDto<T>(
    val records: List<T> = emptyList(),
    val total: Long = 0,
    val current: Long = 1,
    val pages: Long = 1,
)

@Serializable
data class AddToCartDto(val productId: Long, val skuId: Long, val quantity: Int)

@Serializable
data class UpdateCartItemDto(val cartItemId: Long, val quantity: Int)

@Serializable
data class CartDto(
    val cartId: Long? = null,
    val userId: Long? = null,
    val cartItems: List<CartItemDto> = emptyList(),
    val totalItems: Int = 0,
    val totalAmount: Double = 0.0,
)

@Serializable
data class CartItemDto(
    val cartItemId: Long,
    val productId: Long,
    val productName: String,
    val productImage: String? = null,
    val skuId: Long,
    val skuSpec: String? = null,
    val skuAttributes: List<ProductSkuAttributeDto> = emptyList(),
    val quantity: Int,
    val price: Double,
    val subtotal: Double,
)

@Serializable
data class SubaccountQueryDto(
    val identifier: String? = null,
    val type: Int? = null,
)

@Serializable
data class SubaccountPageDto(
    val total: Long = 0,
    val pages: Long = 0,
    val pageNum: Int = 1,
    val pageSize: Int = 20,
    val rows: List<SubaccountDto> = emptyList(),
)

@Serializable
data class SubaccountDto(
    val subaccountId: Long,
    val identifier: String,
    val type: Int,
    val createdAt: String? = null,
)

@Serializable
data class SubaccountCreateDto(val identifier: String, val password: String, val type: Int)

@Serializable
data class SubaccountUpdateDto(val password: String? = null, val type: Int? = null)

@Serializable
data class DatasetCreateDto(
    val name: String,
    val description: String = "",
    val disabled: Int = 0,
)

@Serializable
data class DatasetFileDto(
    val id: Long,
    val name: String,
    val fileType: String? = null,
    val fileSize: Long? = null,
    val datasetId: Long,
    val disabled: Int = 0,
    val createdAt: kotlinx.serialization.json.JsonElement? = null,
    val hitCount: Int = 0,
)
