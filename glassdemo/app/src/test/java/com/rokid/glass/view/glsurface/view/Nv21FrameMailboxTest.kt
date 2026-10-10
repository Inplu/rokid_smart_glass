package com.rokid.glass.view.glsurface.view

import org.junit.Assert.assertEquals
import org.junit.Test

class Nv21FrameMailboxTest {

    @Test
    fun producerReplacesPendingFrameInsteadOfBuildingBacklog() {
        val mailbox = Nv21FrameMailbox()

        mailbox.submit(frame(1), 4, 2)
        mailbox.submit(frame(2), 4, 2)

        val latest = mailbox.acquireLatest()!!
        assertEquals(2, latest.y.get(0).toInt())
        assertEquals(1L, mailbox.droppedFrameCount)
        assertEquals(2L, mailbox.receivedFrameCount)
    }

    @Test
    fun renderingFrameIsNotOverwrittenByNextSubmission() {
        val mailbox = Nv21FrameMailbox()
        mailbox.submit(frame(1), 4, 2)
        val rendering = mailbox.acquireLatest()!!

        mailbox.submit(frame(2), 4, 2)

        assertEquals(1, rendering.y.get(0).toInt())
        assertEquals(2, mailbox.acquireLatest()!!.y.get(0).toInt())
    }

    @Test
    fun clearStartsASeparateMetricsSession() {
        val mailbox = Nv21FrameMailbox()
        mailbox.submit(frame(1), 4, 2)
        mailbox.acquireLatest()

        mailbox.clear()

        assertEquals(0L, mailbox.receivedFrameCount)
        assertEquals(0L, mailbox.renderedFrameCount)
        assertEquals(0L, mailbox.droppedFrameCount)
    }

    private fun frame(value: Int): ByteArray = ByteArray(12) { value.toByte() }
}
