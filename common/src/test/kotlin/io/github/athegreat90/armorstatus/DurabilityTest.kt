package io.github.athegreat90.armorstatus

import kotlin.test.Test
import kotlin.test.assertEquals

class DurabilityTest {
    @Test
    fun `full durability is one hundred percent`() {
        assertEquals(100, remainingPercent(damage = 0, maxDamage = 200))
    }
}
