package com.jschaofan.ragagent.domain.chat.model

/**
 * 一条聊天消息在客户端中的生命周期状态。
 */
enum class MessageStatus {
    SENDING,
    STREAMING,
    COMPLETED,
    STOPPED,
    FAILED,
}
