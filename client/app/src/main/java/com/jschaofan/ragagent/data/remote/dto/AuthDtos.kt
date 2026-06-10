package com.jschaofan.ragagent.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequestDto(val identifier: String, val password: String)

@Serializable
data class LoginResponseDto(
    val token: String,
    val userId: Long,
    val type: Int,
    val identifier: String,
)
