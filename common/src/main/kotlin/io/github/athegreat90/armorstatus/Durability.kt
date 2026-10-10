package io.github.athegreat90.armorstatus

/** Placeholder for the pure domain logic added in a later task. */
fun remainingPercent(damage: Int, maxDamage: Int): Int =
    if (maxDamage <= 0) 0 else ((maxDamage - damage).coerceIn(0, maxDamage) * 100) / maxDamage
