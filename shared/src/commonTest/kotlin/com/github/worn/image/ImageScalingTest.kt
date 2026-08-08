package com.github.worn.image

import com.github.worn.data.source.image.AiImageLimits
import com.github.worn.data.source.image.ImageScaling
import com.github.worn.data.source.image.isJpeg
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ImageScalingTest {

    @Test
    fun `fitLongEdge scales a landscape photo by its width`() {
        val target = ImageScaling.fitLongEdge(4032, 3024, AiImageLimits.CLAUDE_MAX_EDGE)

        assertEquals(AiImageLimits.CLAUDE_MAX_EDGE, target?.width)
        assertEquals(1176, target?.height)
    }

    @Test
    fun `fitLongEdge scales a portrait photo by its height`() {
        val target = ImageScaling.fitLongEdge(3024, 4032, AiImageLimits.CLAUDE_MAX_EDGE)

        assertEquals(1176, target?.width)
        assertEquals(AiImageLimits.CLAUDE_MAX_EDGE, target?.height)
    }

    @Test
    fun `fitLongEdge returns null when the image already fits`() {
        assertNull(ImageScaling.fitLongEdge(1200, 900, AiImageLimits.CLAUDE_MAX_EDGE))
        assertNull(ImageScaling.fitLongEdge(1568, 1568, AiImageLimits.CLAUDE_MAX_EDGE))
    }

    @Test
    fun `fitLongEdge never returns a zero edge for an extreme aspect ratio`() {
        val target = ImageScaling.fitLongEdge(8000, 3, AiImageLimits.CLAUDE_MAX_EDGE)

        assertEquals(AiImageLimits.CLAUDE_MAX_EDGE, target?.width)
        assertEquals(1, target?.height)
    }

    @Test
    fun `fitLongEdge rejects a degenerate size`() {
        assertNull(ImageScaling.fitLongEdge(0, 0, AiImageLimits.CLAUDE_MAX_EDGE))
        assertNull(ImageScaling.fitLongEdge(-10, 100, AiImageLimits.CLAUDE_MAX_EDGE))
    }

    @Test
    fun `sampleSizeFor stops before the long edge drops under the target`() {
        // 4032 -> 2016 still clears 1568; halving again to 1008 would undershoot.
        assertEquals(2, ImageScaling.sampleSizeFor(4032, 3024, AiImageLimits.CLAUDE_MAX_EDGE))
        // 8000 -> 4000 -> 2000, and 1000 would undershoot.
        assertEquals(4, ImageScaling.sampleSizeFor(8000, 6000, AiImageLimits.CLAUDE_MAX_EDGE))
    }

    @Test
    fun `sampleSizeFor leaves a small image untouched`() {
        assertEquals(1, ImageScaling.sampleSizeFor(800, 600, AiImageLimits.CLAUDE_MAX_EDGE))
    }

    @Test
    fun `sampleSizeFor terminates on a non-positive target`() {
        assertEquals(1, ImageScaling.sampleSizeFor(4032, 3024, 0))
    }

    @Test
    fun `isJpeg recognises the SOI marker and rejects other formats`() {
        val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())
        val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)

        assertTrue(jpeg.isJpeg())
        assertFalse(png.isJpeg())
        assertFalse(byteArrayOf(0xFF.toByte(), 0xD8.toByte()).isJpeg())
        assertFalse(ByteArray(0).isJpeg())
    }
}
