package io.github.athegreat90.armorstatus.display

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DurabilityTextCacheTest {
    private val raw = DurabilityDisplayMode.RAW
    private val percent = DurabilityDisplayMode.PERCENT

    @Test
    fun `an item without durability reports not damageable`() {
        assertFalse(DurabilityTextCache().update(maxDamage = 0, damage = 0, mode = raw, gradient = false))
    }

    @Test
    fun `raw mode shows the remaining durability in white`() {
        val cache = DurabilityTextCache()

        assertTrue(cache.update(maxDamage = 363, damage = 100, mode = raw, gradient = false))

        assertEquals("263", cache.text)
        assertEquals(WHITE, cache.color)
    }

    @Test
    fun `percent mode shows the rounded percentage`() {
        val cache = DurabilityTextCache()

        cache.update(maxDamage = 200, damage = 130, mode = percent, gradient = false)

        assertEquals("35%", cache.text)
    }

    @Test
    fun `gradient colours the text by the remaining fraction`() {
        val cache = DurabilityTextCache()

        cache.update(maxDamage = 100, damage = 75, mode = raw, gradient = true)

        assertEquals(durabilityColor(0.25), cache.color)
    }

    @Test
    fun `damage beyond the maximum clamps to zero remaining`() {
        val cache = DurabilityTextCache()

        assertTrue(cache.update(maxDamage = 50, damage = 80, mode = raw, gradient = false))

        assertEquals("0", cache.text)
    }

    @Test
    fun `an unchanged reading keeps the very same string`() {
        val cache = DurabilityTextCache()
        cache.update(363, 100, raw, false)
        val before = cache.text

        cache.update(363, 100, raw, false)

        assertSame(before, cache.text)
    }

    @Test
    fun `a damage change rebuilds the text`() {
        val cache = DurabilityTextCache()
        cache.update(363, 100, raw, false)
        val before = cache.text

        cache.update(363, 101, raw, false)

        assertNotSame(before, cache.text)
        assertEquals("262", cache.text)
    }

    @Test
    fun `a max durability change rebuilds the text`() {
        val cache = DurabilityTextCache()
        cache.update(363, 100, percent, false)

        cache.update(500, 100, percent, false)

        assertEquals("80%", cache.text)
    }

    @Test
    fun `a mode change rebuilds the text`() {
        val cache = DurabilityTextCache()
        cache.update(200, 130, raw, false)

        cache.update(200, 130, percent, false)

        assertEquals("35%", cache.text)
    }

    @Test
    fun `toggling the gradient changes the colour only`() {
        val cache = DurabilityTextCache()
        cache.update(100, 75, raw, false)
        assertEquals(WHITE, cache.color)

        cache.update(100, 75, raw, true)

        assertEquals(durabilityColor(0.25), cache.color)
        assertEquals("25", cache.text)
    }

    @Test
    fun `becoming non damageable and back recomputes the reading`() {
        val cache = DurabilityTextCache()
        cache.update(363, 100, raw, false)

        assertFalse(cache.update(0, 0, raw, false))
        assertTrue(cache.update(363, 100, raw, false))

        assertEquals("263", cache.text)
    }
}
