package com.jschaofan.ragagent.domain.auth.model

data class LoginSession(
    val token: String,
    val userId: Long,
    val userType: Int,
    val identifier: String,
)
