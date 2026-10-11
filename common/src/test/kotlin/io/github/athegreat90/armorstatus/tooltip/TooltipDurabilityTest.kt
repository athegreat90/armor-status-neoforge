package io.github.athegreat90.armorstatus.tooltip

import io.github.athegreat90.armorstatus.config.WeaponTooltipSettings
import io.github.athegreat90.armorstatus.display.ColorScheme
import io.github.athegreat90.armorstatus.display.TIER_GREEN
import io.github.athegreat90.armorstatus.display.TIER_ORANGE
import io.github.athegreat90.armorstatus.display.TIER_RED
import io.github.athegreat90.armorstatus.display.TIER_YELLOW
import io.github.athegreat90.armorstatus.display.WHITE
import io.github.athegreat90.armorstatus.display.durabilityColor
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TooltipDurabilityTest {
    private val defaults = WeaponTooltipSettings.Default

    private fun line(
        maxDamage: Int,
        damage: Int,
        settings: WeaponTooltipSettings = defaults,
    ): TooltipDurability = assertNotNull(tooltipDurability(maxDamage, damage, settings))

    // --- valueText -------------------------------------------------------------------------------------------------

    @ParameterizedTest(name = "showMax={0}, showPercent={1} -> {2}")
    @CsvSource(
        "true, true, '1480 / 1561 (95%)'",
        "true, false, '1480 / 1561'",
        "false, true, '1480 (95%)'",
        "false, false, '1480'",
    )
    fun `valueText composes the enabled parts and always shows the remaining durability`(
        showMax: Boolean,
        showPercent: Boolean,
        expected: String,
    ) {
        val settings = defaults.copy(showMax = showMax, showPercent = showPercent)
        assertEquals(expected, line(maxDamage = 1561, damage = 81, settings = settings).valueText())
    }

    @ParameterizedTest(name = "{0}/{1} -> {2}")
    @CsvSource(
        "1561, 0, '1561 / 1561 (100%)'",
        "1561, 1, '1560 / 1561 (99%)'",
        "1561, 1560, '1 / 1561 (1%)'",
        "1561, 1561, '0 / 1561 (0%)'",
        "59, 29, '30 / 59 (51%)'",
    )
    fun `valueText with the default settings`(
        maxDamage: Int,
        damage: Int,
        expected: String,
    ) {
        assertEquals(expected, line(maxDamage, damage).valueText())
    }

    @Test
    fun `valueText is language neutral digits only plus the separators`() {
        val text = line(maxDamage = 250, damage = 10).valueText()
        assertTrue(text.all { it.isDigit() || it in " /()%" }, text)
    }

    // --- the decision ----------------------------------------------------------------------------------------------

    @Test
    fun `the fields carry the reading and the flags`() {
        val result = line(maxDamage = 1561, damage = 81, settings = defaults.copy(showMax = false))
        assertEquals(1480, result.remaining)
        assertEquals(1561, result.max)
        assertEquals(95, result.percent)
        assertFalse(result.showMax)
        assertTrue(result.showPercent)
    }

    @Test
    fun `disabled yields no line`() {
        assertNull(tooltipDurability(1561, 81, defaults.copy(enabled = false)))
        assertNull(tooltipDurability(1561, 0, defaults.copy(enabled = false)))
    }

    @Test
    fun `showUndamaged off hides full durability but shows a 1-point-damaged weapon`() {
        val settings = defaults.copy(showUndamaged = false)
        assertNull(tooltipDurability(1561, 0, settings))
        assertEquals("1560 / 1561 (99%)", line(1561, 1, settings).valueText())
    }

    @Test
    fun `showUndamaged on shows full durability`() {
        assertEquals("1561 / 1561 (100%)", line(1561, 0).valueText())
    }

    @Test
    fun `an undamaged weapon with damage below zero counts as undamaged`() {
        assertNull(tooltipDurability(100, -5, defaults.copy(showUndamaged = false)))
    }

    @ParameterizedTest(name = "maxDamage {0} -> no line")
    @ValueSource(ints = [0, -1, Int.MIN_VALUE])
    fun `an item without a positive maximum yields no line`(maxDamage: Int) {
        assertNull(tooltipDurability(maxDamage, 0, defaults))
        assertNull(tooltipDurability(maxDamage, 5, defaults))
    }

    @Test
    fun `damage above the maximum clamps to zero remaining`() {
        val result = line(maxDamage = 100, damage = 250)
        assertEquals(0, result.remaining)
        assertEquals(0, result.percent)
        assertEquals("0 / 100 (0%)", result.valueText())
    }

    @Test
    fun `the largest maximum does not overflow`() {
        val result = line(maxDamage = Int.MAX_VALUE, damage = Int.MAX_VALUE / 2)
        assertEquals(Int.MAX_VALUE - Int.MAX_VALUE / 2, result.remaining)
        assertEquals(50, result.percent)
    }

    // --- colour ----------------------------------------------------------------------------------------------------

    @ParameterizedTest(name = "{0} remaining of 100 with the tiers -> {1}")
    @CsvSource("100, GREEN", "51, GREEN", "50, YELLOW", "26, YELLOW", "25, ORANGE", "16, ORANGE", "15, RED", "0, RED")
    fun `the default scheme colours by tier`(
        remaining: Int,
        tier: String,
    ) {
        val expected =
            when (tier) {
                "GREEN" -> TIER_GREEN
                "YELLOW" -> TIER_YELLOW
                "ORANGE" -> TIER_ORANGE
                else -> TIER_RED
            }
        assertEquals(expected, line(maxDamage = 100, damage = 100 - remaining).argb)
    }

    @Test
    fun `the white scheme ignores the reading`() {
        val settings = defaults.copy(colorScheme = ColorScheme.WHITE)
        assertEquals(WHITE, line(100, 0, settings).argb)
        assertEquals(WHITE, line(100, 99, settings).argb)
        assertEquals(WHITE, line(100, 100, settings).argb)
    }

    @Test
    fun `the gradient scheme follows the exact fraction`() {
        val settings = defaults.copy(colorScheme = ColorScheme.GRADIENT)
        assertEquals(durabilityColor(0.5), line(100, 50, settings).argb)
        assertEquals(durabilityColor(1480.0 / 1561), line(1561, 81, settings).argb)
    }

    @Test
    fun `the warning flag and threshold reach the colour`() {
        assertEquals(TIER_RED, line(100, 80, defaults.copy(warningThresholdPercent = 20)).argb)
        assertEquals(TIER_ORANGE, line(100, 80, defaults.copy(warningThresholdPercent = 19)).argb)
        assertEquals(
            TIER_ORANGE,
            line(100, 80, defaults.copy(warningThresholdPercent = 20, lowDurabilityWarning = false)).argb,
        )
    }

    @Test
    fun `the colour is opaque`() {
        ColorScheme.entries.forEach { scheme ->
            val settings = defaults.copy(colorScheme = scheme)
            assertEquals(0xFF, line(100, 33, settings).argb ushr 24, "$scheme")
        }
    }

    // --- shouldAddTooltipLine --------------------------------------------------------------------------------------

    @ParameterizedTest(name = "weapon={0}, advanced={1}, player={2} -> {3}")
    @CsvSource(
        "false, false, false, false",
        "false, false, true, false",
        "false, true, false, false",
        "false, true, true, false",
        "true, false, false, false",
        "true, false, true, true",
        "true, true, false, false",
        "true, true, true, false",
    )
    fun `the line is added only for a weapon, in the normal tooltip, with a player`(
        isWeapon: Boolean,
        isAdvancedTooltip: Boolean,
        hasPlayer: Boolean,
        expected: Boolean,
    ) {
        assertEquals(expected, shouldAddTooltipLine(isWeapon, isAdvancedTooltip, hasPlayer))
    }
}
