package io.github.athegreat90.armorstatus.layout

/** Vertical placement of the HUD block on the screen. */
enum class VerticalAlign { TOP, MIDDLE, BOTTOM }

/** The six screen positions the armor HUD can be pinned to. */
enum class HudAnchor(
    val isLeft: Boolean,
    val vertical: VerticalAlign,
) {
    TOP_LEFT(isLeft = true, vertical = VerticalAlign.TOP),
    MIDDLE_LEFT(isLeft = true, vertical = VerticalAlign.MIDDLE),
    BOTTOM_LEFT(isLeft = true, vertical = VerticalAlign.BOTTOM),
    TOP_RIGHT(isLeft = false, vertical = VerticalAlign.TOP),
    MIDDLE_RIGHT(isLeft = false, vertical = VerticalAlign.MIDDLE),
    BOTTOM_RIGHT(isLeft = false, vertical = VerticalAlign.BOTTOM),
}
