package com.jschaofan.ragagent.domain.chat.model

data class ChatEvaluation(
    val messageId: String,
    val rating: Int,
    val comment: String? = null,
)
