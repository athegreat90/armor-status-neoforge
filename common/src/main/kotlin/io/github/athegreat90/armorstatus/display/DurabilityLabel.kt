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
        DurabilityDisplayMode.PERCENT -> "${percent()}%"
    }

private fun DurabilityReading.Damageable.percent(): Int =
    when (remaining) {
        0 -> 0
        max -> 100
        // Long arithmetic: 200 * remaining overflows Int for large durabilities.
        else -> ((200L * remaining + max) / (2L * max)).toInt().coerceIn(1, 99)
    }
