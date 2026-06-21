package com.jschaofan.ragagent.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class EvaluateRequestDto(
    val sessionId: String,
    val messageId: String,
    val rating: Int,
    val comment: String? = null,
)

@Serializable
data class EvaluateDto(
    val id: Long? = null,
    val userId: Long? = null,
    val sessionId: String,
    val messageId: String,
    val rating: Int,
    val comment: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)
