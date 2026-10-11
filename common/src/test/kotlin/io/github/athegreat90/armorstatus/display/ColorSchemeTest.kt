package io.github.athegreat90.armorstatus.display

import io.github.athegreat90.armorstatus.model.DurabilityReading.Damageable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ColorSchemeTest {
    /** A reading of exactly [percent] percent: 100 points max, so remaining equals the displayed percent. */
    private fun at(percent: Int) = Damageable(remaining = percent, max = 100)

    private fun tiers(
        percent: Int,
        warning: Boolean = true,
        threshold: Int = 15,
    ): Int = at(percent).color(ColorScheme.TIERS, warning, threshold)

    @Test
    fun `tier colours are the opaque vanilla palette`() {
        assertEquals(0xFF55FF55.toInt(), TIER_GREEN)
        assertEquals(0xFFFFFF55.toInt(), TIER_YELLOW)
        assertEquals(0xFFFFAA00.toInt(), TIER_ORANGE)
        assertEquals(0xFFFF5555.toInt(), TIER_RED)
    }

    @ParameterizedTest(name = "{0}% with the default threshold -> {1}")
    @CsvSource(
        "100, GREEN",
        "51, GREEN",
        "50, YELLOW",
        "26, YELLOW",
        "25, ORANGE",
        "16, ORANGE",
        "15, RED",
        "1, RED",
        "0, RED",
    )
    fun `tiers follow the default boundaries`(
        percent: Int,
        tier: String,
    ) {
        assertEquals(tierColor(tier), tiers(percent))
    }

    @ParameterizedTest(name = "{0}% with threshold {1} -> {2}")
    @CsvSource(
        // threshold 30 swallows the whole orange tier (26..30 and 16..25 are all at or below 30)
        "31, 30, YELLOW",
        "30, 30, RED",
        "25, 30, RED",
        "16, 30, RED",
        "51, 30, GREEN",
        // threshold 60 swallows the yellow tier as well; only above 60 is green
        "61, 60, GREEN",
        "60, 60, RED",
        "50, 60, RED",
        // threshold 1 leaves a wide orange tier
        "2, 1, ORANGE",
        "1, 1, RED",
        "25, 1, ORANGE",
        "26, 1, YELLOW",
        // threshold 99 turns everything but undamaged red
        "99, 99, RED",
        "100, 99, GREEN",
    )
    fun `a custom threshold moves the red boundary and hides the tiers it covers`(
        percent: Int,
        threshold: Int,
        tier: String,
    ) {
        assertEquals(tierColor(tier), tiers(percent, threshold = threshold))
    }

    @ParameterizedTest(name = "{0}%, warning off -> {1}")
    @CsvSource(
        "100, GREEN",
        "51, GREEN",
        "50, YELLOW",
        "26, YELLOW",
        "25, ORANGE",
        "15, ORANGE",
        "1, ORANGE",
        "0, ORANGE",
    )
    fun `with the warning off the red tier merges into orange`(
        percent: Int,
        tier: String,
    ) {
        assertEquals(tierColor(tier), tiers(percent, warning = false))
    }

    @Test
    fun `with the warning off a high threshold still never shows red`() {
        (0..100).forEach { percent ->
            val color = tiers(percent, warning = false, threshold = 60)
            assertTrue(color != TIER_RED, "$percent% must not be red")
        }
        assertEquals(TIER_ORANGE, tiers(60, warning = false, threshold = 60))
        assertEquals(TIER_GREEN, tiers(61, warning = false, threshold = 60))
    }

    @Test
    fun `tiers only get more urgent as durability falls`() {
        val urgency = listOf(TIER_GREEN, TIER_YELLOW, TIER_ORANGE, TIER_RED)
        listOf(1, 15, 30, 60, 99).forEach { threshold ->
            listOf(true, false).forEach { warning ->
                var previous = -1
                (100 downTo 0).forEach { percent ->
                    val rank = urgency.indexOf(tiers(percent, warning, threshold))
                    assertTrue(rank >= 0, "unknown colour at $percent%")
                    assertTrue(rank >= previous, "urgency fell at $percent% (threshold $threshold, warning $warning)")
                    previous = rank
                }
            }
        }
    }

    @Test
    fun `tiers use the displayed percent, not the exact fraction`() {
        // 1 left of 1000 is 0.1% exactly but displays 1%; 15 of 101 is 14.85% and displays 15%
        assertEquals(TIER_RED, Damageable(1, 1000).color(ColorScheme.TIERS, true, 1))
        assertEquals(TIER_RED, Damageable(15, 101).color(ColorScheme.TIERS, true, 15))
        assertEquals(TIER_ORANGE, Damageable(16, 101).color(ColorScheme.TIERS, true, 15))
        // 99.5% displays 99, so a scratched item with a huge maximum is not treated as undamaged
        assertEquals(TIER_GREEN, Damageable(Int.MAX_VALUE - 1, Int.MAX_VALUE).color(ColorScheme.TIERS, true, 15))
    }

    @ParameterizedTest(name = "WHITE ignores warning={0} threshold={1}")
    @CsvSource("true, 15", "false, 15", "true, 99", "false, 1")
    fun `white ignores everything`(
        warning: Boolean,
        threshold: Int,
    ) {
        listOf(0, 1, 15, 50, 100).forEach { percent ->
            assertEquals(WHITE, at(percent).color(ColorScheme.WHITE, warning, threshold), "$percent%")
        }
    }

    @ParameterizedTest(name = "GRADIENT of {0}/{1} equals durabilityColor of the fraction")
    @CsvSource("100, 100", "80, 100", "50, 100", "1, 363", "0, 100", "1480, 1561")
    fun `gradient reuses the armor gradient`(
        remaining: Int,
        max: Int,
    ) {
        val reading = Damageable(remaining, max)
        assertEquals(durabilityColor(reading.fraction), reading.color(ColorScheme.GRADIENT, true, 15))
        assertEquals(durabilityColor(reading.fraction), reading.color(ColorScheme.GRADIENT, false, 99))
    }

    @ParameterizedTest(name = "{0} always returns an opaque colour")
    @EnumSource(ColorScheme::class)
    fun `every scheme yields an opaque colour`(scheme: ColorScheme) {
        (0..100).forEach { percent ->
            assertEquals(0xFF, at(percent).color(scheme, true, 15) ushr 24, "$scheme at $percent%")
        }
    }

    @Test
    fun `the scheme names are the config values`() {
        assertEquals(listOf("WHITE", "GRADIENT", "TIERS"), ColorScheme.entries.map { it.name })
    }

    private fun tierColor(name: String): Int =
        when (name) {
            "GREEN" -> TIER_GREEN
            "YELLOW" -> TIER_YELLOW
            "ORANGE" -> TIER_ORANGE
            "RED" -> TIER_RED
            else -> error("unknown tier $name")
        }
}
