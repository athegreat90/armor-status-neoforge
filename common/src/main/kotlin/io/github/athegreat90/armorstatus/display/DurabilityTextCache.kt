package io.github.athegreat90.armorstatus.display

import io.github.athegreat90.armorstatus.model.DurabilityReading
import io.github.athegreat90.armorstatus.model.readDurability

/**
 * The text and colour for one armor row, rebuilt only when the game's `(maxDamage, damage)` pair, the display
 * mode or the gradient flag changes. Between changes [update] allocates nothing and [text] keeps returning the
 * very same `String`, which is what a per-frame HUD needs.
 *
 * Deliberately mutable and single-threaded (one instance per HUD row, used by the render thread only): the
 * per-layer cache that the performance rules in the plan allow as a justified exception.
 */
class DurabilityTextCache {
    private var primed = false
    private var maxDamage = 0
    private var damage = 0
    private var mode = DurabilityDisplayMode.DEFAULT
    private var gradient = false
    private var damageable = false

    /** The label to draw; meaningful only after [update] returned `true`. */
    var text: String = ""
        private set

    /** Opaque ARGB colour of [text]; meaningful only after [update] returned `true`. */
    var color: Int = WHITE
        private set

    /**
     * Refreshes [text] and [color] for the given raw values and returns whether the item can be damaged.
     * A `false` result means the row must be skipped.
     */
    fun update(
        maxDamage: Int,
        damage: Int,
        mode: DurabilityDisplayMode,
        gradient: Boolean,
    ): Boolean {
        val unchanged =
            primed &&
                maxDamage == this.maxDamage &&
                damage == this.damage &&
                mode == this.mode &&
                gradient == this.gradient
        if (!unchanged) rebuild(maxDamage, damage, mode, gradient)
        return damageable
    }

    private fun rebuild(
        maxDamage: Int,
        damage: Int,
        mode: DurabilityDisplayMode,
        gradient: Boolean,
    ) {
        primed = true
        this.maxDamage = maxDamage
        this.damage = damage
        this.mode = mode
        this.gradient = gradient
        when (val reading = readDurability(maxDamage, damage)) {
            is DurabilityReading.Damageable -> {
                damageable = true
                text = reading.label(mode)
                color = if (gradient) durabilityColor(reading.fraction) else WHITE
            }

            DurabilityReading.NotDamageable -> {
                damageable = false
            }
        }
    }
}
