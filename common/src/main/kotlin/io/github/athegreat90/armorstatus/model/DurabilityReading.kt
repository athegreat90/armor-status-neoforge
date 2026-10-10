package io.github.athegreat90.armorstatus.model

/** What the HUD knows about one armor item's durability. */
sealed interface DurabilityReading {
    /**
     * An item that can be damaged.
     *
     * @property remaining uses left, in `0..max`.
     * @property max the item's total durability, always positive.
     */
    data class Damageable(
        val remaining: Int,
        val max: Int,
    ) : DurabilityReading {
        init {
            require(max > 0) { "max must be positive, was $max" }
            require(remaining in 0..max) { "remaining must be in 0..$max, was $remaining" }
        }

        /** Remaining durability as a share of the maximum, in `0.0..1.0`. */
        val fraction: Double get() = remaining.toDouble() / max
    }

    /** An item without durability (for example a carved pumpkin or a player head). */
    data object NotDamageable : DurabilityReading
}

/**
 * Total conversion from the game's `(maxDamage, damage)` pair to a [DurabilityReading].
 *
 * A non-positive [maxDamage] means the item cannot be damaged. [damage] is clamped into `0..maxDamage`,
 * so malformed data never produces a negative or oversized remaining value.
 */
fun readDurability(
    maxDamage: Int,
    damage: Int,
): DurabilityReading =
    if (maxDamage <= 0) {
        DurabilityReading.NotDamageable
    } else {
        DurabilityReading.Damageable(remaining = maxDamage - damage.coerceIn(0, maxDamage), max = maxDamage)
    }
