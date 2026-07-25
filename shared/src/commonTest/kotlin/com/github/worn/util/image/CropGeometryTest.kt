package com.github.worn.util.image

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CropGeometryTest {

    @Test
    fun fitBounds_letterboxes_a_wide_image_in_a_square_view() {
        val bounds = CropGeometry.fitBounds(imageWidth = 1600, imageHeight = 900, viewWidth = 400f, viewHeight = 400f)

        assertEquals(0f, bounds.left)
        assertEquals(400f, bounds.right)
        assertEquals(225f, bounds.height)
        assertEquals(87.5f, bounds.top)
        assertEquals(312.5f, bounds.bottom)
    }

    @Test
    fun fitBounds_pillarboxes_a_tall_image_in_a_square_view() {
        val bounds = CropGeometry.fitBounds(imageWidth = 900, imageHeight = 1600, viewWidth = 400f, viewHeight = 400f)

        assertEquals(0f, bounds.top)
        assertEquals(400f, bounds.bottom)
        assertEquals(225f, bounds.width)
        assertEquals(87.5f, bounds.left)
    }

    @Test
    fun toSourceRect_returns_the_whole_image_when_the_selection_fills_the_bounds() {
        val bounds = CropGeometry.fitBounds(1600, 900, 400f, 400f)

        val rect = CropGeometry.toSourceRect(bounds, bounds, imageWidth = 1600, imageHeight = 900)

        assertEquals(CropRect(left = 0, top = 0, width = 1600, height = 900), rect)
    }

    @Test
    fun toSourceRect_maps_an_offset_half_width_selection() {
        val bounds = CropViewRect(left = 0f, top = 0f, right = 400f, bottom = 225f)
        val selection = CropViewRect(left = 200f, top = 0f, right = 400f, bottom = 225f)

        val rect = CropGeometry.toSourceRect(selection, bounds, imageWidth = 1600, imageHeight = 900)

        assertEquals(CropRect(left = 800, top = 0, width = 800, height = 900), rect)
    }

    @Test
    fun toSourceRect_stays_inside_the_image_when_float_error_pushes_past_the_edge() {
        val bounds = CropViewRect(left = 0f, top = 0f, right = 400f, bottom = 225f)
        // 0.4px of accumulated drag error past the right/bottom edges.
        val selection = CropViewRect(left = 0f, top = 0f, right = 400.4f, bottom = 225.4f)

        val rect = CropGeometry.toSourceRect(selection, bounds, imageWidth = 1600, imageHeight = 900)

        assertTrue(rect.left + rect.width <= 1600, "right edge escaped: $rect")
        assertTrue(rect.top + rect.height <= 900, "bottom edge escaped: $rect")
    }

    @Test
    fun toSourceRect_never_returns_an_empty_rect() {
        val bounds = CropViewRect(left = 0f, top = 0f, right = 400f, bottom = 225f)
        val degenerate = CropViewRect(left = 400f, top = 225f, right = 400f, bottom = 225f)

        val rect = CropGeometry.toSourceRect(degenerate, bounds, imageWidth = 1600, imageHeight = 900)

        assertTrue(rect.width >= 1 && rect.height >= 1, "empty rect: $rect")
        assertTrue(rect.left + rect.width <= 1600 && rect.top + rect.height <= 900, "escaped: $rect")
    }

    @Test
    fun resizeSelection_clamps_a_corner_at_the_minimum_edge() {
        val bounds = CropViewRect(0f, 0f, 400f, 400f)
        val base = CropViewRect(0f, 0f, 400f, 400f)

        val resized = CropGeometry.resizeSelection(base, CropCorner.TOP_LEFT, 999f, 999f, bounds, minEdge = 48f)

        assertEquals(352f, resized.left)
        assertEquals(352f, resized.top)
        assertEquals(48f, resized.width)
        assertEquals(48f, resized.height)
    }

    @Test
    fun resizeSelection_never_escapes_the_bounds() {
        val bounds = CropViewRect(50f, 20f, 350f, 220f)
        val base = CropViewRect(100f, 60f, 300f, 180f)

        val resized = CropGeometry.resizeSelection(base, CropCorner.BOTTOM_RIGHT, 999f, 999f, bounds, minEdge = 48f)

        assertEquals(bounds.right, resized.right)
        assertEquals(bounds.bottom, resized.bottom)
        assertEquals(base.left, resized.left)
        assertEquals(base.top, resized.top)
    }

    @Test
    fun moveSelection_preserves_the_size_and_clamps_at_the_bounds_edges() {
        val bounds = CropViewRect(50f, 20f, 350f, 220f)
        val base = CropViewRect(100f, 60f, 200f, 160f)

        val moved = CropGeometry.moveSelection(base, dx = 999f, dy = 999f, bounds = bounds)

        assertEquals(base.width, moved.width)
        assertEquals(base.height, moved.height)
        assertEquals(bounds.right, moved.right)
        assertEquals(bounds.bottom, moved.bottom)
    }

    @Test
    fun moveSelection_clamps_a_selection_larger_than_the_bounds_to_the_top_left() {
        val bounds = CropViewRect(0f, 0f, 100f, 100f)
        val oversized = CropViewRect(0f, 0f, 200f, 200f)

        val moved = CropGeometry.moveSelection(oversized, dx = -50f, dy = -50f, bounds = bounds)

        assertEquals(bounds.left, moved.left)
        assertEquals(bounds.top, moved.top)
    }
}
