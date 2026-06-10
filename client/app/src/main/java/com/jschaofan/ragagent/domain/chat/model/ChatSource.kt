package com.jschaofan.ragagent.domain.chat.model

data class ChatSource(
    val title: String,
    val sourceType: String,
    val content: String?,
    val ragSource: RagSource?,
)

data class RagSource(
    val productInfo: String,
    val marketingDescription: String,
    val officialFaq: List<Faq>,
    val userReviews: List<UserReview>,
)

data class Faq(
    val question: String,
    val answer: String,
)

data class UserReview(
    val nickname: String,
    val rating: String,
    val content: String,
)
