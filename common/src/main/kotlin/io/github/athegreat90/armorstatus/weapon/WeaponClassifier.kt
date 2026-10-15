package io.github.athegreat90.armorstatus.weapon

/**
 * What the adapter observed about one item stack, as a bitmask in an `Int`.
 *
 * `common` has no Minecraft types, so the adapter reads the stack (tag membership, data components) and reports the
 * result here; [classifyWeapon] then makes the decision in pure code. A value class keeps this free of allocation on
 * the tooltip path, and every flag is a distinct bit (checked by a test).
 *
 * Build one with [NONE] and [or]: `WeaponSignals.NONE or DAMAGEABLE or SWORD_TAG`.
 *
 * ### Signals set for vanilla items
 *
 * This is the contract the adapter is expected to honour. It was derived from the 26.1.2, 26.2 and 26.3 jars, which
 * agree; the real stack-to-signals mapping is **verified in game in T6**, not by these unit tests.
 *
 * | Item | Signals |
 * |---|---|
 * | Swords | [DAMAGEABLE], [SWORD_TAG], [MELEE_WEAPON_TAG], [WEAPON_ENCHANTABLE_TAG] |
 * | Axes | [DAMAGEABLE], [AXE_TAG], [MELEE_WEAPON_TAG], [WEAPON_ENCHANTABLE_TAG] |
 * | Spears | [DAMAGEABLE], [SPEAR_TAG], [MELEE_WEAPON_TAG], [KINETIC_WEAPON_COMPONENT], [PIERCING_WEAPON_COMPONENT], [WEAPON_ENCHANTABLE_TAG] |
 * | Mace | [DAMAGEABLE], [MELEE_WEAPON_TAG], [WEAPON_ENCHANTABLE_TAG] |
 * | Trident | [DAMAGEABLE], [MELEE_WEAPON_TAG], [RANGED_WEAPON_TAG], [TRIDENT_ENCHANTABLE_TAG] |
 * | Bow | [DAMAGEABLE], [RANGED_WEAPON_TAG], [BOW_TAG], [BOW_ENCHANTABLE_TAG] |
 * | Crossbow | [DAMAGEABLE], [RANGED_WEAPON_TAG], [CROSSBOW_TAG], [CROSSBOW_ENCHANTABLE_TAG] |
 * | Pickaxe, shovel, hoe, fishing rod, flint and steel, shield, shears | [DAMAGEABLE] only |
 *
 * [DAMAGEABLE] means the stack is non-empty and damageable (an unbreakable weapon does not set it). A stack that
 * is empty reports [NONE].
 *
 * @property bits the raw mask; combine flags with [or], test them with [has].
 */
