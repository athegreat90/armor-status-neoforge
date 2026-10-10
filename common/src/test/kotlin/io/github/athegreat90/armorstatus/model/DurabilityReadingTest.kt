package io.github.athegreat90.armorstatus.model

import io.github.athegreat90.armorstatus.model.DurabilityReading.Damageable
import io.github.athegreat90.armorstatus.model.DurabilityReading.NotDamageable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DurabilityReadingTest {
    @ParameterizedTest(name = "max={0}, damage={1} -> remaining={2}")
    @CsvSource(
        "363, 0, 363",
        "363, 100, 263",
        "363, 362, 1",
        "363, 363, 0",
        // damage beyond max is clamped, never negative remaining
        "363, 500, 0",
        // negative damage is clamped, never above max
        "363, -10, 363",
        "1, 0, 1",
    )
    fun `remaining is max minus clamped damage`(max: Int, damage: Int, expectedRemaining: Int) {
        assertEquals(Damageable(remaining = expectedRemaining, max = max), readDurability(max, damage))
    }

    @ParameterizedTest(name = "max={0} is not damageable")
    @ValueSource(ints = [0, -1, Int.MIN_VALUE])
    fun `non-positive max damage is not damageable`(max: Int) {
        assertEquals(NotDamageable, readDurability(maxDamage = max, damage = 5))
    }

    @ParameterizedTest(name = "{0}/{1} -> {2}")
    @CsvSource("363, 363, 1.0", "0, 363, 0.0", "25, 100, 0.25", "150, 200, 0.75")
    fun `fraction is remaining over max`(remaining: Int, max: Int, expected: Double) {
        assertEquals(expected, Damageable(remaining, max).fraction, 1e-12)
    }

    @Test
    fun `a damageable reading cannot be inconsistent`() {
        assertFailsWith<IllegalArgumentException> { Damageable(remaining = 5, max = 0) }
        assertFailsWith<IllegalArgumentException> { Damageable(remaining = -1, max = 10) }
        assertFailsWith<IllegalArgumentException> { Damageable(remaining = 11, max = 10) }
    }
}
