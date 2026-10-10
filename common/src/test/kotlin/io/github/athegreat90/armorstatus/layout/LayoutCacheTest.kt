package io.github.athegreat90.armorstatus.layout

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class LayoutCacheTest {
    private fun LayoutCache.base() =
        layout(
            guiWidth = 427,
            guiHeight = 240,
            scale = 1.0f,
            fontHeight = 9,
            anchor = HudAnchor.MIDDLE_LEFT,
            rowCount = 4,
        )

    @Test
    fun `the first call computes the same layout as HudLayout compute`() {
        val expected = HudLayout.compute(427, 240, 1.0f, 9, HudAnchor.MIDDLE_LEFT, 4)

        assertEquals(expected, LayoutCache().base())
    }

    @Test
    fun `identical inputs return the very same instance`() {
        val cache = LayoutCache()

        assertSame(cache.base(), cache.base())
    }

    @Test
    fun `a different gui width recomputes`() =
        assertRecomputed {
            layout(
                guiWidth = 640,
                guiHeight = 240,
                scale = 1.0f,
                fontHeight = 9,
                anchor = HudAnchor.MIDDLE_LEFT,
                rowCount = 4,
            )
        }

    @Test
    fun `a different gui height recomputes`() =
        assertRecomputed {
            layout(
                guiWidth = 427,
                guiHeight = 360,
                scale = 1.0f,
                fontHeight = 9,
                anchor = HudAnchor.MIDDLE_LEFT,
                rowCount = 4,
            )
        }

    @Test
    fun `a different scale recomputes`() =
        assertRecomputed {
            layout(
                guiWidth = 427,
                guiHeight = 240,
                scale = 1.5f,
                fontHeight = 9,
                anchor = HudAnchor.MIDDLE_LEFT,
                rowCount = 4,
            )
        }

    @Test
    fun `a different font height recomputes`() =
        assertRecomputed {
            layout(
                guiWidth = 427,
                guiHeight = 240,
                scale = 1.0f,
                fontHeight = 20,
                anchor = HudAnchor.MIDDLE_LEFT,
                rowCount = 4,
            )
        }

    @Test
    fun `a different row count recomputes`() =
        assertRecomputed {
            layout(
                guiWidth = 427,
                guiHeight = 240,
                scale = 1.0f,
                fontHeight = 9,
                anchor = HudAnchor.MIDDLE_LEFT,
                rowCount = 2,
            )
        }

    @ParameterizedTest(name = "anchor {0}")
    @EnumSource(HudAnchor::class)
    fun `a different anchor recomputes`(anchor: HudAnchor) {
        val cache = LayoutCache()
        val first = cache.base()

        val second = cache.layout(427, 240, 1.0f, 9, anchor, 4)

        assertEquals(HudLayout.compute(427, 240, 1.0f, 9, anchor, 4), second)
        if (anchor != HudAnchor.MIDDLE_LEFT) assertNotSame(first, second)
    }

    @Test
    fun `going back to earlier inputs yields an equal layout`() {
        val cache = LayoutCache()
        val first = cache.base()
        cache.layout(100, 100, 2.0f, 9, HudAnchor.TOP_RIGHT, 1)

        assertEquals(first, cache.base())
    }

    @Test
    fun `invalid inputs still fail and do not poison the cache`() {
        val cache = LayoutCache()
        val good = cache.base()

        assertFailsWith<IllegalArgumentException> { cache.layout(-1, 240, 1.0f, 9, HudAnchor.MIDDLE_LEFT, 4) }

        assertSame(good, cache.base())
    }

    private fun assertRecomputed(change: LayoutCache.() -> HudLayout) {
        val cache = LayoutCache()
        val first = cache.base()

        val second = cache.change()

        assertNotSame(first, second)
        assertSame(second, cache.change())
    }
}
