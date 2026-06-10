package com.jschaofan.ragagent.data.repository

import com.jschaofan.ragagent.core.network.ApiResult
import com.jschaofan.ragagent.data.mapper.toDomain
import com.jschaofan.ragagent.data.remote.api.ProductApi
import com.jschaofan.ragagent.domain.product.model.ProductDetail
import com.jschaofan.ragagent.domain.product.repository.ProductRepository
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException

class ProductRepositoryImpl(
    private val productApi: ProductApi,
) : ProductRepository {
    override suspend fun getProductDetail(productId: Long): ApiResult<ProductDetail> {
        return try {
            val response = productApi.getProductDetail(productId)
            when {
                response.data == null -> ApiResult.Failure(
                    message = "服务没有返回商品详情",
                    code = response.code.toString(),
                )

                else -> ApiResult.Success(response.data.toDomain())
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: HttpException) {
            ApiResult.Failure(
                message = if (exception.code() == HTTP_NOT_FOUND) {
                    "商品不存在或已被删除"
                } else {
                    "商品详情请求失败（HTTP ${exception.code()}）"
                },
                code = exception.code().toString(),
                cause = exception,
            )
        } catch (exception: SerializationException) {
            ApiResult.Failure(
                message = "商品详情数据格式无法解析",
                cause = exception,
            )
        } catch (exception: IOException) {
            ApiResult.Failure(
                message = "网络连接失败，请检查网络后重试",
                cause = exception,
            )
        } catch (exception: Throwable) {
            ApiResult.Failure(
                message = exception.message ?: "加载商品详情时发生未知错误",
                cause = exception,
            )
        }
    }

    private companion object {
        const val HTTP_NOT_FOUND = 404
    }
}
