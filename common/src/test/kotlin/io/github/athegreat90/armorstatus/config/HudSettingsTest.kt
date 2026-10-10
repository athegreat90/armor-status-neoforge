package io.github.athegreat90.armorstatus.config

import io.github.athegreat90.armorstatus.display.DurabilityDisplayMode
import io.github.athegreat90.armorstatus.layout.HudAnchor
import io.github.athegreat90.armorstatus.model.ArmorSlot
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HudSettingsTest {
    private fun sanitized(
        anchorName: String? = "MIDDLE_LEFT",
        scale: Double = 1.0,
        displayModeName: String? = "RAW",
    ) = HudSettings.sanitized(
        enabled = true,
        anchorName = anchorName,
        scale = scale,
        showHead = true,
        showChest = true,
        showLegs = true,
        showFeet = true,
        displayModeName = displayModeName,
        gradientColors = false,
    )

    @Test
    fun `defaults match the upstream look`() {
        val defaults = HudSettings.Default
        assertTrue(defaults.enabled)
        assertEquals(HudAnchor.MIDDLE_LEFT, defaults.anchor)
        assertEquals(1.0f, defaults.scale)
        assertEquals(DurabilityDisplayMode.RAW, defaults.displayMode)
        assertFalse(defaults.gradientColors)
        ArmorSlot.entries.forEach { assertTrue(defaults.shows(it), "$it") }
    }

    @Test
    fun `scale bounds are 0_5 to 2_0`() {
        assertEquals(0.5f, MIN_SCALE)
        assertEquals(2.0f, MAX_SCALE)
    }

    @ParameterizedTest(name = "{0} is shown only when its toggle is on")
    @EnumSource(ArmorSlot::class)
    fun `each slot has its own toggle`(slot: ArmorSlot) {
        val allOff = HudSettings.Default.copy(showHead = false, showChest = false, showLegs = false, showFeet = false)
        val withSlot =
            when (slot) {
                ArmorSlot.HEAD -> allOff.copy(showHead = true)
                ArmorSlot.CHEST -> allOff.copy(showChest = true)
                ArmorSlot.LEGS -> allOff.copy(showLegs = true)
                ArmorSlot.FEET -> allOff.copy(showFeet = true)
            }
        ArmorSlot.entries.forEach { assertEquals(it == slot, withSlot.shows(it), "$it") }
        ArmorSlot.entries.forEach { assertFalse(allOff.shows(it), "$it") }
    }

    @ParameterizedTest(name = "scale {0} -> {1}")
    @CsvSource("1.0, 1.0", "0.5, 0.5", "2.0, 2.0", "0.1, 0.5", "-3.0, 0.5", "7.5, 2.0", "1.25, 1.25")
    fun `sanitized coerces scale into the allowed range`(
        scale: Double,
        expected: Float,
    ) {
        assertEquals(expected, sanitized(scale = scale).scale)
    }

    @Test
    fun `sanitized replaces NaN scale with the default`() {
        assertEquals(1.0f, sanitized(scale = Double.NaN).scale)
    }

    @Test
    fun `sanitized clamps infinite scale to the bounds`() {
        assertEquals(MAX_SCALE, sanitized(scale = Double.POSITIVE_INFINITY).scale)
        assertEquals(MIN_SCALE, sanitized(scale = Double.NEGATIVE_INFINITY).scale)
    }

    @ParameterizedTest(name = "anchor \"{0}\" -> {1}")
    @CsvSource("top_right, TOP_RIGHT", "BOTTOM_LEFT, BOTTOM_LEFT", "' Middle_Right ', MIDDLE_RIGHT")
    fun `sanitized parses anchor names case-insensitively`(
        name: String,
        expected: HudAnchor,
    ) {
        assertEquals(expected, sanitized(anchorName = name).anchor)
    }

    @ParameterizedTest(name = "unknown anchor \"{0}\" -> default")
    @ValueSource(strings = ["", "centre", "TOP"])
    fun `sanitized falls back to the default anchor`(name: String) {
        assertEquals(HudSettings.Default.anchor, sanitized(anchorName = name).anchor)
    }

    @Test
    fun `sanitized falls back to the default anchor and mode for null names`() {
        val result = sanitized(anchorName = null, displayModeName = null)
        assertEquals(HudSettings.Default.anchor, result.anchor)
        assertEquals(HudSettings.Default.displayMode, result.displayMode)
    }

    @ParameterizedTest(name = "mode \"{0}\" -> {1}")
    @CsvSource("percent, PERCENT", "RAW, RAW", "Percent, PERCENT")
    fun `sanitized parses display mode names case-insensitively`(
        name: String,
        expected: DurabilityDisplayMode,
    ) {
        assertEquals(expected, sanitized(displayModeName = name).displayMode)
    }

    @Test
    fun `sanitized falls back to the default mode for unknown names`() {
        assertEquals(DurabilityDisplayMode.RAW, sanitized(displayModeName = "fraction").displayMode)
    }

    @Test
    fun `sanitized keeps every other value as given`() {
        val result =
            HudSettings.sanitized(
                enabled = false,
                anchorName = "TOP_RIGHT",
                scale = 1.5,
                showHead = false,
                showChest = true,
                showLegs = false,
                showFeet = true,
                displayModeName = "PERCENT",
                gradientColors = true,
            )
        assertEquals(
            HudSettings(
                enabled = false,
                anchor = HudAnchor.TOP_RIGHT,
                scale = 1.5f,
                showHead = false,
                showChest = true,
                showLegs = false,
                showFeet = true,
                displayMode = DurabilityDisplayMode.PERCENT,
                gradientColors = true,
            ),
            result,
        )
    }

    @Test
    fun `the constructor rejects an out-of-range scale`() {
        assertFailsWith<IllegalArgumentException> { HudSettings.Default.copy(scale = 0.4f) }
        assertFailsWith<IllegalArgumentException> { HudSettings.Default.copy(scale = 2.1f) }
        assertFailsWith<IllegalArgumentException> { HudSettings.Default.copy(scale = Float.NaN) }
    }
}
