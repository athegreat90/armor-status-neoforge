package io.github.athegreat90.armorstatus.tooltip

import com.mojang.logging.LogUtils
import io.github.athegreat90.armorstatus.config.ActiveHudSettings
import io.github.athegreat90.armorstatus.config.policy
import io.github.athegreat90.armorstatus.error.isFatal
import io.github.athegreat90.armorstatus.weapon.classifyWeapon
import net.minecraft.network.chat.Component
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent

/**
 * Adds one durability line, right under the item name, to the hover tooltip of weapons (inventory, chests, creative
 * menu and every other screen that builds a stack tooltip). Client side only; the armor HUD is not involved.
 *
 * Rules:
 * - The line is skipped in advanced mode (F3+H): vanilla prints its own `Durability: x / y` line there.
 * - It is skipped when the event has no player: that is the creative-menu search index being built for every item,
 *   and a line added then would make every weapon match searches such as "durability".
 * - What counts as a weapon is decided by `classifyWeapon` from [weaponSignals]; the line itself by
 *   `tooltipDurability`. This object only wires the event to those pure decisions.
 *
 * No state is kept besides the failure flag: nothing is cached (tag sets are rebound on datapack reload) and no
 * `ItemStack` reference outlives the event.
 *
 * Failure policy, like the HUD layer: a recoverable exception disables the listener for the rest of the session and
 * is logged once, never per event. Fatal errors (see [isFatal]) are rethrown.
 */
internal object WeaponTooltip {
    private val LOGGER = LogUtils.getLogger()

    private const val DURABILITY_KEY = "armorstatusneoforge.tooltip.durability"

    // One-shot flag. This listener can also run off the render thread (the search-index build), hence @Volatile.
    @Volatile
    private var failed = false

    fun register() {
        NeoForge.EVENT_BUS.addListener(::onItemTooltip)
    }

    private fun onItemTooltip(event: ItemTooltipEvent) {
        if (failed) return
        try {
            addLine(event)
        } catch (e: Throwable) {
            if (e.isFatal()) throw e
            failed = true
            LOGGER.error("Armor Status weapon tooltip failed and is now disabled until the game restarts", e)
        }
    }

    private fun addLine(event: ItemTooltipEvent) {
        val settings = ActiveHudSettings.current.weaponTooltip
        if (!settings.enabled) return
        // No player means the creative-menu search index is being built; cheapest exit, and it keeps our line out.
        if (event.entity == null) return

        val stack = event.itemStack
        val filters = ActiveHudSettings.weaponFilters
        val isWeapon = classifyWeapon(stack.weaponSignals(filters.included, filters.excluded), settings.policy())
        if (!shouldAddTooltipLine(isWeapon, event.flags.isAdvanced, hasPlayer = true)) return
        val line = tooltipDurability(stack.maxDamage, stack.damageValue, settings) ?: return

        val component = Component.translatable(DURABILITY_KEY, line.valueText()).withColor(line.argb)
        val lines = event.toolTip
        lines.add(if (lines.isEmpty()) 0 else 1, component)
    }
}
