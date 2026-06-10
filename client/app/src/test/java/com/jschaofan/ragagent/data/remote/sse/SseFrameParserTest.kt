package com.jschaofan.ragagent.data.remote.sse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SseFrameParserTest {
    @Test
    fun `dispatches frame when blank line arrives`() {
        val parser = SseFrameParser()

        assertNull(parser.acceptLine("event:content"))
        assertNull(parser.acceptLine("id:42"))
        assertNull(parser.acceptLine("""data:{"type":"content","data":"hello"}"""))

        assertEquals(
            SseFrame(
                event = "content",
                data = """{"type":"content","data":"hello"}""",
                id = "42",
            ),
            parser.acceptLine(""),
        )
    }

    @Test
    fun `joins multiple data lines and ignores heartbeat comments`() {
        val parser = SseFrameParser()

        assertNull(parser.acceptLine(": heartbeat"))
        assertNull(parser.acceptLine("event:message"))
        assertNull(parser.acceptLine("data:first"))
        assertNull(parser.acceptLine("data: second"))

        assertEquals(
            SseFrame(
                event = "message",
                data = "first\nsecond",
            ),
            parser.acceptLine(""),
        )
    }
}
