package io.github.athegreat90.armorstatus.config

import io.github.athegreat90.armorstatus.MOD_ID
import io.github.athegreat90.armorstatus.display.ColorScheme
import io.github.athegreat90.armorstatus.display.DurabilityDisplayMode
import io.github.athegreat90.armorstatus.layout.HudAnchor
import net.neoforged.neoforge.common.ModConfigSpec

/**
 * The client configuration file (`armorstatusneoforge-client.toml`) as a [ModConfigSpec].
 *
 * Only [read] touches the spec values, and it must only run while NeoForge reports a loaded config
 * (see [ConfigRefresh]). The render path reads the immutable [ActiveHudSettings] snapshot instead. The weapon tooltip
 * options live in their own `[weaponTooltip]` section, after the armor HUD entries.
 */
internal object ArmorStatusConfig {
    private val builder = ModConfigSpec.Builder()

    private val enabled = builder.entry("enabled", "Show the armor status HUD.").define("enabled", true)

    private val position =
        builder
            .entry("position", "Screen corner or edge the HUD is pinned to.")
            .defineEnum("position", HudAnchor.MIDDLE_LEFT)

    private val scale =
        builder
            .entry("scale", "HUD size multiplier applied on top of the GUI scale.")
            .defineInRange("scale", 1.0, MIN_SCALE.toDouble(), MAX_SCALE.toDouble())

    private val showHelmet = builder.entry("showHelmet", "Show the helmet.").define("showHelmet", true)
    private val showChestplate = builder.entry("showChestplate", "Show the chestplate.").define("showChestplate", true)
    private val showLeggings = builder.entry("showLeggings", "Show the leggings.").define("showLeggings", true)
    private val showBoots = builder.entry("showBoots", "Show the boots.").define("showBoots", true)

    private val displayMode =
        builder
            .entry("displayMode", "RAW shows remaining durability points, PERCENT shows a percentage.")
            .defineEnum("displayMode", DurabilityDisplayMode.RAW)

    private val gradientColors =
        builder
            .entry("gradientColors", "Color the durability text from green (full) to red (almost broken).")
            .define("gradientColors", false)

    private val debugScreenMode =
        builder
            .entry(
                "debugScreenMode",
                "What the HUD does while the F3 debug screen is open: COMPACT draws it smaller, HIDE hides it, " +
                    "KEEP leaves it unchanged.",
            ).defineEnum("debugScreenMode", DebugScreenMode.COMPACT)

    private val debugScreenScale =
        builder
            .entry(
                "debugScreenScale",
                "Extra size factor applied to the HUD while the F3 debug screen is open in COMPACT mode.",
            ).defineInRange("debugScreenScale", 0.6, MIN_DEBUG_SCALE.toDouble(), MAX_DEBUG_SCALE.toDouble())

    private val debugScreenAnchor =
        builder
            .entry(
                "debugScreenAnchor",
                "Where the HUD is pinned while the F3 debug screen is open in COMPACT mode. " +
                    "BOTTOM_RIGHT leaves the debug text uncovered.",
            ).defineEnum("debugScreenAnchor", HudAnchor.BOTTOM_RIGHT)

    private val weaponTooltip = defineWeaponTooltip()

    val spec: ModConfigSpec = builder.build()

    /**
     * Converts the current config values into valid [HudSettings]. Throws if the config is not loaded yet;
     * callers go through `loadOrDefault`.
     */
    fun read(): HudSettings =
        HudSettings.sanitized(
            enabled = enabled.get(),
            anchorName = position.get().name,
            scale = scale.get(),
            showHead = showHelmet.get(),
            showChest = showChestplate.get(),
            showLegs = showLeggings.get(),
            showFeet = showBoots.get(),
            displayModeName = displayMode.get().name,
            gradientColors = gradientColors.get(),
            debugScreenModeName = debugScreenMode.get().name,
            debugScreenScale = debugScreenScale.get(),
            debugScreenAnchorName = debugScreenAnchor.get().name,
            weaponTooltip =
                WeaponTooltipSettings.sanitized(
                    enabled = weaponTooltip.enabled.get(),
                    showMax = weaponTooltip.showMax.get(),
                    showPercent = weaponTooltip.showPercent.get(),
                    showUndamaged = weaponTooltip.showUndamaged.get(),
                    colorSchemeName = weaponTooltip.colorScheme.get().name,
                    lowDurabilityWarning = weaponTooltip.lowDurabilityWarning.get(),
                    warningThresholdPercent = weaponTooltip.warningThresholdPercent.get().toDouble(),
                    includeAxes = weaponTooltip.includeAxes.get(),
                ),
        )

    /** Defines the `[weaponTooltip]` section; it must stay after every armor entry so their order is unchanged. */
    private fun defineWeaponTooltip(): WeaponTooltipValues {
        builder.entry("weaponTooltip", "Weapon durability line in the item hover tooltip.").push("weaponTooltip")
        val values =
            WeaponTooltipValues(
                enabled =
                    builder
                        .entry("weaponTooltipEnabled", "Add the durability line to weapon tooltips.")
                        .define("enabled", true),
                showMax =
                    builder
                        .entry("weaponTooltipShowMax", "Show the maximum durability, as in 1480 / 1561.")
                        .define("showMax", true),
                showPercent =
                    builder
                        .entry("weaponTooltipShowPercent", "Show the remaining durability as a percentage.")
                        .define("showPercent", true),
                showUndamaged =
                    builder
                        .entry("weaponTooltipShowUndamaged", "Also show the line for weapons that are not damaged.")
                        .define("showUndamaged", true),
                colorScheme =
                    builder
                        .entry(
                            "weaponTooltipColorScheme",
                            "WHITE is plain, GRADIENT fades from green to red, TIERS uses green, yellow, orange and red.",
                        ).defineEnum("colorScheme", ColorScheme.TIERS),
                lowDurabilityWarning =
                    builder
                        .entry(
                            "weaponTooltipLowDurabilityWarning",
                            "Show the red tier in TIERS when durability is low. When off, it is shown as orange.",
                        ).define("lowDurabilityWarning", true),
                warningThresholdPercent =
                    builder
                        .entry(
                            "weaponTooltipWarningThresholdPercent",
                            "Durability percent at or below which a weapon counts as low.",
                        ).defineInRange("warningThresholdPercent", 15, MIN_WARNING_THRESHOLD, MAX_WARNING_THRESHOLD),
                includeAxes =
                    builder
                        .entry("weaponTooltipIncludeAxes", "Count axes as weapons.")
                        .define("includeAxes", true),
            )
        builder.pop()
        return values
    }

    private class WeaponTooltipValues(
        val enabled: ModConfigSpec.BooleanValue,
        val showMax: ModConfigSpec.BooleanValue,
        val showPercent: ModConfigSpec.BooleanValue,
        val showUndamaged: ModConfigSpec.BooleanValue,
        val colorScheme: ModConfigSpec.EnumValue<ColorScheme>,
        val lowDurabilityWarning: ModConfigSpec.BooleanValue,
        val warningThresholdPercent: ModConfigSpec.IntValue,
        val includeAxes: ModConfigSpec.BooleanValue,
    )

    private fun ModConfigSpec.Builder.entry(
        key: String,
        comment: String,
    ): ModConfigSpec.Builder = comment(comment).translation("$MOD_ID.configuration.$key")
}
