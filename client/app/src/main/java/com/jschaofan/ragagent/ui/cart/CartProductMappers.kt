package com.jschaofan.ragagent.ui.cart

import com.jschaofan.ragagent.domain.cart.model.AddCartProduct
import com.jschaofan.ragagent.domain.product.model.ProductDetail
import com.jschaofan.ragagent.domain.product.model.ProductSku
import com.jschaofan.ragagent.ui.product.model.ProductCardUiModel

fun ProductCardUiModel.toAddCartProduct(): AddCartProduct? {
    val validPrice = price ?: return null
    return AddCartProduct(
        productId = id,
        productName = name,
        imageUrl = imageUrl,
        unitPrice = validPrice,
    )
}

fun ProductDetail.toAddCartProduct(sku: ProductSku? = null): AddCartProduct? {
    val availableSkus = skus.filter { it.isAvailable }
    // 多规格商品必须由用户在详情页明确选择，避免误加错误规格。
    val selectedSku = sku ?: availableSkus.singleOrNull() ?: return null
    val validPrice = selectedSku?.price ?: basePrice ?: return null
    return AddCartProduct(
        productId = id,
        skuId = selectedSku?.id,
        productName = title,
        imageUrl = mainImageUrl,
        unitPrice = validPrice,
        maxStock = selectedSku?.stock,
        specification = selectedSku?.attributes
            ?.joinToString("，") { attribute -> "${attribute.name}：${attribute.value}" }
            ?.takeIf(String::isNotBlank),
    )
}
