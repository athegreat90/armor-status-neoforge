package io.github.athegreat90.armorstatus.tooltip

import io.github.athegreat90.armorstatus.weapon.WeaponSignals
import net.minecraft.core.component.DataComponents
import net.minecraft.tags.ItemTags
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.common.Tags

/**
 * Reports what this stack is (tag membership, data components, damageability) as [WeaponSignals]. It only observes:
 * the decision of what counts as a weapon lives in `classifyWeapon` in `common`. An empty stack reports
 * [WeaponSignals.NONE]. The user's [included] and [excluded] lists are reported as signals too; empty lists cost
 * nothing.
 *
 * Nothing is cached, because the tag sets are rebound on datapack reload. The mask is built in a local `Int`, so
 * nothing is allocated.
 */
internal fun ItemStack.weaponSignals(
    included: ResolvedItemFilter = ResolvedItemFilter.Empty,
    excluded: ResolvedItemFilter = ResolvedItemFilter.Empty,
): WeaponSignals {
    if (isEmpty) return WeaponSignals.NONE
    var bits = 0
    if (isDamageableItem) bits = bits or WeaponSignals.DAMAGEABLE
    if (!included.isEmpty && included.matches(this)) bits = bits or WeaponSignals.INCLUDED_BY_USER
    if (!excluded.isEmpty && excluded.matches(this)) bits = bits or WeaponSignals.EXCLUDED_BY_USER
    if (`is`(Tags.Items.MELEE_WEAPON_TOOLS)) bits = bits or WeaponSignals.MELEE_WEAPON_TAG
    if (`is`(Tags.Items.RANGED_WEAPON_TOOLS)) bits = bits or WeaponSignals.RANGED_WEAPON_TAG
    if (`is`(Tags.Items.TOOLS_BOW)) bits = bits or WeaponSignals.BOW_TAG
    if (`is`(Tags.Items.TOOLS_CROSSBOW)) bits = bits or WeaponSignals.CROSSBOW_TAG
    if (`is`(ItemTags.SWORDS)) bits = bits or WeaponSignals.SWORD_TAG
    if (`is`(ItemTags.AXES)) bits = bits or WeaponSignals.AXE_TAG
    if (`is`(ItemTags.SPEARS)) bits = bits or WeaponSignals.SPEAR_TAG
    if (`is`(ItemTags.WEAPON_ENCHANTABLE)) bits = bits or WeaponSignals.WEAPON_ENCHANTABLE_TAG
    if (`is`(ItemTags.BOW_ENCHANTABLE)) bits = bits or WeaponSignals.BOW_ENCHANTABLE_TAG
    if (`is`(ItemTags.CROSSBOW_ENCHANTABLE)) bits = bits or WeaponSignals.CROSSBOW_ENCHANTABLE_TAG
    if (`is`(ItemTags.TRIDENT_ENCHANTABLE)) bits = bits or WeaponSignals.TRIDENT_ENCHANTABLE_TAG
    if (has(DataComponents.KINETIC_WEAPON)) bits = bits or WeaponSignals.KINETIC_WEAPON_COMPONENT
    if (has(DataComponents.PIERCING_WEAPON)) bits = bits or WeaponSignals.PIERCING_WEAPON_COMPONENT
    return WeaponSignals(bits)
}
