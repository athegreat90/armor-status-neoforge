package io.github.athegreat90.armorstatus.display

import io.github.athegreat90.armorstatus.model.DurabilityReading.Damageable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.Test
import kotlin.test.assertEquals

class DurabilityLabelTest {
    @Test
    fun `raw is the default display mode`() {
        assertEquals(DurabilityDisplayMode.RAW, DurabilityDisplayMode.DEFAULT)
    }

    @ParameterizedTest(name = "{0}/{1} raw -> {0}")
    @CsvSource("123, 363", "363, 363", "0, 363", "1, 1")
    fun `raw label is the remaining durability as an integer`(
        remaining: Int,
        max: Int,
    ) {
        assertEquals(remaining.toString(), Damageable(remaining, max).label(DurabilityDisplayMode.RAW))
    }

    @ParameterizedTest(name = "{0}/{1} percent -> {2}")
    @CsvSource(
        "75, 100, 75%",
        "363, 363, 100%",
        "0, 363, 0%",
        // exact halves round up
        "1, 8, 13%",
        "3, 8, 38%",
        // never 0% while something remains
        "1, 363, 1%",
        "1, 1000, 1%",
        // never 100% once damaged
        "362, 363, 99%",
        "999, 1000, 99%",
        // rounds to nearest otherwise
        "1, 3, 33%",
        "2, 3, 67%",
    )
    fun `percent label rounds half up and never lies at the extremes`(
        remaining: Int,
        max: Int,
        expected: String,
    ) {
        assertEquals(expected, Damageable(remaining, max).label(DurabilityDisplayMode.PERCENT))
    }

    @Test
    fun `percent label does not overflow for very large durability`() {
        assertEquals("50%", Damageable(Int.MAX_VALUE / 2, Int.MAX_VALUE).label(DurabilityDisplayMode.PERCENT))
    }
}
