package io.github.athegreat90.armorstatus.layout

/**
 * Remembers the last [HudLayout] so the render loop only recomputes it when one of the inputs of
 * [HudLayout.compute] changed. An unchanged frame costs a handful of integer comparisons and allocates nothing.
 *
 * Deliberately mutable: a renderer owns one instance and calls it from a single thread (the render thread).
 * This is the per-layer cache that the performance rules in the plan allow as a justified exception to
 * "immutable everything".
 */
class LayoutCache {
    private var cached: HudLayout? = null
    private var guiWidth = 0
    private var guiHeight = 0
    private var scale = 0f
    private var fontHeight = 0

    /**
     * The layout for these inputs, identical (same instance) to the previous result when nothing changed.
     * Invalid inputs throw exactly like [HudLayout.compute] and leave the cache untouched.
     */
    fun layout(guiWidth: Int, guiHeight: Int, scale: Float, fontHeight: Int, anchor: HudAnchor, rowCount: Int): HudLayout {
        val hit = cached
        if (hit != null &&
            guiWidth == this.guiWidth &&
            guiHeight == this.guiHeight &&
            scale == this.scale &&
            fontHeight == this.fontHeight &&
            anchor == hit.anchor &&
            rowCount == hit.rowCount
        ) {
            return hit
        }
        val computed = HudLayout.compute(guiWidth, guiHeight, scale, fontHeight, anchor, rowCount)
        cached = computed
        this.guiWidth = guiWidth
        this.guiHeight = guiHeight
        this.scale = scale
        this.fontHeight = fontHeight
        return computed
    }
}
