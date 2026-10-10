package io.github.athegreat90.armorstatus.config

import io.github.athegreat90.armorstatus.MOD_ID
import net.minecraft.client.gui.screens.Screen
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.config.ModConfig
import net.neoforged.fml.event.config.ModConfigEvent
import net.neoforged.neoforge.client.gui.ConfigurationScreen
import net.neoforged.neoforge.client.gui.IConfigScreenFactory

/** Wiring between NeoForge's config system and the mod: file registration, refresh events, config screen. */
internal object ConfigRefresh {
    fun register(modBus: IEventBus, container: ModContainer) {
        container.registerConfig(ModConfig.Type.CLIENT, ArmorStatusConfig.spec)
        // Safe to reference client classes: the entrypoint only loads on the client dist.
        container.registerExtensionPoint(IConfigScreenFactory::class.java, IConfigScreenFactory(::configScreen))
        modBus.addListener(::onLoading)
        modBus.addListener(::onReloading)
    }

    private fun configScreen(container: ModContainer, parent: Screen): Screen = ConfigurationScreen(container, parent)

    private fun onLoading(event: ModConfigEvent.Loading) = refreshIfOurs(event)

    private fun onReloading(event: ModConfigEvent.Reloading) = refreshIfOurs(event)

    private fun refreshIfOurs(event: ModConfigEvent) {
        if (event.config.modId == MOD_ID && event.config.type == ModConfig.Type.CLIENT) ActiveHudSettings.refresh()
    }
}
