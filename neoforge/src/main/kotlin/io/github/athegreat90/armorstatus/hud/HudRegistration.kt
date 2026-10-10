package io.github.athegreat90.armorstatus.hud

import io.github.athegreat90.armorstatus.MOD_ID
import net.minecraft.resources.Identifier
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent
import net.neoforged.neoforge.client.gui.VanillaGuiLayers

/** Adds the armor HUD to the GUI layer stack. */
internal object HudRegistration {
    private val layerId: Identifier = Identifier.fromNamespaceAndPath(MOD_ID, "armor_status")

    fun register(modBus: IEventBus) {
        modBus.addListener(::onRegisterLayers)
    }

    /**
     * Placed just below the chat: above the hotbar, bars, effects and scoreboard so the icons are never hidden by
     * them, but below chat, the player list and subtitles, which must stay readable on top of an overlay.
     */
    private fun onRegisterLayers(event: RegisterGuiLayersEvent) {
        event.registerBelow(VanillaGuiLayers.CHAT, layerId, ArmorStatusLayer())
    }
}
