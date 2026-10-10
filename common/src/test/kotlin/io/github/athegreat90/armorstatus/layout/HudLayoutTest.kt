package io.github.athegreat90.armorstatus.layout

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.util.stream.Stream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class HudLayoutTest {
    private fun layout(
        anchor: HudAnchor = HudAnchor.MIDDLE_LEFT,
        width: Int = 427,
        height: Int = 240,
        scale: Float = 1.0f,
        fontHeight: Int = 9,
        rows: Int = 4,
    ) = HudLayout.compute(width, height, scale, fontHeight, anchor, rows)

    @Test
    fun `row height is the larger of icon and font height plus two`() {
        assertEquals(18, layout(fontHeight = 9).rowHeight)
        assertEquals(18, layout(fontHeight = 16).rowHeight)
        assertEquals(22, layout(fontHeight = 20).rowHeight)
    }

    @Test
    fun `total height is rows times row height`() {
        assertEquals(72, layout(rows = 4).totalHeight)
        assertEquals(18, layout(rows = 1).totalHeight)
        assertEquals(0, layout(rows = 0).totalHeight)
    }

    @ParameterizedTest(name = "{0} at {1}x{2}, scale {3}: originY={4}")
    @CsvSource(
        "TOP_LEFT, 427, 240, 1.0, 4",
        "TOP_RIGHT, 427, 240, 1.0, 4",
        "MIDDLE_LEFT, 427, 240, 1.0, 84",
        "MIDDLE_RIGHT, 427, 240, 1.0, 84",
        "BOTTOM_LEFT, 427, 240, 1.0, 164",
        "BOTTOM_RIGHT, 427, 240, 1.0, 164",
        // scale 2.0 halves the logical height: 120 -> middle (120-72)/2, bottom 120-72-4
        "MIDDLE_LEFT, 427, 240, 2.0, 24",
        "BOTTOM_LEFT, 427, 240, 2.0, 44",
        // scale 0.5 doubles it: 480
        "MIDDLE_LEFT, 427, 240, 0.5, 204",
        "BOTTOM_LEFT, 427, 240, 0.5, 404",
        // odd leftover truncates: (241 - 72) / 2 = 84
        "MIDDLE_LEFT, 427, 241, 1.0, 84",
    )
    fun `vertical origin follows the anchor`(
        anchor: HudAnchor,
        width: Int,
        height: Int,
        scale: Float,
        expectedY: Int,
    ) {
        assertEquals(expectedY, layout(anchor = anchor, width = width, height = height, scale = scale).originY)
    }

    @ParameterizedTest(name = "{0} at width {1}, scale {2}: iconX={3}, textX={4}")
    @CsvSource(
        // left anchors: icon at the margin, text right after the icon and a 2 px gap
        "TOP_LEFT, 427, 1.0, 4, 22",
        "MIDDLE_LEFT, 854, 0.5, 4, 22",
        "BOTTOM_LEFT, 427, 2.0, 4, 22",
        // right anchors: icon on the outer edge, text right edge 2 px left of the icon
        "TOP_RIGHT, 427, 1.0, 407, 405",
        "MIDDLE_RIGHT, 800, 2.0, 380, 378",
        "BOTTOM_RIGHT, 800, 0.5, 1580, 1578",
    )
    fun `horizontal positions follow the anchor side`(
        anchor: HudAnchor,
        width: Int,
        scale: Float,
        expectedIconX: Int,
        expectedTextX: Int,
    ) {
        val result = layout(anchor = anchor, width = width, scale = scale)
        assertEquals(expectedIconX, result.iconX)
        assertEquals(expectedTextX, result.textX)
    }

    @Test
    fun `text is right aligned only for right anchors`() {
        HudAnchor.entries.forEach { anchor ->
            assertEquals(!anchor.isLeft, layout(anchor = anchor).textRightAligned, "$anchor")
        }
    }

    @ParameterizedTest(name = "font {0} -> text offset {1}")
    @CsvSource("9, 4", "8, 5", "16, 1", "20, -1")
    fun `text is vertically centred on the icon`(
        fontHeight: Int,
        expectedOffset: Int,
    ) {
        assertEquals(expectedOffset, layout(fontHeight = fontHeight).textOffsetY)
    }

    @Test
    fun `rows are stacked from the origin`() {
        val result = layout(anchor = HudAnchor.TOP_LEFT)
        assertEquals(listOf(4, 22, 40, 58), (0 until 4).map(result::rowY))
    }

    @Test
    fun `rowY rejects an index outside the rows`() {
        val result = layout(rows = 2)
        assertFailsWith<IllegalArgumentException> { result.rowY(-1) }
        assertFailsWith<IllegalArgumentException> { result.rowY(2) }
    }

    @Test
    fun `zero rows is a valid empty layout`() {
        val result = layout(anchor = HudAnchor.BOTTOM_RIGHT, rows = 0)
        assertEquals(0, result.totalHeight)
        assertEquals(236, result.originY)
        assertFailsWith<IllegalArgumentException> { result.rowY(0) }
    }

    @ParameterizedTest(name = "scale {0} is rejected")
    @ValueSource(floats = [0.0f, -1.0f, Float.NaN, Float.POSITIVE_INFINITY])
    fun `scale must be positive and finite`(scale: Float) {
        assertFailsWith<IllegalArgumentException> { layout(scale = scale) }
    }

    @Test
    fun `sizes and counts must not be negative`() {
        assertFailsWith<IllegalArgumentException> { layout(width = -1) }
        assertFailsWith<IllegalArgumentException> { layout(height = -1) }
        assertFailsWith<IllegalArgumentException> { layout(fontHeight = -1) }
        assertFailsWith<IllegalArgumentException> { layout(rows = -1) }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allAnchorsAndScales")
    fun `every anchor, scale and gui size keeps the rows inside the logical screen`(
        anchor: HudAnchor,
        scale: Float,
        width: Int,
        height: Int,
    ) {
        val result = layout(anchor = anchor, width = width, height = height, scale = scale)
        val logicalWidth = (width / scale).toInt()
        val logicalHeight = (height / scale).toInt()
        assertTrue(result.originY >= 0, "starts above the screen: ${result.originY}")
        assertTrue(result.originY + result.totalHeight <= logicalHeight, "ends below the screen")
        assertTrue(result.iconX >= 0 && result.iconX + HudLayout.ICON_SIZE <= logicalWidth, "icon leaves the screen")
    }

    companion object {
        @JvmStatic
        fun allAnchorsAndScales(): Stream<Arguments> =
            HudAnchor.entries
                .flatMap { anchor ->
                    listOf(0.5f, 1.0f, 1.5f, 2.0f).flatMap { scale ->
                        listOf(427 to 240, 320 to 180, 960 to 540).map { (w, h) -> Arguments.of(anchor, scale, w, h) }
                    }
                }.stream()
    }
}
