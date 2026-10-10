package io.github.athegreat90.armorstatus.config

import io.github.athegreat90.armorstatus.display.DurabilityDisplayMode
import io.github.athegreat90.armorstatus.layout.HudAnchor
import io.github.athegreat90.armorstatus.model.ArmorSlot

/** Smallest HUD scale the mod applies on top of Minecraft's GUI scale. */
const val MIN_SCALE: Float = 0.5f

/** Largest HUD scale the mod applies on top of Minecraft's GUI scale. */
const val MAX_SCALE: Float = 2.0f

/** Smallest extra factor applied to the HUD scale while the F3 screen is open in compact mode. */
const val MIN_DEBUG_SCALE: Float = 0.25f

/** Largest extra factor applied to the HUD scale while the F3 screen is open in compact mode (1.0 = no change). */
const val MAX_DEBUG_SCALE: Float = 1.0f

private const val DEFAULT_DEBUG_SCALE: Float = 0.6f

/**
 * Immutable user settings for the armor HUD. Always valid: [scale] is within [MIN_SCALE]..[MAX_SCALE] and
 * [debugScreenScale] within [MIN_DEBUG_SCALE]..[MAX_DEBUG_SCALE]. Build one from untrusted input (a config file)
 * with [sanitized].
 *
 * Defaults reproduce the upstream mod's look: enabled, middle left, scale 1.0, every slot shown,
 * raw durability numbers in white. [debugScreenMode] and [debugScreenScale] keep the HUD out of the way of the
 * F3 debug text; see [effectiveScale].
 */
data class HudSettings(
    val enabled: Boolean = true,
    val anchor: HudAnchor = HudAnchor.MIDDLE_LEFT,
    val scale: Float = 1.0f,
    val showHead: Boolean = true,
    val showChest: Boolean = true,
    val showLegs: Boolean = true,
    val showFeet: Boolean = true,
    val displayMode: DurabilityDisplayMode = DurabilityDisplayMode.DEFAULT,
    val gradientColors: Boolean = false,
    val debugScreenMode: DebugScreenMode = DebugScreenMode.COMPACT,
    val debugScreenScale: Float = DEFAULT_DEBUG_SCALE,
) {
    init {
        require(scale in MIN_SCALE..MAX_SCALE) { "scale must be in $MIN_SCALE..$MAX_SCALE, was $scale" }
        require(debugScreenScale in MIN_DEBUG_SCALE..MAX_DEBUG_SCALE) {
            "debugScreenScale must be in $MIN_DEBUG_SCALE..$MAX_DEBUG_SCALE, was $debugScreenScale"
        }
    }

    companion object {
        val Default: HudSettings = HudSettings()

        /**
         * Lenient factory for raw config values. Never throws: [scale] is coerced into
         * [MIN_SCALE]..[MAX_SCALE] and [debugScreenScale] into [MIN_DEBUG_SCALE]..[MAX_DEBUG_SCALE] (`NaN` becomes
         * the default), and an unknown or missing enum name falls back to its default. Names are matched ignoring
         * case and surrounding whitespace.
         */
        fun sanitized(
            enabled: Boolean,
            anchorName: String?,
            scale: Double,
            showHead: Boolean,
            showChest: Boolean,
            showLegs: Boolean,
            showFeet: Boolean,
            displayModeName: String?,
            gradientColors: Boolean,
            debugScreenModeName: String? = null,
            debugScreenScale: Double = Default.debugScreenScale.toDouble(),
        ): HudSettings =
            HudSettings(
                enabled = enabled,
                anchor = parseEnum(anchorName, Default.anchor),
                scale = coerceScale(scale, Default.scale, MIN_SCALE, MAX_SCALE),
                showHead = showHead,
                showChest = showChest,
                showLegs = showLegs,
                showFeet = showFeet,
                displayMode = parseEnum(displayModeName, Default.displayMode),
                gradientColors = gradientColors,
                debugScreenMode = parseEnum(debugScreenModeName, Default.debugScreenMode),
                debugScreenScale =
                    coerceScale(
                        debugScreenScale,
                        Default.debugScreenScale,
                        MIN_DEBUG_SCALE,
                        MAX_DEBUG_SCALE,
                    ),
            )

        private fun coerceScale(
            value: Double,
            default: Float,
            min: Float,
            max: Float,
        ): Float = if (value.isNaN()) default else value.coerceIn(min.toDouble(), max.toDouble()).toFloat()

        private inline fun <reified E : Enum<E>> parseEnum(
            name: String?,
            default: E,
        ): E {
            val wanted = name?.trim()
            return enumValues<E>().firstOrNull { it.name.equals(wanted, ignoreCase = true) } ?: default
        }
    }
}

/**
 * The scale to draw the HUD at this frame: [HudSettings.scale], times [HudSettings.debugScreenScale] while the F3
 * screen is open in [DebugScreenMode.COMPACT]. Scaling the whole HUD also shrinks the gaps between its rows, and
 * the anchor stays put. Returns a primitive, so it is safe on the per-frame path.
 */
fun HudSettings.effectiveScale(debugScreenOpen: Boolean): Float =
    if (debugScreenOpen && debugScreenMode == DebugScreenMode.COMPACT) scale * debugScreenScale else scale

/** Whether the user wants [slot] shown. */
fun HudSettings.shows(slot: ArmorSlot): Boolean =
    when (slot) {
        ArmorSlot.HEAD -> showHead
        ArmorSlot.CHEST -> showChest
        ArmorSlot.LEGS -> showLegs
        ArmorSlot.FEET -> showFeet
    }
