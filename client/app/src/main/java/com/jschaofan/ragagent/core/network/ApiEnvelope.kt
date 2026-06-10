package com.jschaofan.ragagent.core.network

import kotlinx.serialization.Serializable

/**
 * 后端普通 HTTP 接口统一使用的响应外壳。
 */
@Serializable
data class ApiEnvelope<T>(
    val code: Int,
    // 后端成功响应也可能显式返回 null，客户端不能按非空字符串解析。
    val msg: String? = null,
    val data: T? = null,
)
