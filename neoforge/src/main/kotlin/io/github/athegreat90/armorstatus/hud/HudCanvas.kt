package io.github.athegreat90.armorstatus.hud

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.item.ItemStack

/**
 * The only place where the HUD touches Minecraft's drawing API, so a rename in a future Minecraft version is a
 * one-file fix. A value class: wrapping the per-frame [GuiGraphicsExtractor] costs no allocation.
 *
 * Coordinates are in the scaled GUI space. Between [pushScale] and [pop] they are additionally multiplied by the
 * mod's own scale, which is why `HudLayout` works in `guiSize / scale` units.
 *
 * Whether the HUD is hidden (F1) is not here because that check differs between Minecraft versions; it lives in
 * the per-version source sets as `Minecraft.isHudHidden()`.
 */
@JvmInline
internal value class HudCanvas(private val graphics: GuiGraphicsExtractor) {
    /** Width of the scaled GUI in pixels. */
    val guiWidth: Int get() = graphics.guiWidth()

    /** Height of the scaled GUI in pixels. */
    val guiHeight: Int get() = graphics.guiHeight()

    /** Starts drawing at [factor] times the GUI scale; every call must be paired with [pop]. */
    fun pushScale(factor: Float) {
        val pose = graphics.pose()
        pose.pushMatrix()
        pose.scale(factor, factor)
    }

    /** Ends the scope opened by [pushScale]. */
    fun pop() {
        graphics.pose().popMatrix()
    }

    /** Draws [stack] as a 16 x 16 GUI icon with its top-left corner at ([x], [y]), without count or damage bar. */
    fun drawItem(stack: ItemStack, x: Int, y: Int) = graphics.item(stack, x, y)

    /** Draws [text] with a drop shadow; [argb] is an opaque colour such as `0xFFFFFFFF`. */
    fun drawText(text: String, x: Int, y: Int, argb: Int) = graphics.text(Minecraft.getInstance().font, text, x, y, argb, true)

    /** Width of [text] in GUI pixels. */
    fun textWidth(text: String): Int = Minecraft.getInstance().font.width(text)

    companion object {
        /** Line height of the default font. */
        val fontHeight: Int get() = Minecraft.getInstance().font.lineHeight
    }
}
