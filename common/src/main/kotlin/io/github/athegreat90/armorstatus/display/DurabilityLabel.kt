package io.github.athegreat90.armorstatus.display

import io.github.athegreat90.armorstatus.model.DurabilityReading

/**
 * The text shown for this reading.
 *
 * - [DurabilityDisplayMode.RAW]: the remaining durability, for example `123`.
 * - [DurabilityDisplayMode.PERCENT]: `remaining / max` as a whole percent rounded half up
 *   (`1/8` is `13%`). The extremes are kept honest: `0%` only when the item has nothing left,
 *   `100%` only when it is undamaged, so a nearly-broken item never reads `0%` and a scratched one
 *   never reads `100%`.
 */
fun DurabilityReading.Damageable.label(mode: DurabilityDisplayMode): String =
    when (mode) {
        DurabilityDisplayMode.RAW -> remaining.toString()
        DurabilityDisplayMode.PERCENT -> "${percentRounded()}%"
    }

/**
 * The whole percent shown to the player for this reading, in `0..100`: `remaining / max` rounded half up, with
 * honest extremes (`0` only when nothing is left, `100` only when undamaged, otherwise clamped into `1..99`).
 *
 * This is the single source of the displayed percent: [label], [isLow] and the tooltip colour tiers all derive from
 * it, so the text and the colour can never disagree. Uses `Long` arithmetic, so it is safe for any [max].
 */
fun DurabilityReading.Damageable.percentRounded(): Int =
    when (remaining) {
        0 -> 0

        max -> 100

        // Long arithmetic: 200 * remaining overflows Int for large durabilities.
        else -> ((200L * remaining + max) / (2L * max)).toInt().coerceIn(1, 99)
    }

/**
 * Whether this item is at or below [thresholdPercent] percent, judged on the percent the player reads
 * ([percentRounded]) rather than on the exact fraction. Example: 1 point left of 1000 displays `1%`, so it is low at
 * a threshold of 1 even though the exact value is `0.1%`; an item displaying `15%` is low at 15 and one displaying
 * `16%` is not. Deciding on the displayed value keeps the warning colour consistent with the text beside it. An item
 * with nothing left (`0%`) is low for every threshold of at least 0.
 */
fun DurabilityReading.Damageable.isLow(thresholdPercent: Int): Boolean = percentRounded() <= thresholdPercent
