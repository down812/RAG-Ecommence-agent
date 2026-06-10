package com.jschaofan.ragagent.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProductDetailDto(
    val id: Long = 0,
    val productCode: String = "",
    val title: String = "",
    val brand: String? = null,
    val category: String? = null,
    val subCategory: String? = null,
    val basePrice: Double? = null,
    val mainImageUrl: String? = null,
    val localImagePath: String? = null,
    val status: Int? = null,
    val salesCount: Int? = null,
    val favoriteCount: Int? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val skuList: List<ProductSkuDto> = emptyList(),
)

@Serializable
data class ProductSkuDto(
    val id: Long = 0,
    val skuCode: String = "",
    val productId: Long = 0,
    val price: Double? = null,
    val originalPrice: Double? = null,
    val stock: Int? = null,
    val status: Int? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val attributeList: List<ProductSkuAttributeDto> = emptyList(),
)

@Serializable
data class ProductSkuAttributeDto(
    val id: Long = 0,
    val skuId: Long = 0,
    val attrName: String = "",
    val attrValue: String = "",
    val createdAt: String? = null,
    val updatedAt: String? = null,
)
