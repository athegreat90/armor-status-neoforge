package io.github.athegreat90.armorstatus.display

import io.github.athegreat90.armorstatus.model.DurabilityReading

/** Opaque green (`0xFF55FF55`): the healthy tier of [ColorScheme.TIERS]. Same value as the gradient's first stop. */
const val TIER_GREEN: Int = 0xFF55FF55.toInt()

/** Opaque yellow (`0xFFFFFF55`): the half-worn tier of [ColorScheme.TIERS]. */
const val TIER_YELLOW: Int = 0xFFFFFF55.toInt()

/** Opaque orange (`0xFFFFAA00`): the worn tier of [ColorScheme.TIERS]; also stands in for red when the warning is off. */
const val TIER_ORANGE: Int = 0xFFFFAA00.toInt()

/** Opaque red (`0xFFFF5555`): the low-durability tier of [ColorScheme.TIERS]. */
const val TIER_RED: Int = 0xFFFF5555.toInt()

/** Highest displayed percent of the orange tier; above it (up to [YELLOW_TIER_MAX_PERCENT]) the colour is yellow. */
const val ORANGE_TIER_MAX_PERCENT: Int = 25

/** Highest displayed percent of the yellow tier; above it the colour is green. */
const val YELLOW_TIER_MAX_PERCENT: Int = 50

/** How a durability value is coloured. The names are the config values. */
enum class ColorScheme {
    /** Always opaque white ([WHITE]), like the armor HUD's default. */
    WHITE,

    /** Smooth green-to-purple gradient of the exact fraction, the same as the armor HUD's [durabilityColor]. */
    GRADIENT,

    /** Four fixed colours by displayed percent; see [color] for the table. */
    TIERS,
}

/**
 * Opaque ARGB colour for this reading under [scheme].
 *
 * [ColorScheme.TIERS] decides on the displayed percent ([percentRounded]), so the colour always matches the number
 * beside it. With the default [warningThresholdPercent] of 15:
 *
 * | Displayed percent | Colour |
 * |---|---|
 * | above 50 | green ([TIER_GREEN]) |
 * | 26 to 50 | yellow ([TIER_YELLOW]) |
 * | 16 to 25 | orange ([TIER_ORANGE]) |
 * | 0 to 15 (at or below the threshold) | red ([TIER_RED]) |
 *
 * A higher threshold widens red: from 25 up it swallows the orange tier and from 50 up the yellow tier as well.
 * When [warningEnabled] is false the at-or-below-threshold tier is orange instead of red, so red is never shown.
 * [ColorScheme.WHITE] and [ColorScheme.GRADIENT] ignore both warning arguments. The warning arguments are read from
 * validated settings (threshold `1..99`) but any value is safe here.
 */
fun DurabilityReading.Damageable.color(
    scheme: ColorScheme,
    warningEnabled: Boolean,
    warningThresholdPercent: Int,
): Int =
    when (scheme) {
        ColorScheme.WHITE -> WHITE
        ColorScheme.GRADIENT -> durabilityColor(fraction)
        ColorScheme.TIERS -> tierColor(warningEnabled, warningThresholdPercent)
    }

private fun DurabilityReading.Damageable.tierColor(
    warningEnabled: Boolean,
    warningThresholdPercent: Int,
): Int =
    when {
        isLow(warningThresholdPercent) -> if (warningEnabled) TIER_RED else TIER_ORANGE
        percentRounded() <= ORANGE_TIER_MAX_PERCENT -> TIER_ORANGE
        percentRounded() <= YELLOW_TIER_MAX_PERCENT -> TIER_YELLOW
        else -> TIER_GREEN
    }
