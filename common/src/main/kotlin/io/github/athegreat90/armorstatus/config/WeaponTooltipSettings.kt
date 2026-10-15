package io.github.athegreat90.armorstatus.config

import io.github.athegreat90.armorstatus.display.ColorScheme
import io.github.athegreat90.armorstatus.weapon.ItemFilter
import io.github.athegreat90.armorstatus.weapon.WeaponPolicy
import io.github.athegreat90.armorstatus.weapon.parseItemFilter
import kotlin.math.roundToInt

/** Smallest low-durability warning threshold, in displayed percent. */
const val MIN_WARNING_THRESHOLD: Int = 1

/** Largest low-durability warning threshold, in displayed percent. */
const val MAX_WARNING_THRESHOLD: Int = 99

private const val DEFAULT_WARNING_THRESHOLD: Int = 15

/**
 * Immutable user settings for the weapon durability line in the item tooltip. Always valid:
 * [warningThresholdPercent] is within [MIN_WARNING_THRESHOLD]..[MAX_WARNING_THRESHOLD]. Build one from untrusted
 * input (a config file) with [sanitized]. Independent of the armor HUD settings; [HudSettings.weaponTooltip] carries
 * it alongside them.
 *
 * The defaults show the whole line (`1480 / 1561 (95%)`) in the [ColorScheme.TIERS] colours for every weapon,
 * axes included, with red at or below 15 percent.
 *
 * @property enabled whether the tooltip line is added at all.
 * @property showMax append ` / <max>` after the remaining durability.
 * @property showPercent append ` (<percent>%)`, with the same rounding as the armor percent.
 * @property showUndamaged when false, a weapon at full durability gets no line.
 * @property colorScheme how the text is coloured.
 * @property lowDurabilityWarning when false, the red tier of [ColorScheme.TIERS] is shown as orange.
 * @property warningThresholdPercent displayed percent at or below which a weapon is considered low.
 * @property includeAxes whether axes count as weapons; see [WeaponPolicy].
 * @property includeItems items (ids and tags) that always count as weapons, for modded weapons that carry none of the
 *   standard weapon tags. They still need durability to get a line.
 * @property excludeItems items (ids and tags) that never get the line; this wins over [includeItems].
 */
data class WeaponTooltipSettings(
    val enabled: Boolean = true,
    val showMax: Boolean = true,
    val showPercent: Boolean = true,
    val showUndamaged: Boolean = true,
    val colorScheme: ColorScheme = ColorScheme.TIERS,
    val lowDurabilityWarning: Boolean = true,
    val warningThresholdPercent: Int = DEFAULT_WARNING_THRESHOLD,
    val includeAxes: Boolean = true,
    val includeItems: ItemFilter = ItemFilter.Empty,
    val excludeItems: ItemFilter = ItemFilter.Empty,
) {
    init {
        require(warningThresholdPercent in MIN_WARNING_THRESHOLD..MAX_WARNING_THRESHOLD) {
            "warningThresholdPercent must be in $MIN_WARNING_THRESHOLD..$MAX_WARNING_THRESHOLD, " +
                "was $warningThresholdPercent"
        }
    }

    companion object {
        val Default: WeaponTooltipSettings = WeaponTooltipSettings()

        /**
         * Lenient factory for raw config values. Never throws: [warningThresholdPercent] is rounded and coerced into
         * [MIN_WARNING_THRESHOLD]..[MAX_WARNING_THRESHOLD] (`NaN` becomes the default), and an unknown, blank or
         * missing [colorSchemeName] falls back to [ColorScheme.TIERS]. Names are matched ignoring case and
         * surrounding whitespace, like [HudSettings.sanitized].
         */
        fun sanitized(
            enabled: Boolean = Default.enabled,
            showMax: Boolean = Default.showMax,
            showPercent: Boolean = Default.showPercent,
            showUndamaged: Boolean = Default.showUndamaged,
            colorSchemeName: String? = null,
            lowDurabilityWarning: Boolean = Default.lowDurabilityWarning,
            warningThresholdPercent: Double = Default.warningThresholdPercent.toDouble(),
            includeAxes: Boolean = Default.includeAxes,
            includeItems: List<String> = emptyList(),
            excludeItems: List<String> = emptyList(),
        ): WeaponTooltipSettings =
            WeaponTooltipSettings(
                enabled = enabled,
                showMax = showMax,
                showPercent = showPercent,
                showUndamaged = showUndamaged,
                colorScheme = parseColorScheme(colorSchemeName),
                lowDurabilityWarning = lowDurabilityWarning,
                warningThresholdPercent = coerceThreshold(warningThresholdPercent),
                includeAxes = includeAxes,
                includeItems = parseItemFilter(includeItems),
                excludeItems = parseItemFilter(excludeItems),
            )

        private fun coerceThreshold(value: Double): Int =
            if (value.isNaN()) {
                Default.warningThresholdPercent
            } else {
                value.coerceIn(MIN_WARNING_THRESHOLD.toDouble(), MAX_WARNING_THRESHOLD.toDouble()).roundToInt()
            }

        private fun parseColorScheme(name: String?): ColorScheme {
            val wanted = name?.trim()
            return ColorScheme.entries.firstOrNull { it.name.equals(wanted, ignoreCase = true) } ?: Default.colorScheme
        }
    }
}

/** The classification choices contained in these settings. */
fun WeaponTooltipSettings.policy(): WeaponPolicy = WeaponPolicy(includeAxes = includeAxes)
