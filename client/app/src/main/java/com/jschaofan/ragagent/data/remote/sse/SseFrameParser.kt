package com.jschaofan.ragagent.data.remote.sse

/**
 * 按行接收 SSE 文本，并逐步组合成完整事件。
 *
 * SSE 使用空行标记一条事件结束；同一事件可以包含多行 data，需要用换行符拼接。
 */
class SseFrameParser {
    private var event = DEFAULT_EVENT
    private var id: String? = null
    private val dataLines = mutableListOf<String>()

    fun acceptLine(line: String): SseFrame? {
        if (line.isEmpty()) {
            return dispatch()
        }
        // 以 ':' 开头的是服务端心跳注释，不属于业务事件。
        if (line.startsWith(':')) {
            return null
        }

        val separatorIndex = line.indexOf(':')
        val field = if (separatorIndex >= 0) line.substring(0, separatorIndex) else line
        val rawValue = if (separatorIndex >= 0) line.substring(separatorIndex + 1) else ""
        val value = rawValue.removePrefix(" ")

        when (field) {
            "event" -> event = value.ifBlank { DEFAULT_EVENT }
            "data" -> dataLines += value
            "id" -> id = value
        }
        return null
    }

    fun finish(): SseFrame? = dispatch()

    private fun dispatch(): SseFrame? {
        if (dataLines.isEmpty()) {
            resetEvent()
            return null
        }

        // 此处只负责 SSE 分帧，不解析 JSON，避免协议处理和业务解析耦合。
        val frame = SseFrame(
            event = event,
            data = dataLines.joinToString(separator = "\n"),
            id = id,
        )
        dataLines.clear()
        resetEvent()
        return frame
    }

    private fun resetEvent() {
        event = DEFAULT_EVENT
    }

    private companion object {
        const val DEFAULT_EVENT = "message"
    }
}
