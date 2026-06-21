package com.jschaofan.ragagent.data.repository

import com.jschaofan.ragagent.core.network.ApiResult
import com.jschaofan.ragagent.data.remote.api.AuthApi
import com.jschaofan.ragagent.data.remote.dto.LoginRequestDto
import com.jschaofan.ragagent.domain.auth.model.LoginSession
import com.jschaofan.ragagent.domain.auth.repository.AuthRepository

class AuthRepositoryImpl(
    private val authApi: AuthApi,
) : AuthRepository {
    override suspend fun login(identifier: String, password: String): ApiResult<LoginSession> =
        runCatching { authApi.login(LoginRequestDto(identifier, password)) }.fold(
            onSuccess = { response ->
                val data = response.data
                // 联调环境成功时 code 为 1，与文档示例的 0 不一致，因此以有效 token 为准。
                if (data != null && data.token.isNotBlank()) {
                    ApiResult.Success(
                        LoginSession(data.token, data.userId, data.type, data.identifier),
                    )
                } else {
                    ApiResult.Failure(response.msg.orEmpty().ifBlank { "登录失败，请检查账号和密码" })
                }
            },
            onFailure = { error ->
                ApiResult.Failure(
                    message = error.message ?: "网络连接失败，请稍后重试",
                    cause = error,
                )
            },
        )

    override suspend fun logout(): ApiResult<Unit> =
        runCatching { authApi.logout() }.fold(
            onSuccess = { response ->
                if (response.code == 0 || !response.data.isNullOrBlank()) {
                    ApiResult.Success(Unit)
                } else {
                    ApiResult.Failure(response.msg.orEmpty().ifBlank { "退出登录失败" })
                }
            },
            onFailure = { error ->
                ApiResult.Failure(error.message ?: "退出登录失败", cause = error)
            },
        )
}
