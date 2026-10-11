package io.github.athegreat90.armorstatus.display

import io.github.athegreat90.armorstatus.model.DurabilityReading.Damageable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DurabilityPercentTest {
    @ParameterizedTest(name = "{0}/{1} -> {2}")
    @CsvSource(
        "0, 363, 0",
        "0, 1, 0",
        "1, 1, 100",
        "363, 363, 100",
        // exactly half rounds up
        "1, 8, 13",
        "3, 8, 38",
        "1, 2, 50",
        "50, 100, 50",
        // never 0 while something remains, even with a huge maximum
        "1, 363, 1",
        "1, 1000000, 1",
        "1, 2147483647, 1",
        // never 100 once damaged: 99.5 would round to 100, so it is clamped to 99
        "199, 200, 99",
        "999, 1000, 99",
        "2147483646, 2147483647, 99",
        // plain rounding
        "1, 3, 33",
        "2, 3, 67",
        "1480, 1561, 95",
    )
    fun `percentRounded rounds half up and never lies at the extremes`(
        remaining: Int,
        max: Int,
        expected: Int,
    ) {
        assertEquals(expected, Damageable(remaining, max).percentRounded())
    }

    @Test
    fun `percentRounded does not overflow for the largest maximum`() {
        assertEquals(50, Damageable(Int.MAX_VALUE / 2, Int.MAX_VALUE).percentRounded())
        assertEquals(100, Damageable(Int.MAX_VALUE, Int.MAX_VALUE).percentRounded())
        assertEquals(0, Damageable(0, Int.MAX_VALUE).percentRounded())
    }

    @ParameterizedTest(name = "{0}/{1} percent label is the percentRounded value")
    @CsvSource("1, 8", "362, 363", "1, 363", "0, 10", "10, 10")
    fun `the percent label and percentRounded agree`(
        remaining: Int,
        max: Int,
    ) {
        val reading = Damageable(remaining, max)
        assertEquals("${reading.percentRounded()}%", reading.label(DurabilityDisplayMode.PERCENT))
    }

    @ParameterizedTest(name = "{0}/{1} at threshold {2} -> {3}")
    @CsvSource(
        // 15% of 100: exactly at, one above, one below
        "15, 100, 15, true",
        "16, 100, 15, false",
        "14, 100, 15, true",
        // thresholds at the ends of the allowed range
        "1, 100, 1, true",
        "2, 100, 1, false",
        "99, 100, 99, true",
        "100, 100, 99, false",
        // nothing left is always low
        "0, 100, 1, true",
        "0, 1, 1, true",
        "0, 100, 99, true",
        // decided on the displayed percent: 1 left of 1000 reads 1%, so it is low at threshold 1
        "1, 1000, 1, true",
        // 15 of 101 is 14.85 -> displays 15%, so it is low at 15
        "15, 101, 15, true",
        // 16 of 101 is 15.84 -> displays 16%, so it is not low at 15
        "16, 101, 15, false",
        // undamaged is never low for thresholds below 100
        "363, 363, 99, false",
    )
    fun `isLow compares the displayed percent with the threshold`(
        remaining: Int,
        max: Int,
        threshold: Int,
        expected: Boolean,
    ) {
        assertEquals(expected, Damageable(remaining, max).isLow(threshold))
    }

    @Test
    fun `isLow never disagrees with the percent text`() {
        val max = 77
        (0..max).forEach { remaining ->
            val reading = Damageable(remaining, max)
            (1..99).forEach { threshold ->
                assertEquals(
                    reading.percentRounded() <= threshold,
                    reading.isLow(threshold),
                    "$remaining/$max @ $threshold",
                )
            }
        }
        assertTrue(Damageable(0, max).isLow(1))
        assertFalse(Damageable(max, max).isLow(99))
    }
}
