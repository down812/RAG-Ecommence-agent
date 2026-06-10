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
}
