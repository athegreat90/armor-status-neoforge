package io.github.athegreat90.armorstatus.config

import io.github.athegreat90.armorstatus.display.DurabilityDisplayMode
import io.github.athegreat90.armorstatus.layout.HudAnchor
import io.github.athegreat90.armorstatus.model.ArmorSlot

/** Smallest HUD scale the mod applies on top of Minecraft's GUI scale. */
const val MIN_SCALE: Float = 0.5f

/** Largest HUD scale the mod applies on top of Minecraft's GUI scale. */
const val MAX_SCALE: Float = 2.0f

/**
 * Immutable user settings for the armor HUD. Always valid: [scale] is within [MIN_SCALE]..[MAX_SCALE].
 * Build one from untrusted input (a config file) with [sanitized].
 *
 * Defaults reproduce the upstream mod's look: enabled, middle left, scale 1.0, every slot shown,
 * raw durability numbers in white.
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
) {
    init {
        require(scale in MIN_SCALE..MAX_SCALE) { "scale must be in $MIN_SCALE..$MAX_SCALE, was $scale" }
    }

    companion object {
        val Default: HudSettings = HudSettings()

        /**
         * Lenient factory for raw config values. Never throws: [scale] is coerced into
         * [MIN_SCALE]..[MAX_SCALE] (`NaN` becomes the default 1.0), and an unknown or missing enum name falls
         * back to its default. Names are matched ignoring case and surrounding whitespace.
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
        ): HudSettings =
            HudSettings(
                enabled = enabled,
                anchor = parseEnum(anchorName, Default.anchor),
                scale = if (scale.isNaN()) Default.scale else scale.coerceIn(MIN_SCALE.toDouble(), MAX_SCALE.toDouble()).toFloat(),
                showHead = showHead,
                showChest = showChest,
                showLegs = showLegs,
                showFeet = showFeet,
                displayMode = parseEnum(displayModeName, Default.displayMode),
                gradientColors = gradientColors,
            )

        private inline fun <reified E : Enum<E>> parseEnum(name: String?, default: E): E {
            val wanted = name?.trim()
            return enumValues<E>().firstOrNull { it.name.equals(wanted, ignoreCase = true) } ?: default
        }
    }
}

/** Whether the user wants [slot] shown. */
fun HudSettings.shows(slot: ArmorSlot): Boolean =
    when (slot) {
        ArmorSlot.HEAD -> showHead
        ArmorSlot.CHEST -> showChest
        ArmorSlot.LEGS -> showLegs
        ArmorSlot.FEET -> showFeet
    }
