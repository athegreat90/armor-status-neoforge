package io.github.athegreat90.armorstatus.display

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.Test
import kotlin.test.assertEquals

class DurabilityColorTest {
    private fun argb(value: Long): Int = value.toInt()

    @Test
    fun `default text colour is opaque white`() {
        assertEquals(argb(0xFFFFFFFF), WHITE)
    }

    @ParameterizedTest(name = "fraction {0} -> {1}")
    @CsvSource(
        // the six breakpoints: green, light green, yellow, red, dark red, purple
        "1.0, 0xFF55FF55",
        "0.8, 0xFFAAFF55",
        "0.6, 0xFFFFFF55",
        "0.4, 0xFFFF5555",
        "0.2, 0xFFAA0000",
        "0.0, 0xFFAA00AA",
        // halfway between two breakpoints
        "0.5, 0xFFFFAA55",
        "0.1, 0xFFAA0055",
    )
    fun `colour follows the gradient`(fraction: Double, expected: String) {
        assertEquals(argb(java.lang.Long.decode(expected)), durabilityColor(fraction))
    }

    @ParameterizedTest(name = "out-of-range {0} -> {1}")
    @CsvSource("1.5, 0xFF55FF55", "-0.3, 0xFFAA00AA", "Infinity, 0xFF55FF55", "-Infinity, 0xFFAA00AA")
    fun `fraction is clamped`(fraction: Double, expected: String) {
        assertEquals(argb(java.lang.Long.decode(expected)), durabilityColor(fraction))
    }

    @Test
    fun `NaN is treated as full durability`() {
        assertEquals(durabilityColor(1.0), durabilityColor(Double.NaN))
    }

    @Test
    fun `every colour in the gradient is opaque`() {
        (0..1000).forEach { step ->
            val color = durabilityColor(step / 1000.0)
            assertEquals(0xFF, color ushr 24, "alpha at step $step")
        }
    }
}
