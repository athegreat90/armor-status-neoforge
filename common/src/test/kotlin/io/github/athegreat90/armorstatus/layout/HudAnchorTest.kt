package io.github.athegreat90.armorstatus.layout

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.Test
import kotlin.test.assertEquals

class HudAnchorTest {
    @ParameterizedTest(name = "{0}: left={1}, vertical={2}")
    @CsvSource(
        "TOP_LEFT, true, TOP",
        "MIDDLE_LEFT, true, MIDDLE",
        "BOTTOM_LEFT, true, BOTTOM",
        "TOP_RIGHT, false, TOP",
        "MIDDLE_RIGHT, false, MIDDLE",
        "BOTTOM_RIGHT, false, BOTTOM",
    )
    fun `anchor exposes its horizontal and vertical side`(
        anchor: HudAnchor,
        isLeft: Boolean,
        vertical: VerticalAlign,
    ) {
        assertEquals(isLeft, anchor.isLeft)
        assertEquals(vertical, anchor.vertical)
    }

    @Test
    fun `there are exactly six anchors`() {
        assertEquals(6, HudAnchor.entries.size)
    }
}
