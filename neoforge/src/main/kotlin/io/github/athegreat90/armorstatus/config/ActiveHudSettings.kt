package io.github.athegreat90.armorstatus.config

import com.mojang.logging.LogUtils
import io.github.athegreat90.armorstatus.error.loadOrDefault

/**
 * Holder of the immutable [HudSettings] snapshot used by the HUD.
 *
 * This is the one deliberate piece of shared mutable state: a single `@Volatile` reference that is
 * replaced as a whole when NeoForge loads or reloads the config file (a config thread), and read by the
 * render thread. Because [HudSettings] is immutable, readers never see a half-updated value and the render
 * path never touches `ModConfigSpec`.
 */
internal object ActiveHudSettings {
    private val logger = LogUtils.getLogger()

    @Volatile
    var current: HudSettings = HudSettings.Default
        private set

    /** Re-reads the config file into a new snapshot; an invalid config falls back to the defaults. */
    fun refresh() {
        current =
            loadOrDefault(
                default = { HudSettings.Default },
                onFailure = { logger.warn("Invalid Armor Status configuration, using defaults", it) },
            ) { ArmorStatusConfig.read() }
        logger.info("Armor Status settings: {}", current)
    }
}
