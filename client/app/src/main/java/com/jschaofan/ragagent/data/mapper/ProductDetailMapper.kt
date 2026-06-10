package com.jschaofan.ragagent.data.mapper

import com.jschaofan.ragagent.data.remote.dto.ProductDetailDto
import com.jschaofan.ragagent.data.remote.dto.ProductSkuAttributeDto
import com.jschaofan.ragagent.data.remote.dto.ProductSkuDto
import com.jschaofan.ragagent.domain.product.model.ProductDetail
import com.jschaofan.ragagent.domain.product.model.ProductSku
import com.jschaofan.ragagent.domain.product.model.ProductSkuAttribute

fun ProductDetailDto.toDomain() = ProductDetail(
    id = id,
    productCode = productCode,
    title = title,
    brand = brand,
    category = category,
    subCategory = subCategory,
    basePrice = basePrice,
    mainImageUrl = mainImageUrl,
    isOnSale = status == STATUS_ON_SALE,
    salesCount = salesCount,
    favoriteCount = favoriteCount,
    skus = skuList.map(ProductSkuDto::toDomain),
)

private fun ProductSkuDto.toDomain() = ProductSku(
    id = id,
    skuCode = skuCode,
    price = price,
    originalPrice = originalPrice,
    stock = stock ?: 0,
    // 后端未返回库存时，只根据 status 判断是否可售，避免把 null 误当成缺货。
    isAvailable = status == STATUS_ON_SALE && (stock == null || stock > 0),
    attributes = attributeList.map(ProductSkuAttributeDto::toDomain),
)

private fun ProductSkuAttributeDto.toDomain() = ProductSkuAttribute(
    name = attrName,
    value = attrValue,
)

private const val STATUS_ON_SALE = 1
