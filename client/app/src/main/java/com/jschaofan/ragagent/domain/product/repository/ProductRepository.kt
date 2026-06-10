package com.jschaofan.ragagent.domain.product.repository

import com.jschaofan.ragagent.core.network.ApiResult
import com.jschaofan.ragagent.domain.product.model.ProductDetail

interface ProductRepository {
    suspend fun getProductDetail(productId: Long): ApiResult<ProductDetail>
}
