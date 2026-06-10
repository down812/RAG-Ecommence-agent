package com.jschaofan.ragagent.ui.product.detail

import com.jschaofan.ragagent.domain.product.model.ProductDetail

data class ProductDetailUiState(
    val productId: Long? = null,
    val isLoading: Boolean = false,
    val product: ProductDetail? = null,
    val errorMessage: String? = null,
) {
    val canRetry: Boolean
        get() = productId != null && !isLoading && errorMessage != null
}
