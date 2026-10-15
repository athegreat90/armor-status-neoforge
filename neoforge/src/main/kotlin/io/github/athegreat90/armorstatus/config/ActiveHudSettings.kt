package io.github.athegreat90.armorstatus.config

import com.mojang.logging.LogUtils
import io.github.athegreat90.armorstatus.error.loadOrDefault
import io.github.athegreat90.armorstatus.tooltip.ResolvedItemFilter

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

    /**
     * The tooltip's include and exclude lists as registry keys, replaced whole with [current] on every refresh. It is
     * part of this same snapshot: [HudSettings] lives in `common` and cannot hold Minecraft types, so the resolved
     * form sits beside it. Readers may briefly see the new settings with the old lists; the next frame is consistent.
     */
    @Volatile
    var weaponFilters: WeaponFilters = WeaponFilters.Empty
        private set

    /** Re-reads the config file into a new snapshot; an invalid config falls back to the defaults. */
    fun refresh() {
        current =
            loadOrDefault(
                default = { HudSettings.Default },
                onFailure = { logger.warn("Invalid Armor Status configuration, using defaults", it) },
            ) { ArmorStatusConfig.read() }
        weaponFilters = WeaponFilters.of(current.weaponTooltip)
        logger.info("Armor Status settings: {}", current)
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
