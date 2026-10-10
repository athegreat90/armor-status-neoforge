package io.github.athegreat90.armorstatus.display

import kotlin.math.roundToInt

/** Opaque white (`0xFFFFFFFF`): the text colour when the gradient is off, as upstream. */
const val WHITE: Int = 0xFFFFFFFF.toInt()

private const val OPAQUE: Int = 0xFF shl 24

/**
 * Gradient stops from full durability (index 0) to broken (last index), evenly spaced at
 * fractions 1.0, 0.8, 0.6, 0.4, 0.2 and 0.0: green, light green, yellow, red, dark red, purple.
 */
private val GRADIENT_STOPS: IntArray = intArrayOf(0x55FF55, 0xAAFF55, 0xFFFF55, 0xFF5555, 0xAA0000, 0xAA00AA)

/**
 * Opaque ARGB colour for a remaining-durability [fraction], linearly interpolated between the
 * gradient stops. The fraction is clamped into `0.0..1.0`; `NaN` is treated as full durability so
 * bad input never produces an alarming colour.
 */
fun durabilityColor(fraction: Double): Int {
    val clamped = if (fraction.isNaN()) 1.0 else fraction.coerceIn(0.0, 1.0)
    val position = (1.0 - clamped) * (GRADIENT_STOPS.size - 1)
    val lowerIndex = position.toInt().coerceAtMost(GRADIENT_STOPS.size - 2)
    val t = position - lowerIndex
    val from = GRADIENT_STOPS[lowerIndex]
    val to = GRADIENT_STOPS[lowerIndex + 1]
    return OPAQUE or
        (interpolate(from, to, t, shift = 16) shl 16) or
        (interpolate(from, to, t, shift = 8) shl 8) or
        interpolate(from, to, t, shift = 0)
}

private fun interpolate(from: Int, to: Int, t: Double, shift: Int): Int {
    val start = (from shr shift) and 0xFF
    val end = (to shr shift) and 0xFF
    return (start + (end - start) * t).roundToInt()
}
