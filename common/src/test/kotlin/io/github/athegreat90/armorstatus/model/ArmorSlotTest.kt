package io.github.athegreat90.armorstatus.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ArmorSlotTest {
    @Test
    fun `slots are displayed head, chest, legs, feet`() {
        assertEquals(listOf(ArmorSlot.HEAD, ArmorSlot.CHEST, ArmorSlot.LEGS, ArmorSlot.FEET), ArmorSlot.entries)
    }
}
