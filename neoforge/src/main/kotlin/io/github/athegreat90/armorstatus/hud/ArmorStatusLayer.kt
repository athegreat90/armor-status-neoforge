package io.github.athegreat90.armorstatus.hud

import com.mojang.logging.LogUtils
import io.github.athegreat90.armorstatus.config.ActiveHudSettings
import io.github.athegreat90.armorstatus.config.shows
import io.github.athegreat90.armorstatus.display.DurabilityTextCache
import io.github.athegreat90.armorstatus.error.isFatal
import io.github.athegreat90.armorstatus.layout.LayoutCache
import io.github.athegreat90.armorstatus.model.ArmorSlot
import io.github.athegreat90.armorstatus.visibility.shouldRender
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.client.gui.GuiLayer

/** `ArmorSlot` -> the game's slot. Exhaustive, so a new [ArmorSlot] cannot be forgotten. */
private fun ArmorSlot.toEquipmentSlot(): EquipmentSlot =
    when (this) {
        ArmorSlot.HEAD -> EquipmentSlot.HEAD
        ArmorSlot.CHEST -> EquipmentSlot.CHEST
        ArmorSlot.LEGS -> EquipmentSlot.LEGS
        ArmorSlot.FEET -> EquipmentSlot.FEET
    }

private val SLOTS: Array<ArmorSlot> = ArmorSlot.entries.toTypedArray()
private val EQUIPMENT_SLOTS: Array<EquipmentSlot> = Array(SLOTS.size) { SLOTS[it].toEquipmentSlot() }

/**
 * Draws the armor HUD: for each worn, damageable armor piece the item icon and its durability, packed in rows
 * (head, chest, legs, feet) at the configured anchor and scale.
 *
 * Runs every frame on the render thread, so it follows the performance rules: indexed loops over fixed arrays, no
 * collection pipelines, no lambdas or lists created per frame, and the strings, colours and layout are cached and
 * rebuilt only when their inputs change. The mutable fields below are that cache. They are owned by this single
 * instance and touched only by the render thread, which is the justified exception to "immutable by default".
 *
 * Failure policy: a recoverable exception disables the layer for the rest of the session and is logged once. The
 * game keeps running without the HUD, and nothing is logged per frame. Fatal errors (see [isFatal]) are rethrown.
 */
internal class ArmorStatusLayer : GuiLayer {
    private val layoutCache = LayoutCache()
    private val textCaches: Array<DurabilityTextCache> = Array(SLOTS.size) { DurabilityTextCache() }

    // The drawn rows of the current frame, packed from index 0. `rowStacks` is refilled every frame.
    private val rowStacks: Array<ItemStack> = Array(SLOTS.size) { ItemStack.EMPTY }
    private val rowSlots = IntArray(SLOTS.size)

    // Width of each slot's text, recomputed only when its (cached) text instance changes.
    private val measuredText: Array<String> = Array(SLOTS.size) { "" }
    private val measuredWidth = IntArray(SLOTS.size)

    private var failed = false

    override fun render(
        graphics: GuiGraphicsExtractor,
        deltaTracker: DeltaTracker,
    ) {
        if (failed) return
        try {
            draw(graphics)
        } catch (e: Throwable) {
            if (e.isFatal()) throw e
            failed = true
            LOGGER.error("Armor Status HUD failed and is now disabled until the game restarts", e)
        }
    }

    private fun draw(graphics: GuiGraphicsExtractor) {
        val settings = ActiveHudSettings.current
        val minecraft = Minecraft.getInstance()
        val player: Player? = minecraft.player
        val visible =
            shouldRender(
                settings = settings,
                hasPlayer = player != null,
                isSpectator = player != null && player.isSpectator,
                hudHidden = minecraft.isHudHidden(),
            )
        if (player == null || !visible) return

        var rows = 0
        for (i in SLOTS.indices) {
            if (!settings.shows(SLOTS[i])) continue
            val stack = player.getItemBySlot(EQUIPMENT_SLOTS[i])
            if (stack.isEmpty || !stack.isDamageableItem) continue
            if (!textCaches[i].update(
                    stack.maxDamage,
                    stack.damageValue,
                    settings.displayMode,
                    settings.gradientColors,
                )
            ) {
                continue
            }
            rowStacks[rows] = stack
            rowSlots[rows] = i
            rows++
        }
        if (rows == 0) return

        val canvas = HudCanvas(graphics)
        val layout =
            layoutCache.layout(
                canvas.guiWidth,
                canvas.guiHeight,
                settings.scale,
                HudCanvas.fontHeight,
                settings.anchor,
                rows,
            )
        canvas.pushScale(settings.scale)
        try {
            for (row in 0 until rows) {
                val slot = rowSlots[row]
                val cache = textCaches[slot]
                val text = cache.text
                val y = layout.rowY(row)
                canvas.drawItem(rowStacks[row], layout.iconX, y)
                val textX = if (layout.textRightAligned) layout.textX - widthOf(canvas, slot, text) else layout.textX
                canvas.drawText(text, textX, y + layout.textOffsetY, cache.color)
            }
        } finally {
            canvas.pop()
        }
    }

    private fun widthOf(
        canvas: HudCanvas,
        slot: Int,
        text: String,
    ): Int {
        if (measuredText[slot] !== text) {
            measuredText[slot] = text
            measuredWidth[slot] = canvas.textWidth(text)
        }
        return measuredWidth[slot]
    }

    private companion object {
        val LOGGER = LogUtils.getLogger()
    }
}
