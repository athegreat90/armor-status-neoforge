package io.github.athegreat90.armorstatus.tooltip

import io.github.athegreat90.armorstatus.config.WeaponTooltipSettings
import io.github.athegreat90.armorstatus.display.color
import io.github.athegreat90.armorstatus.display.percentRounded
import io.github.athegreat90.armorstatus.model.DurabilityReading
import io.github.athegreat90.armorstatus.model.readDurability

/**
 * Everything the adapter needs to draw the weapon durability line, already decided.
 *
 * @property remaining uses left, in `0..max`.
 * @property max the item's total durability, positive.
 * @property percent the displayed percent, see [percentRounded].
 * @property argb opaque ARGB text colour.
 * @property showMax whether [valueText] appends the maximum.
 * @property showPercent whether [valueText] appends the percent.
 */
data class TooltipDurability(
    val remaining: Int,
    val max: Int,
    val percent: Int,
    val argb: Int,
    val showMax: Boolean,
    val showPercent: Boolean,
)

/**
 * The tooltip line for a stack with the game's `(maxDamage, damage)` pair, or `null` when no line should be added:
 * the feature is [WeaponTooltipSettings.enabled] off, the item cannot be damaged ([readDurability] says so), or it is
 * undamaged while [WeaponTooltipSettings.showUndamaged] is off. Total: malformed input is clamped by
 * [readDurability], never thrown on. Pure, so the whole decision is unit-tested and the adapter only builds the text.
 */
fun tooltipDurability(
    maxDamage: Int,
    damage: Int,
    settings: WeaponTooltipSettings,
): TooltipDurability? {
    if (!settings.enabled) return null
    val reading = readDurability(maxDamage, damage) as? DurabilityReading.Damageable ?: return null
    if (!settings.showUndamaged && reading.remaining == reading.max) return null
    return TooltipDurability(
        remaining = reading.remaining,
        max = reading.max,
        percent = reading.percentRounded(),
        argb = reading.color(settings.colorScheme, settings.lowDurabilityWarning, settings.warningThresholdPercent),
        showMax = settings.showMax,
        showPercent = settings.showPercent,
    )
}

/**
 * The language-neutral value of the line: the remaining durability, then ` / <max>` when [TooltipDurability.showMax]
 * and ` (<percent>%)` when [TooltipDurability.showPercent], for example `1480 / 1561 (95%)`, `1480 (95%)`,
 * `1480 / 1561` or `1480`. The remaining durability is always shown, so a line never reads as just `95%`. The adapter
 * puts this into a translated sentence (`Durability: <value>`); only digits and separators live here, so nothing needs
 * translating.
 */
fun TooltipDurability.valueText(): String =
    buildString {
        append(remaining)
        if (showMax) append(" / ").append(max)
        if (showPercent) append(" (").append(percent).append("%)")
    }

/**
 * Whether the adapter should add the durability line to a tooltip.
 *
 * - [isWeapon]: only weapons get the line; armor and tools are left alone.
 * - Not [isAdvancedTooltip]: vanilla prints its own `Durability: x / y` line in advanced mode (F3+H), so adding ours
 *   would show durability twice.
 * - [hasPlayer]: a null player means the tooltip is being built for the creative-menu search index, once for every
 *   item; a line added then would make every weapon match searches such as "durability" or "100".
 */
fun shouldAddTooltipLine(
    isWeapon: Boolean,
    isAdvancedTooltip: Boolean,
    hasPlayer: Boolean,
): Boolean = isWeapon && !isAdvancedTooltip && hasPlayer
