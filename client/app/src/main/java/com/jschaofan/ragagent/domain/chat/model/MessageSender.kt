package com.jschaofan.ragagent.domain.chat.model

/**
 * 消息发送方。
 *
 * 这里描述的是聊天消息由谁发出，不是用户账号的权限角色。
 */
enum class MessageSender {
    USER,
    ASSISTANT,
    UNKNOWN,
}
