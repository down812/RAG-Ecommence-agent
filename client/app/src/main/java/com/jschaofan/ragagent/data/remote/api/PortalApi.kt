package com.jschaofan.ragagent.data.remote.api

import com.jschaofan.ragagent.core.network.ApiEnvelope
import com.jschaofan.ragagent.data.remote.dto.AddToCartDto
import com.jschaofan.ragagent.data.remote.dto.CartDto
import com.jschaofan.ragagent.data.remote.dto.SubaccountPageDto
import com.jschaofan.ragagent.data.remote.dto.SubaccountQueryDto
import com.jschaofan.ragagent.data.remote.dto.SubaccountCreateDto
import com.jschaofan.ragagent.data.remote.dto.SubaccountUpdateDto
import com.jschaofan.ragagent.data.remote.dto.DatasetCreateDto
import com.jschaofan.ragagent.data.remote.dto.UpdateCartItemDto
import com.jschaofan.ragagent.data.remote.dto.DatasetFileDto
import com.jschaofan.ragagent.data.remote.dto.EvaluateDto
import com.jschaofan.ragagent.data.remote.dto.PageDto
import okhttp3.ResponseBody
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface PortalApi {
    @GET("api/cart/get")
    suspend fun getCart(): ApiEnvelope<CartDto>

    @POST("api/cart/add")
    suspend fun addToCart(@Body body: AddToCartDto): ApiEnvelope<CartDto>

    @PUT("api/cart/update")
    suspend fun updateCart(@Body body: UpdateCartItemDto): ApiEnvelope<CartDto>

    @DELETE("api/cart/remove/{cartItemId}")
    suspend fun removeCartItem(@Path("cartItemId") id: Long): ApiEnvelope<CartDto>

    @DELETE("api/cart/clear")
    suspend fun clearCart(): ApiEnvelope<Unit>

    @POST("user/subaccountList")
    suspend fun getUsers(
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Body query: SubaccountQueryDto = SubaccountQueryDto(),
    ): ApiEnvelope<SubaccountPageDto>

    @POST("user/subaccount")
    suspend fun createUser(@Body body: SubaccountCreateDto): ApiEnvelope<kotlinx.serialization.json.JsonElement>

    @PUT("user/subaccount/{subaccountId}")
    suspend fun updateUser(
        @Path("subaccountId") id: Long,
        @Body body: SubaccountUpdateDto,
    ): ApiEnvelope<kotlinx.serialization.json.JsonElement>

    @DELETE("user/subaccount/{subaccountId}")
    suspend fun deleteUser(@Path("subaccountId") id: Long): ApiEnvelope<kotlinx.serialization.json.JsonElement>

    @GET("ai/dataset/list")
    suspend fun getDatasets(): ApiEnvelope<kotlinx.serialization.json.JsonElement>

    @POST("ai/dataset/create")
    suspend fun createDataset(@Body body: DatasetCreateDto): ApiEnvelope<kotlinx.serialization.json.JsonElement>

    @POST("ai/dataset/toggle/{datasetId}")
    suspend fun toggleDataset(
        @Path("datasetId") id: Long,
        @Query("disabled") disabled: Int,
    ): ApiEnvelope<kotlinx.serialization.json.JsonElement>

    @DELETE("ai/dataset/{datasetId}")
    suspend fun deleteDataset(@Path("datasetId") id: Long): ApiEnvelope<kotlinx.serialization.json.JsonElement>

    @Multipart
    @POST("ai/dataset/upload/{datasetId}")
    suspend fun uploadDatasetFile(
        @Path("datasetId") datasetId: Long,
        @Part file: MultipartBody.Part,
    ): ApiEnvelope<kotlinx.serialization.json.JsonElement>

    @GET("ai/dataset/{datasetId}/files")
    suspend fun getDatasetFiles(
        @Path("datasetId") datasetId: Long,
    ): ApiEnvelope<List<DatasetFileDto>>

    @Streaming
    @GET("ai/dataset/file/{fileId}")
    suspend fun downloadDatasetFile(
        @Path("fileId") fileId: Long,
    ): ResponseBody

    @DELETE("ai/dataset/file/{fileId}")
    suspend fun deleteDatasetFile(
        @Path("fileId") fileId: Long,
    ): ApiEnvelope<kotlinx.serialization.json.JsonElement>

    @GET("evaluate/page")
    suspend fun getEvaluations(
        @Query("current") current: Int = 1,
        @Query("size") size: Int = 100,
        @Query("rating") rating: Int? = null,
    ): ApiEnvelope<PageDto<EvaluateDto>>

    @DELETE("evaluate/delete/{id}")
    suspend fun deleteEvaluation(
        @Path("id") id: Long,
    ): ApiEnvelope<Boolean>
}
