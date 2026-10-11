package io.github.athegreat90.armorstatus.config

import io.github.athegreat90.armorstatus.display.ColorScheme
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
        debugScreenModeName: String? = "COMPACT",
        debugScreenScale: Double = 0.6,
        debugScreenAnchorName: String? = "BOTTOM_RIGHT",
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
        debugScreenModeName = debugScreenModeName,
        debugScreenScale = debugScreenScale,
        debugScreenAnchorName = debugScreenAnchorName,
    )

    @Test
    fun `defaults match the upstream look`() {
        val defaults = HudSettings.Default
        assertTrue(defaults.enabled)
        assertEquals(HudAnchor.MIDDLE_LEFT, defaults.anchor)
        assertEquals(1.0f, defaults.scale)
        assertEquals(DurabilityDisplayMode.RAW, defaults.displayMode)
        assertFalse(defaults.gradientColors)
        assertEquals(DebugScreenMode.COMPACT, defaults.debugScreenMode)
        assertEquals(0.6f, defaults.debugScreenScale)
        assertEquals(HudAnchor.BOTTOM_RIGHT, defaults.debugScreenAnchor)
        ArmorSlot.entries.forEach { assertTrue(defaults.shows(it), "$it") }
    }

    @Test
    fun `scale bounds are 0_5 to 2_0`() {
        assertEquals(0.5f, MIN_SCALE)
        assertEquals(2.0f, MAX_SCALE)
    }

    @Test
    fun `debug scale bounds are 0_25 to 1_0`() {
        assertEquals(0.25f, MIN_DEBUG_SCALE)
        assertEquals(1.0f, MAX_DEBUG_SCALE)
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
                debugScreenModeName = "KEEP",
                debugScreenScale = 0.4,
                debugScreenAnchorName = "TOP_LEFT",
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
                debugScreenMode = DebugScreenMode.KEEP,
                debugScreenScale = 0.4f,
                debugScreenAnchor = HudAnchor.TOP_LEFT,
            ),
            result,
        )
    }

    @ParameterizedTest(name = "debug mode \"{0}\" -> {1}")
    @CsvSource("compact, COMPACT", "HIDE, HIDE", "' Keep ', KEEP", "Hide, HIDE")
    fun `sanitized parses debug screen mode names case-insensitively`(
        name: String,
        expected: DebugScreenMode,
    ) {
        assertEquals(expected, sanitized(debugScreenModeName = name).debugScreenMode)
    }

    @ParameterizedTest(name = "unknown debug mode \"{0}\" -> COMPACT")
    @ValueSource(strings = ["", "  ", "true", "false", "shrink"])
    fun `sanitized falls back to compact for unknown debug screen modes`(name: String) {
        assertEquals(DebugScreenMode.COMPACT, sanitized(debugScreenModeName = name).debugScreenMode)
    }

    @Test
    fun `sanitized falls back to compact for a null debug screen mode`() {
        assertEquals(DebugScreenMode.COMPACT, sanitized(debugScreenModeName = null).debugScreenMode)
    }

    @ParameterizedTest(name = "debug scale {0} -> {1}")
    @CsvSource("0.6, 0.6", "0.25, 0.25", "1.0, 1.0", "0.1, 0.25", "-2.0, 0.25", "3.0, 1.0", "0.5, 0.5")
    fun `sanitized coerces the debug scale into the allowed range`(
        scale: Double,
        expected: Float,
    ) {
        assertEquals(expected, sanitized(debugScreenScale = scale).debugScreenScale)
    }

    @Test
    fun `sanitized replaces a NaN debug scale with the default and clamps infinities`() {
        assertEquals(0.6f, sanitized(debugScreenScale = Double.NaN).debugScreenScale)
        assertEquals(MAX_DEBUG_SCALE, sanitized(debugScreenScale = Double.POSITIVE_INFINITY).debugScreenScale)
        assertEquals(MIN_DEBUG_SCALE, sanitized(debugScreenScale = Double.NEGATIVE_INFINITY).debugScreenScale)
    }

    @ParameterizedTest(name = "mode {0}, f3 closed -> the plain scale")
    @EnumSource(DebugScreenMode::class)
    fun `the effective scale ignores the debug scale while f3 is closed`(mode: DebugScreenMode) {
        val settings = HudSettings.Default.copy(scale = 1.5f, debugScreenMode = mode, debugScreenScale = 0.5f)
        assertEquals(1.5f, settings.effectiveScale(debugScreenOpen = false))
    }

    @ParameterizedTest(name = "mode {0}, f3 open -> {1}")
    @CsvSource("COMPACT, 0.75", "HIDE, 1.5", "KEEP, 1.5")
    fun `only compact shrinks the hud while f3 is open`(
        mode: DebugScreenMode,
        expected: Float,
    ) {
        val settings = HudSettings.Default.copy(scale = 1.5f, debugScreenMode = mode, debugScreenScale = 0.5f)
        assertEquals(expected, settings.effectiveScale(debugScreenOpen = true))
    }

    @Test
    fun `the default settings draw at 0_6 while f3 is open and at 1_0 otherwise`() {
        assertEquals(0.6f, HudSettings.Default.effectiveScale(debugScreenOpen = true))
        assertEquals(1.0f, HudSettings.Default.effectiveScale(debugScreenOpen = false))
    }

    @Test
    fun `the effective scale stays positive at the smallest scale and debug scale`() {
        val settings = HudSettings.Default.copy(scale = MIN_SCALE, debugScreenScale = MIN_DEBUG_SCALE)
        assertEquals(0.125f, settings.effectiveScale(debugScreenOpen = true))
    }

    @ParameterizedTest(name = "debug anchor \"{0}\" -> {1}")
    @CsvSource(
        "top_left, TOP_LEFT",
        "BOTTOM_RIGHT, BOTTOM_RIGHT",
        "' Middle_Right ', MIDDLE_RIGHT",
        "bottom_left, BOTTOM_LEFT",
    )
    fun `sanitized parses debug screen anchor names case-insensitively`(
        name: String,
        expected: HudAnchor,
    ) {
        assertEquals(expected, sanitized(debugScreenAnchorName = name).debugScreenAnchor)
    }

    @ParameterizedTest(name = "unknown debug anchor \"{0}\" -> BOTTOM_RIGHT")
    @ValueSource(strings = ["", "  ", "centre", "TOP", "true"])
    fun `sanitized falls back to bottom right for unknown debug screen anchors`(name: String) {
        assertEquals(HudAnchor.BOTTOM_RIGHT, sanitized(debugScreenAnchorName = name).debugScreenAnchor)
    }

    @Test
    fun `sanitized falls back to bottom right for a null debug screen anchor`() {
        assertEquals(HudAnchor.BOTTOM_RIGHT, sanitized(debugScreenAnchorName = null).debugScreenAnchor)
    }

    @Test
    fun `sanitized keeps the debug screen anchor independent of the normal anchor`() {
        val result = sanitized(anchorName = "TOP_LEFT", debugScreenAnchorName = "MIDDLE_RIGHT")
        assertEquals(HudAnchor.TOP_LEFT, result.anchor)
        assertEquals(HudAnchor.MIDDLE_RIGHT, result.debugScreenAnchor)
    }

    @ParameterizedTest(name = "mode {0}, f3 closed -> the normal anchor")
    @EnumSource(DebugScreenMode::class)
    fun `the effective anchor ignores the debug anchor while f3 is closed`(mode: DebugScreenMode) {
        val settings =
            HudSettings.Default.copy(
                anchor = HudAnchor.TOP_LEFT,
                debugScreenMode = mode,
                debugScreenAnchor = HudAnchor.BOTTOM_RIGHT,
            )
        assertEquals(HudAnchor.TOP_LEFT, settings.effectiveAnchor(debugScreenOpen = false))
    }

    @ParameterizedTest(name = "mode {0}, f3 open -> {1}")
    @CsvSource("COMPACT, BOTTOM_RIGHT", "HIDE, TOP_LEFT", "KEEP, TOP_LEFT")
    fun `only compact uses the debug anchor while f3 is open`(
        mode: DebugScreenMode,
        expected: HudAnchor,
    ) {
        val settings =
            HudSettings.Default.copy(
                anchor = HudAnchor.TOP_LEFT,
                debugScreenMode = mode,
                debugScreenAnchor = HudAnchor.BOTTOM_RIGHT,
            )
        assertEquals(expected, settings.effectiveAnchor(debugScreenOpen = true))
    }

    @Test
    fun `the default settings move to the bottom right while f3 is open and stay middle left otherwise`() {
        assertEquals(HudAnchor.BOTTOM_RIGHT, HudSettings.Default.effectiveAnchor(debugScreenOpen = true))
        assertEquals(HudAnchor.MIDDLE_LEFT, HudSettings.Default.effectiveAnchor(debugScreenOpen = false))
    }

    @ParameterizedTest(name = "debug anchor {0} is used as is in compact mode")
    @EnumSource(HudAnchor::class)
    fun `every anchor can be the debug anchor`(anchor: HudAnchor) {
        val settings = HudSettings.Default.copy(debugScreenAnchor = anchor)
        assertEquals(anchor, settings.effectiveAnchor(debugScreenOpen = true))
    }

    @Test
    fun `the weapon tooltip settings default to the weapon tooltip defaults`() {
        assertEquals(WeaponTooltipSettings.Default, HudSettings().weaponTooltip)
        assertEquals(WeaponTooltipSettings.Default, HudSettings.Default.weaponTooltip)
    }

    @Test
    fun `the legacy sanitized argument list yields default weapon tooltip settings and unchanged armor fields`() {
        val legacy =
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
                debugScreenModeName = "KEEP",
                debugScreenScale = 0.4,
                debugScreenAnchorName = "TOP_LEFT",
            )
        assertEquals(WeaponTooltipSettings.Default, legacy.weaponTooltip)
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
                debugScreenMode = DebugScreenMode.KEEP,
                debugScreenScale = 0.4f,
                debugScreenAnchor = HudAnchor.TOP_LEFT,
            ),
            legacy,
        )
    }

    @Test
    fun `the shortest legacy sanitized call also keeps the weapon tooltip defaults`() {
        val result = sanitized()
        assertEquals(WeaponTooltipSettings.Default, result.weaponTooltip)
    }

    @Test
    fun `a custom weapon tooltip is carried through sanitized untouched`() {
        val custom =
            WeaponTooltipSettings(enabled = false, colorScheme = ColorScheme.WHITE, warningThresholdPercent = 40)
        val result =
            HudSettings.sanitized(
                enabled = true,
                anchorName = "MIDDLE_LEFT",
                scale = 1.0,
                showHead = true,
                showChest = true,
                showLegs = true,
                showFeet = true,
                displayModeName = "RAW",
                gradientColors = false,
                weaponTooltip = custom,
            )
        assertEquals(custom, result.weaponTooltip)
        assertEquals(HudSettings.Default.copy(weaponTooltip = custom), result)
    }

    @Test
    fun `changing the weapon tooltip leaves the armor settings alone`() {
        val changed = HudSettings.Default.copy(weaponTooltip = WeaponTooltipSettings(enabled = false))
        assertEquals(HudSettings.Default, changed.copy(weaponTooltip = WeaponTooltipSettings.Default))
        ArmorSlot.entries.forEach { assertTrue(changed.shows(it), "$it") }
    }

    @Test
    fun `the constructor rejects an out-of-range scale`() {
        assertFailsWith<IllegalArgumentException> { HudSettings.Default.copy(scale = 0.4f) }
        assertFailsWith<IllegalArgumentException> { HudSettings.Default.copy(scale = 2.1f) }
        assertFailsWith<IllegalArgumentException> { HudSettings.Default.copy(scale = Float.NaN) }
    }

    @Test
    fun `the constructor rejects an out-of-range debug scale`() {
        assertFailsWith<IllegalArgumentException> { HudSettings.Default.copy(debugScreenScale = 0.24f) }
        assertFailsWith<IllegalArgumentException> { HudSettings.Default.copy(debugScreenScale = 1.01f) }
        assertFailsWith<IllegalArgumentException> { HudSettings.Default.copy(debugScreenScale = Float.NaN) }
    }
}
