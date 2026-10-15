package io.github.athegreat90.armorstatus.config

import io.github.athegreat90.armorstatus.display.ColorScheme
import io.github.athegreat90.armorstatus.weapon.WeaponPolicy
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WeaponTooltipSettingsTest {
    private fun sanitized(
        colorSchemeName: String? = "TIERS",
        warningThresholdPercent: Double = 15.0,
    ) = WeaponTooltipSettings.sanitized(
        enabled = true,
        showMax = true,
        showPercent = true,
        showUndamaged = true,
        colorSchemeName = colorSchemeName,
        lowDurabilityWarning = true,
        warningThresholdPercent = warningThresholdPercent,
        includeAxes = true,
    )

    @Test
    fun `defaults show the full line in tier colours with axes included`() {
        val defaults = WeaponTooltipSettings.Default
        assertTrue(defaults.enabled)
        assertTrue(defaults.showMax)
        assertTrue(defaults.showPercent)
        assertTrue(defaults.showUndamaged)
        assertEquals(ColorScheme.TIERS, defaults.colorScheme)
        assertTrue(defaults.lowDurabilityWarning)
        assertEquals(15, defaults.warningThresholdPercent)
        assertTrue(defaults.includeAxes)
        assertEquals(WeaponTooltipSettings(), defaults)
    }

    @Test
    fun `threshold bounds are 1 to 99`() {
        assertEquals(1, MIN_WARNING_THRESHOLD)
        assertEquals(99, MAX_WARNING_THRESHOLD)
    }

    @ParameterizedTest(name = "threshold {0} is accepted")
    @ValueSource(ints = [1, 15, 50, 99])
    fun `the constructor accepts thresholds in range`(threshold: Int) {
        assertEquals(threshold, WeaponTooltipSettings(warningThresholdPercent = threshold).warningThresholdPercent)
    }

    @ParameterizedTest(name = "threshold {0} is rejected")
    @ValueSource(ints = [Int.MIN_VALUE, -1, 0, 100, 101, Int.MAX_VALUE])
    fun `the constructor rejects thresholds out of range`(threshold: Int) {
        assertFailsWith<IllegalArgumentException> { WeaponTooltipSettings(warningThresholdPercent = threshold) }
    }

    @ParameterizedTest(name = "threshold {0} -> {1}")
    @CsvSource("15, 15", "1, 1", "99, 99", "0, 1", "-40, 1", "100, 99", "1000, 99", "15.4, 15", "15.6, 16")
    fun `sanitized coerces the threshold into the allowed range`(
        threshold: Double,
        expected: Int,
    ) {
        assertEquals(expected, sanitized(warningThresholdPercent = threshold).warningThresholdPercent)
    }

    @Test
    fun `sanitized replaces a NaN threshold with the default and clamps infinities`() {
        assertEquals(15, sanitized(warningThresholdPercent = Double.NaN).warningThresholdPercent)
        assertEquals(
            MAX_WARNING_THRESHOLD,
            sanitized(warningThresholdPercent = Double.POSITIVE_INFINITY).warningThresholdPercent,
        )
        assertEquals(
            MIN_WARNING_THRESHOLD,
            sanitized(warningThresholdPercent = Double.NEGATIVE_INFINITY).warningThresholdPercent,
        )
    }

    @ParameterizedTest(name = "scheme \"{0}\" -> {1}")
    @CsvSource(
        "WHITE, WHITE",
        "white, WHITE",
        "Gradient, GRADIENT",
        "' tiers ', TIERS",
        "'  GRADIENT', GRADIENT",
        "tIeRs, TIERS",
    )
    fun `sanitized parses colour scheme names ignoring case and surrounding whitespace`(
        name: String,
        expected: ColorScheme,
    ) {
        assertEquals(expected, sanitized(colorSchemeName = name).colorScheme)
    }

    @ParameterizedTest(name = "unknown scheme \"{0}\" -> TIERS")
    @ValueSource(strings = ["", "  ", "rainbow", "tier", "true", "WHITE GRADIENT"])
    fun `sanitized falls back to tiers for unknown colour schemes`(name: String) {
        assertEquals(ColorScheme.TIERS, sanitized(colorSchemeName = name).colorScheme)
    }

    @Test
    fun `sanitized falls back to tiers for a null colour scheme`() {
        assertEquals(ColorScheme.TIERS, sanitized(colorSchemeName = null).colorScheme)
    }

    @Test
    fun `sanitized keeps every other value as given`() {
        val result =
            WeaponTooltipSettings.sanitized(
                enabled = false,
                showMax = false,
                showPercent = false,
                showUndamaged = false,
                colorSchemeName = "gradient",
                lowDurabilityWarning = false,
                warningThresholdPercent = 30.0,
                includeAxes = false,
            )
        assertEquals(
            WeaponTooltipSettings(
                enabled = false,
                showMax = false,
                showPercent = false,
                showUndamaged = false,
                colorScheme = ColorScheme.GRADIENT,
                lowDurabilityWarning = false,
                warningThresholdPercent = 30,
                includeAxes = false,
            ),
            result,
        )
    }

    @Test
    fun `sanitized with no arguments yields the defaults`() {
        assertEquals(WeaponTooltipSettings.Default, WeaponTooltipSettings.sanitized())
    }

    @Test
    fun `the item lists are empty by default`() {
        assertTrue(WeaponTooltipSettings.Default.includeItems.isEmpty)
        assertTrue(WeaponTooltipSettings.Default.excludeItems.isEmpty)
    }

    @Test
    fun `sanitized parses and normalises the item lists and drops bad entries`() {
        val result =
            WeaponTooltipSettings.sanitized(
                includeItems = listOf("Examplemod:Ruby_Sword", "#c:tools/melee_weapon", "bad entry"),
                excludeItems = listOf("iron_sword", "#examplemod:toys"),
            )
        assertEquals(listOf("examplemod:ruby_sword"), result.includeItems.ids)
        assertEquals(listOf("c:tools/melee_weapon"), result.includeItems.tags)
        assertEquals(listOf("minecraft:iron_sword"), result.excludeItems.ids)
        assertEquals(listOf("examplemod:toys"), result.excludeItems.tags)
    }

    @ParameterizedTest(name = "includeAxes {0} -> policy includeAxes {0}")
    @ValueSource(booleans = [true, false])
    fun `policy carries includeAxes`(includeAxes: Boolean) {
        assertEquals(WeaponPolicy(includeAxes = includeAxes), WeaponTooltipSettings(includeAxes = includeAxes).policy())
    }

    @Test
    fun `the default policy includes axes`() {
        assertTrue(WeaponTooltipSettings.Default.policy().includeAxes)
        assertFalse(WeaponTooltipSettings(includeAxes = false).policy().includeAxes)
    }
}
