package com.jschaofan.ragagent.core.network

/**
 * Repository 对外暴露的统一结果，避免 UI 直接处理 HTTP 异常。
 */
sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>

    data class Failure(
        val message: String,
        val code: String? = null,
        val cause: Throwable? = null,
    ) : ApiResult<Nothing>
}
