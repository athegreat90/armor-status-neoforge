package io.github.athegreat90.armorstatus.layout

/**
 * Where the armor rows go, in the mod's own logical coordinate space (the scaled GUI size divided by
 * the mod's `scale`). The adapter applies `scale` to the pose stack, then draws with these numbers.
 *
 * @property rowHeight height of one row.
 * @property totalHeight height of all rows together.
 * @property originY top edge of the first row.
 * @property iconX left edge of the 16 px icon.
 * @property textX left edge of the text for left anchors; for right anchors the right edge the text
 *   must end at (see [textRightAligned]), so the adapter subtracts the text width itself.
 * @property textOffsetY vertical offset of the text from the top of its row.
 */
data class HudLayout(
    val anchor: HudAnchor,
    val rowCount: Int,
    val rowHeight: Int,
    val totalHeight: Int,
    val originY: Int,
    val iconX: Int,
    val textX: Int,
    val textOffsetY: Int,
) {
    /** True when [textX] is the text's right edge. */
    val textRightAligned: Boolean get() = !anchor.isLeft

    /** Top edge of the row at [index], which must be in `0 until rowCount`. */
    fun rowY(index: Int): Int {
        require(index in 0 until rowCount) { "row index must be in 0 until $rowCount, was $index" }
        return originY + index * rowHeight
    }

    companion object {
        const val ICON_SIZE = 16
        const val MARGIN = 4
        private const val ICON_TEXT_GAP = 2
        private const val ROW_PADDING = 2

        /**
         * Computes the layout from Minecraft's scaled GUI size, the mod's [scale], the font height, the
         * [anchor] and how many rows will be drawn (0 is valid). All maths is integer and truncating.
         */
        fun compute(
            guiWidth: Int,
            guiHeight: Int,
            scale: Float,
            fontHeight: Int,
            anchor: HudAnchor,
            rowCount: Int,
        ): HudLayout {
            require(guiWidth >= 0) { "guiWidth must not be negative, was $guiWidth" }
            require(guiHeight >= 0) { "guiHeight must not be negative, was $guiHeight" }
            require(scale > 0f && scale.isFinite()) { "scale must be positive and finite, was $scale" }
            require(fontHeight >= 0) { "fontHeight must not be negative, was $fontHeight" }
            require(rowCount >= 0) { "rowCount must not be negative, was $rowCount" }

            val width = (guiWidth / scale).toInt()
            val height = (guiHeight / scale).toInt()
            val rowHeight = maxOf(ICON_SIZE, fontHeight) + ROW_PADDING
            val totalHeight = rowCount * rowHeight
            val iconX = if (anchor.isLeft) MARGIN else width - MARGIN - ICON_SIZE

            return HudLayout(
                anchor = anchor,
                rowCount = rowCount,
                rowHeight = rowHeight,
                totalHeight = totalHeight,
                originY = when (anchor.vertical) {
                    VerticalAlign.TOP -> MARGIN
                    VerticalAlign.MIDDLE -> (height - totalHeight) / 2
                    VerticalAlign.BOTTOM -> height - totalHeight - MARGIN
                },
                iconX = iconX,
                textX = if (anchor.isLeft) iconX + ICON_SIZE + ICON_TEXT_GAP else iconX - ICON_TEXT_GAP,
                textOffsetY = (ICON_SIZE - fontHeight) / 2 + 1,
            )
        }
    }
}
