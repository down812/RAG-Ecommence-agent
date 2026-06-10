package com.jschaofan.ragagent.data.remote.api

import com.jschaofan.ragagent.core.network.ApiEnvelope
import com.jschaofan.ragagent.data.remote.dto.ProductDetailDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ProductApi {
    @GET("api/product/list")
    suspend fun getProducts(
        @Query("title") title: String? = null,
        @Query("status") status: Int? = null,
        @Query("category") category: String? = null,
        @Query("brand") brand: String? = null,
    ): ApiEnvelope<List<ProductDetailDto>>

    @GET("api/product/list/page")
    suspend fun getProductPage(
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 10,
        @Query("status") status: Int? = null,
        @Query("category") category: String? = null,
        @Query("brand") brand: String? = null,
    ): ApiEnvelope<com.jschaofan.ragagent.data.remote.dto.PageDto<ProductDetailDto>>

    @GET("api/product/search")
    suspend fun searchProducts(
        @Query("keyword") keyword: String,
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 10,
    ): ApiEnvelope<com.jschaofan.ragagent.data.remote.dto.PageDto<ProductDetailDto>>

    @GET("api/product/detail/{id}")
    suspend fun getProductDetail(
        @Path("id") productId: Long,
    ): ApiEnvelope<ProductDetailDto>
}