@JvmInline
value class WeaponSignals(
    val bits: Int,
) {
    /** True when at least one bit of [flag] is set. Pass one flag constant; combined masks mean "any of them". */
    fun has(flag: Int): Boolean = (bits and flag) != 0

    /** A copy with [flag] added. */
    infix fun or(flag: Int): WeaponSignals = WeaponSignals(bits or flag)

    /** A copy with every bit of [other] added. */
    infix fun or(other: WeaponSignals): WeaponSignals = WeaponSignals(bits or other.bits)

    companion object {
        /** Nothing observed: an empty stack. */
        val NONE: WeaponSignals = WeaponSignals(0)

        /** The stack is non-empty and damageable (has max damage, is not unbreakable). A precondition for everything. */
        const val DAMAGEABLE: Int = 1 shl 0

        /** Item tag `c:tools/melee_weapon` (sword, axe, spear, mace and trident in vanilla). */
        const val MELEE_WEAPON_TAG: Int = 1 shl 1

        /** Item tag `c:tools/ranged_weapon` (bow, crossbow and trident in vanilla). */
        const val RANGED_WEAPON_TAG: Int = 1 shl 2

        /** Item tag `c:tools/bow`. */
        const val BOW_TAG: Int = 1 shl 3

        /** Item tag `c:tools/crossbow`. */
        const val CROSSBOW_TAG: Int = 1 shl 4

        /** Item tag `#minecraft:swords`. */
        const val SWORD_TAG: Int = 1 shl 5

        /** Item tag `#minecraft:axes`. A weapon signal only while [WeaponPolicy.includeAxes] is true: see [classifyWeapon]. */
        const val AXE_TAG: Int = 1 shl 6

        /** Item tag `#minecraft:spears`. */
        const val SPEAR_TAG: Int = 1 shl 7

        /** Item tag `#minecraft:enchantable/weapon`. */
        const val WEAPON_ENCHANTABLE_TAG: Int = 1 shl 8

        /** Item tag `#minecraft:enchantable/bow`. */
        const val BOW_ENCHANTABLE_TAG: Int = 1 shl 9

        /** Item tag `#minecraft:enchantable/crossbow`. */
        const val CROSSBOW_ENCHANTABLE_TAG: Int = 1 shl 10

        /** Item tag `#minecraft:enchantable/trident`. */
        const val TRIDENT_ENCHANTABLE_TAG: Int = 1 shl 11

        /** The stack has the `kinetic_weapon` data component (spears; catches modded spear-likes). */
        const val KINETIC_WEAPON_COMPONENT: Int = 1 shl 12

        /** The stack has the `piercing_weapon` data component (spears; catches modded spear-likes). */
        const val PIERCING_WEAPON_COMPONENT: Int = 1 shl 13

        /** The item matches the user's `includeItems` list. Makes a damageable item a weapon; see [classifyWeapon]. */
        const val INCLUDED_BY_USER: Int = 1 shl 14

        /** The item matches the user's `excludeItems` list. Always wins over every other signal. */
        const val EXCLUDED_BY_USER: Int = 1 shl 15

        /** Every flag that, together with [DAMAGEABLE], proves a weapon on its own: all but [DAMAGEABLE] and [AXE_TAG], which
         * [classifyWeapon] handles separately because of [WeaponPolicy.includeAxes]. The two user-list flags are not
         * in this mask either: [classifyWeapon] applies them first. */
        internal const val WEAPON_SIGNALS: Int =
            MELEE_WEAPON_TAG or RANGED_WEAPON_TAG or BOW_TAG or CROSSBOW_TAG or SWORD_TAG or SPEAR_TAG or
                WEAPON_ENCHANTABLE_TAG or BOW_ENCHANTABLE_TAG or CROSSBOW_ENCHANTABLE_TAG or
                TRIDENT_ENCHANTABLE_TAG or KINETIC_WEAPON_COMPONENT or PIERCING_WEAPON_COMPONENT
    }
}

/**
 * User choices that change which items [classifyWeapon] accepts.
 *
 * @property includeAxes whether axes count as weapons. Axes are in the melee-weapon tag, so they are included by
 *   default; turn this off for players who only want swords, spears and ranged weapons.
 */
data class WeaponPolicy(
    val includeAxes: Boolean = true,
)

/**
 * Whether the stack described by [signals] is a weapon whose durability the tooltip should show.
 *
 * Rules, in order:
 * 1. Not [WeaponSignals.DAMAGEABLE] (empty, undamageable or unbreakable): never a weapon, even when the user listed it.
 * 2. [WeaponSignals.EXCLUDED_BY_USER]: never a weapon. It wins over [WeaponSignals.INCLUDED_BY_USER] and over every
 *    other signal.
 * 3. [WeaponSignals.INCLUDED_BY_USER]: a weapon, whatever the other signals and [WeaponPolicy.includeAxes] say.
 * 4. With [WeaponPolicy.includeAxes] false, an item tagged [WeaponSignals.AXE_TAG] is excluded, unless it is also
 *    tagged [WeaponSignals.SWORD_TAG] or [WeaponSignals.SPEAR_TAG]: a modded sword-axe is still a weapon.
 * 5. Otherwise it is a weapon when any signal other than [WeaponSignals.DAMAGEABLE] is present, [WeaponSignals.AXE_TAG]
 *    included: many modded axes are tagged `#minecraft:axes` and nothing else, and with axes included they count.
 *
 * No signal means no weapon, which excludes pickaxes, shovels, hoes, fishing rods, flint and steel, shields, shears
 * and elytra: they are damageable but carry none of the weapon signals. The data component `weapon` is deliberately
 * not a signal (pickaxes, shovels and hoes have it too). See [WeaponSignals] for the per-item signal table.
 */
fun classifyWeapon(
    signals: WeaponSignals,
    policy: WeaponPolicy = WeaponPolicy(),
): Boolean {
    if (!signals.has(WeaponSignals.DAMAGEABLE)) return false
    if (signals.has(WeaponSignals.EXCLUDED_BY_USER)) return false
    if (signals.has(WeaponSignals.INCLUDED_BY_USER)) return true
    if (signals.has(WeaponSignals.AXE_TAG) && !policy.includeAxes) {
        return signals.has(WeaponSignals.SWORD_TAG or WeaponSignals.SPEAR_TAG)
    }
    return signals.has(WeaponSignals.WEAPON_SIGNALS or WeaponSignals.AXE_TAG)
}
