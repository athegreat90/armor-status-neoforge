package io.github.athegreat90.armorstatus.config

import com.mojang.logging.LogUtils
import io.github.athegreat90.armorstatus.error.loadOrDefault
import io.github.athegreat90.armorstatus.tooltip.ResolvedItemFilter

/**
 * Holder of the immutable settings snapshot used by the HUD and the weapon tooltip.
 *
 * This is the one deliberate piece of shared mutable state: a single `@Volatile` reference to an immutable
 * [Snapshot], replaced as a whole when NeoForge loads or reloads the config file (a config thread), and read by the
 * render thread. Readers never see a half-updated value, and the render path never touches `ModConfigSpec`. Code
 * that needs the settings and the resolved filters together reads [snapshot] once.
 */
internal object ActiveHudSettings {
    private val logger = LogUtils.getLogger()

    /**
     * The settings and their resolved weapon filters, always from the same refresh. [HudSettings] lives in `common`
     * and cannot hold Minecraft types, so the registry-key form of the include and exclude lists travels beside it.
     */
    class Snapshot(
        val settings: HudSettings,
        val weaponFilters: WeaponFilters,
    )

    @Volatile
    var snapshot: Snapshot = Snapshot(HudSettings.Default, WeaponFilters.Empty)
        private set

    /** The current settings, for readers that do not need the weapon filters. */
    val current: HudSettings get() = snapshot.settings

    /** Re-reads the config file into a new snapshot; an invalid config falls back to the defaults. */
    fun refresh() {
        val settings =
            loadOrDefault(
                default = { HudSettings.Default },
                onFailure = { logger.warn("Invalid Armor Status configuration, using defaults", it) },
            ) { ArmorStatusConfig.read() }
        snapshot = Snapshot(settings, WeaponFilters.of(settings.weaponTooltip))
        logger.info("Armor Status settings: {}", settings)
    }
}

/** The resolved include and exclude lists of the weapon tooltip. */
internal class WeaponFilters(
    val included: ResolvedItemFilter,
    val excluded: ResolvedItemFilter,
) {
    companion object {
        val Empty: WeaponFilters = WeaponFilters(ResolvedItemFilter.Empty, ResolvedItemFilter.Empty)

        fun of(settings: WeaponTooltipSettings): WeaponFilters =
            if (settings.includeItems.isEmpty && settings.excludeItems.isEmpty) {
                Empty
            } else {
                WeaponFilters(
                    ResolvedItemFilter.of(settings.includeItems),
                    ResolvedItemFilter.of(settings.excludeItems),
                )
            }
    }
}
