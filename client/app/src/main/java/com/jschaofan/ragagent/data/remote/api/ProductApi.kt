package com.jschaofan.ragagent.data.remote.api

import com.jschaofan.ragagent.core.network.ApiEnvelope
import com.jschaofan.ragagent.data.remote.dto.ProductDetailDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Multipart
import retrofit2.http.Part
import okhttp3.MultipartBody

interface ProductApi {
    @Multipart
    @POST("api/product/upload/image")
    suspend fun uploadProductImage(
        @Part file: MultipartBody.Part,
        @Part("productCode") productCode: okhttp3.RequestBody,
    ): ApiEnvelope<String>

    @POST("api/product/create")
    suspend fun createProduct(@Body product: ProductDetailDto): ApiEnvelope<Boolean>

    @PUT("api/product/update")
    suspend fun updateProduct(@Body product: ProductDetailDto): ApiEnvelope<Boolean>

    @DELETE("api/product/delete/{id}")
    suspend fun deleteProduct(@Path("id") productId: Long): ApiEnvelope<Boolean>

    @Multipart
    @PUT("api/product/update/image")
    suspend fun updateProductImage(
        @Part file: MultipartBody.Part,
        @Part("productId") productId: okhttp3.RequestBody,
    ): ApiEnvelope<Boolean>

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
