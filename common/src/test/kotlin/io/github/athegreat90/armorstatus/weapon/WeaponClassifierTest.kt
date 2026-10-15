package io.github.athegreat90.armorstatus.weapon

import io.github.athegreat90.armorstatus.weapon.WeaponSignals.Companion.AXE_TAG
import io.github.athegreat90.armorstatus.weapon.WeaponSignals.Companion.BOW_ENCHANTABLE_TAG
import io.github.athegreat90.armorstatus.weapon.WeaponSignals.Companion.BOW_TAG
import io.github.athegreat90.armorstatus.weapon.WeaponSignals.Companion.CROSSBOW_ENCHANTABLE_TAG
import io.github.athegreat90.armorstatus.weapon.WeaponSignals.Companion.CROSSBOW_TAG
import io.github.athegreat90.armorstatus.weapon.WeaponSignals.Companion.DAMAGEABLE
import io.github.athegreat90.armorstatus.weapon.WeaponSignals.Companion.EXCLUDED_BY_USER
import io.github.athegreat90.armorstatus.weapon.WeaponSignals.Companion.INCLUDED_BY_USER
import io.github.athegreat90.armorstatus.weapon.WeaponSignals.Companion.KINETIC_WEAPON_COMPONENT
import io.github.athegreat90.armorstatus.weapon.WeaponSignals.Companion.MELEE_WEAPON_TAG
import io.github.athegreat90.armorstatus.weapon.WeaponSignals.Companion.NONE
import io.github.athegreat90.armorstatus.weapon.WeaponSignals.Companion.PIERCING_WEAPON_COMPONENT
import io.github.athegreat90.armorstatus.weapon.WeaponSignals.Companion.RANGED_WEAPON_TAG
import io.github.athegreat90.armorstatus.weapon.WeaponSignals.Companion.SPEAR_TAG
import io.github.athegreat90.armorstatus.weapon.WeaponSignals.Companion.SWORD_TAG
import io.github.athegreat90.armorstatus.weapon.WeaponSignals.Companion.TRIDENT_ENCHANTABLE_TAG
import io.github.athegreat90.armorstatus.weapon.WeaponSignals.Companion.WEAPON_ENCHANTABLE_TAG
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WeaponClassifierTest {
    private fun signals(vararg flags: Int): WeaponSignals = WeaponSignals(flags.fold(0) { acc, flag -> acc or flag })

    private val everyFlag: List<Int> =
        listOf(
            DAMAGEABLE,
            MELEE_WEAPON_TAG,
            RANGED_WEAPON_TAG,
            BOW_TAG,
            CROSSBOW_TAG,
            SWORD_TAG,
            AXE_TAG,
            SPEAR_TAG,
            WEAPON_ENCHANTABLE_TAG,
            BOW_ENCHANTABLE_TAG,
            CROSSBOW_ENCHANTABLE_TAG,
            TRIDENT_ENCHANTABLE_TAG,
            KINETIC_WEAPON_COMPONENT,
            PIERCING_WEAPON_COMPONENT,
        )

    // The two user-list flags are not weapon signals: they are decided by the item filters, so they stay out of everyFlag.
    private val userListFlags: List<Int> = listOf(INCLUDED_BY_USER, EXCLUDED_BY_USER)

    // --- the flags -------------------------------------------------------------------------------------------------

    @Test
    fun `every flag is a single distinct bit`() {
        val all = everyFlag + userListFlags
        all.forEach { assertEquals(1, Integer.bitCount(it), "flag ${Integer.toBinaryString(it)}") }
        assertEquals(all.size, all.toSet().size)
        assertEquals(all.size, Integer.bitCount(all.fold(0) { acc, flag -> acc or flag }))
    }

    @Test
    fun `NONE has no bits and has() sees none of the flags`() {
        assertEquals(0, NONE.bits)
        everyFlag.forEach { assertFalse(NONE.has(it)) }
    }

    @Test
    fun `has() is true only for the flags that were set`() {
        val set = signals(DAMAGEABLE, SWORD_TAG)
        assertTrue(set.has(DAMAGEABLE))
        assertTrue(set.has(SWORD_TAG))
        everyFlag.filter { it != DAMAGEABLE && it != SWORD_TAG }.forEach { assertFalse(set.has(it)) }
    }

    @Test
    fun `or() adds one flag and keeps the others`() {
        val built = NONE or DAMAGEABLE or BOW_TAG or RANGED_WEAPON_TAG
        assertEquals(signals(DAMAGEABLE, BOW_TAG, RANGED_WEAPON_TAG), built)
        assertEquals(built, built or BOW_TAG)
    }

    @Test
    fun `or() also combines two signal sets`() {
        assertEquals(
            signals(DAMAGEABLE, SWORD_TAG, MELEE_WEAPON_TAG),
            signals(DAMAGEABLE, SWORD_TAG) or signals(MELEE_WEAPON_TAG),
        )
    }

    // --- vanilla rows of the KDoc table ----------------------------------------------------------------------------

    @ParameterizedTest(name = "{0} is a weapon")
    @MethodSource("vanillaWeapons")
    fun `every vanilla weapon is a weapon`(
        name: String,
        bits: Int,
    ) {
        val signals = WeaponSignals(bits)
        assertTrue(classifyWeapon(signals), name)
    }

    @ParameterizedTest(name = "{0} is a weapon even when axes are excluded, except axes")
    @MethodSource("vanillaWeapons")
    fun `excluding axes drops only the axe`(
        name: String,
        bits: Int,
    ) {
        val signals = WeaponSignals(bits)
        val expected = name != "axe"
        assertEquals(expected, classifyWeapon(signals, WeaponPolicy(includeAxes = false)), name)
    }

    @ParameterizedTest(name = "{0} carries no weapon signal")
    @MethodSource("vanillaNonWeapons")
    fun `damageable tools and utilities are not weapons`(
        name: String,
        bits: Int,
    ) {
        val signals = WeaponSignals(bits)
        assertTrue(signals.has(DAMAGEABLE), "$name is a damageable fixture")
        assertFalse(classifyWeapon(signals), name)
        assertFalse(classifyWeapon(signals, WeaponPolicy(includeAxes = false)), name)
    }

    @Test
    fun `an empty stack is not a weapon`() {
        assertFalse(classifyWeapon(NONE))
    }

    @ParameterizedTest(name = "{0} without DAMAGEABLE is not a weapon")
    @MethodSource("vanillaWeapons")
    fun `an undamageable weapon is not a weapon`(
        name: String,
        bits: Int,
    ) {
        val signals = WeaponSignals(bits)
        val unbreakable = WeaponSignals(bits and DAMAGEABLE.inv())
        assertFalse(classifyWeapon(unbreakable), name)
    }

    @Test
    fun `every signal alone with DAMAGEABLE is enough while axes are included`() {
        val weaponFlags = everyFlag.filter { it != DAMAGEABLE }
        weaponFlags.forEach { flag ->
            assertTrue(classifyWeapon(signals(DAMAGEABLE, flag)), "flag ${Integer.toBinaryString(flag)}")
        }
    }

    @Test
    fun `a weapon signal without DAMAGEABLE is not enough`() {
        everyFlag.filter { it != DAMAGEABLE }.forEach { flag ->
            assertFalse(classifyWeapon(signals(flag)), "flag ${Integer.toBinaryString(flag)}")
        }
    }

    // --- axes ------------------------------------------------------------------------------------------------------

    @Test
    fun `an item that is only an axe by tag is a weapon while axes are included and not otherwise`() {
        // Many modded axes are tagged #minecraft:axes and nothing else.
        assertTrue(classifyWeapon(signals(DAMAGEABLE, AXE_TAG)))
        assertTrue(classifyWeapon(signals(DAMAGEABLE, AXE_TAG), WeaponPolicy(includeAxes = true)))
        assertFalse(classifyWeapon(signals(DAMAGEABLE, AXE_TAG), WeaponPolicy(includeAxes = false)))
    }

    @Test
    fun `an axe tag without DAMAGEABLE is never a weapon`() {
        assertFalse(classifyWeapon(signals(AXE_TAG)))
        assertFalse(classifyWeapon(signals(AXE_TAG, MELEE_WEAPON_TAG)))
    }

    @Test
    fun `axes are weapons by default and with includeAxes true`() {
        val axe = signals(DAMAGEABLE, AXE_TAG, MELEE_WEAPON_TAG, WEAPON_ENCHANTABLE_TAG)
        assertTrue(classifyWeapon(axe))
        assertTrue(classifyWeapon(axe, WeaponPolicy(includeAxes = true)))
        assertTrue(WeaponPolicy().includeAxes)
    }

    @Test
    fun `axes are excluded when includeAxes is false`() {
        val axe = signals(DAMAGEABLE, AXE_TAG, MELEE_WEAPON_TAG, WEAPON_ENCHANTABLE_TAG)
        assertFalse(classifyWeapon(axe, WeaponPolicy(includeAxes = false)))
    }

    @Test
    fun `an axe stays excluded whatever other generic weapon signals it has`() {
        val axe = signals(DAMAGEABLE, AXE_TAG, MELEE_WEAPON_TAG, RANGED_WEAPON_TAG, KINETIC_WEAPON_COMPONENT)
        assertFalse(classifyWeapon(axe, WeaponPolicy(includeAxes = false)))
    }

    @Test
    fun `a modded sword-axe stays a weapon when axes are excluded`() {
        val swordAxe = signals(DAMAGEABLE, AXE_TAG, SWORD_TAG, MELEE_WEAPON_TAG)
        assertTrue(classifyWeapon(swordAxe, WeaponPolicy(includeAxes = false)))
        assertTrue(classifyWeapon(swordAxe))
    }

    @Test
    fun `a modded spear-axe stays a weapon when axes are excluded`() {
        val spearAxe = signals(DAMAGEABLE, AXE_TAG, SPEAR_TAG, MELEE_WEAPON_TAG)
        assertTrue(classifyWeapon(spearAxe, WeaponPolicy(includeAxes = false)))
    }

    // --- modded items ----------------------------------------------------------------------------------------------

    @Test
    fun `a modded ranged weapon is recognised through the ranged tag alone`() {
        assertTrue(classifyWeapon(signals(DAMAGEABLE, RANGED_WEAPON_TAG)))
    }

    @Test
    fun `a modded melee weapon is recognised through the melee tag alone`() {
        assertTrue(classifyWeapon(signals(DAMAGEABLE, MELEE_WEAPON_TAG)))
    }

    @Test
    fun `a spear-like item is recognised through a component alone`() {
        assertTrue(classifyWeapon(signals(DAMAGEABLE, KINETIC_WEAPON_COMPONENT)))
        assertTrue(classifyWeapon(signals(DAMAGEABLE, PIERCING_WEAPON_COMPONENT)))
    }

    @Test
    fun `a modded sword-like item is recognised through the weapon enchantable tag alone`() {
        assertTrue(classifyWeapon(signals(DAMAGEABLE, WEAPON_ENCHANTABLE_TAG)))
    }

    // --- user include and exclude lists ----------------------------------------------------------------------------

    @Test
    fun `an included damageable item with no weapon signal is a weapon`() {
        assertTrue(classifyWeapon(signals(DAMAGEABLE, INCLUDED_BY_USER)))
        assertTrue(classifyWeapon(signals(DAMAGEABLE, INCLUDED_BY_USER), WeaponPolicy(includeAxes = false)))
    }

    @Test
    fun `an included item that is not damageable is still not a weapon`() {
        assertFalse(classifyWeapon(signals(INCLUDED_BY_USER)))
        assertFalse(classifyWeapon(signals(INCLUDED_BY_USER, SWORD_TAG)))
    }

    @ParameterizedTest(name = "excluded {0} is not a weapon")
    @MethodSource("vanillaWeapons")
    fun `an excluded item is never a weapon`(
        name: String,
        bits: Int,
    ) {
        assertFalse(classifyWeapon(WeaponSignals(bits or EXCLUDED_BY_USER)), name)
    }

    @Test
    fun `exclude wins over include`() {
        assertFalse(classifyWeapon(signals(DAMAGEABLE, INCLUDED_BY_USER, EXCLUDED_BY_USER)))
        assertFalse(classifyWeapon(signals(DAMAGEABLE, SWORD_TAG, INCLUDED_BY_USER, EXCLUDED_BY_USER)))
    }

    @Test
    fun `an included axe is a weapon even when axes are excluded`() {
        val axe = signals(DAMAGEABLE, AXE_TAG, MELEE_WEAPON_TAG, INCLUDED_BY_USER)
        assertTrue(classifyWeapon(axe, WeaponPolicy(includeAxes = false)))
    }

    @Test
    fun `the user list flags alone change nothing for non-weapons`() {
        assertFalse(classifyWeapon(signals(DAMAGEABLE)))
        assertFalse(classifyWeapon(NONE or INCLUDED_BY_USER or EXCLUDED_BY_USER))
    }

    companion object {
        private fun row(
            name: String,
            vararg flags: Int,
        ): Arguments = Arguments.of(name, flags.fold(0) { acc, flag -> acc or flag })

        /** The vanilla rows of the table in `WeaponClassifier.kt`, as the adapter is expected to report them. */
        @JvmStatic
        fun vanillaWeapons(): Stream<Arguments> =
            Stream.of(
                row("sword", DAMAGEABLE, SWORD_TAG, MELEE_WEAPON_TAG, WEAPON_ENCHANTABLE_TAG),
                row("axe", DAMAGEABLE, AXE_TAG, MELEE_WEAPON_TAG, WEAPON_ENCHANTABLE_TAG),
                row(
                    "spear",
                    DAMAGEABLE,
                    SPEAR_TAG,
                    MELEE_WEAPON_TAG,
                    KINETIC_WEAPON_COMPONENT,
                    PIERCING_WEAPON_COMPONENT,
                    WEAPON_ENCHANTABLE_TAG,
                ),
                row("mace", DAMAGEABLE, MELEE_WEAPON_TAG, WEAPON_ENCHANTABLE_TAG),
                row("trident", DAMAGEABLE, MELEE_WEAPON_TAG, RANGED_WEAPON_TAG, TRIDENT_ENCHANTABLE_TAG),
                row("bow", DAMAGEABLE, RANGED_WEAPON_TAG, BOW_TAG, BOW_ENCHANTABLE_TAG),
                row("crossbow", DAMAGEABLE, RANGED_WEAPON_TAG, CROSSBOW_TAG, CROSSBOW_ENCHANTABLE_TAG),
            )

        /** Damageable vanilla items that carry none of the weapon bits. */
        @JvmStatic
        fun vanillaNonWeapons(): Stream<Arguments> =
            Stream.of(
                row("pickaxe", DAMAGEABLE),
                row("shovel", DAMAGEABLE),
                row("hoe", DAMAGEABLE),
                row("fishing rod", DAMAGEABLE),
                row("flint and steel", DAMAGEABLE),
                row("shield", DAMAGEABLE),
                row("shears", DAMAGEABLE),
                row("elytra", DAMAGEABLE),
            )
    }
}
