package com.jschaofan.ragagent.core.network

import okhttp3.Interceptor
import okhttp3.Response

/**
 * 为共享 OkHttpClient 发出的每个请求自动添加当前登录 Token。
 *
 * 当前后端协议使用 `token` 请求头。请求头名称和 Token 前缀保持可配置，
 * 方便后端以后切换到 `Authorization: Bearer ...`。
 */
class AuthInterceptor(
    private val tokenProvider: TokenProvider,
    private val onUnauthorized: () -> Unit = {},
    private val headerName: String = DEFAULT_TOKEN_HEADER,
    private val tokenPrefix: String = "",
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenProvider.getToken()
        if (token.isNullOrBlank()) {
            return chain.proceed(chain.request()).also { response ->
                if (response.code == 401) onUnauthorized()
            }
        }

        // 使用 header() 覆盖旧值，避免重试时重复携带认证信息。
        val authenticatedRequest = chain.request()
            .newBuilder()
            .header(headerName, "$tokenPrefix$token")
            .build()

        return chain.proceed(authenticatedRequest).also { response ->
            if (response.code == 401) onUnauthorized()
        }
    }

    companion object {
        const val DEFAULT_TOKEN_HEADER = "token"
    }
}
