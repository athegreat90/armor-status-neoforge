package io.github.athegreat90.armorstatus.weapon

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ItemFilterTest {
    @Test
    fun `no entries gives the empty filter`() {
        val filter = parseItemFilter(emptyList())
        assertEquals(ItemFilter.Empty, filter)
        assertTrue(filter.isEmpty)
    }

    @Test
    fun `an item id and a tag are told apart`() {
        val filter = parseItemFilter(listOf("examplemod:ruby_sword", "#examplemod:swords"))
        assertEquals(listOf("examplemod:ruby_sword"), filter.ids)
        assertEquals(listOf("examplemod:swords"), filter.tags)
        assertFalse(filter.isEmpty)
    }

    @ParameterizedTest(name = "{0} is read as {1}")
    @MethodSource("validEntries")
    fun `valid entries are normalised`(
        raw: String,
        expectedId: String,
    ) {
        assertEquals(listOf(expectedId), parseItemFilter(listOf(raw)).ids)
    }

    @ParameterizedTest(name = "{0} is read as the tag {1}")
    @MethodSource("validTagEntries")
    fun `valid tag entries are normalised`(
        raw: String,
        expectedTag: String,
    ) {
        val filter = parseItemFilter(listOf(raw))
        assertEquals(listOf(expectedTag), filter.tags)
        assertTrue(filter.ids.isEmpty())
    }

    @ParameterizedTest(name = "\"{0}\" is dropped")
    @MethodSource("invalidEntries")
    fun `invalid entries are dropped and never throw`(raw: String) {
        assertEquals(ItemFilter.Empty, parseItemFilter(listOf(raw)))
    }

    @Test
    fun `a bad entry does not hide the good ones around it`() {
        val filter = parseItemFilter(listOf("a:b", "not valid", "#c:d", "", "e:f"))
        assertEquals(listOf("a:b", "e:f"), filter.ids)
        assertEquals(listOf("c:d"), filter.tags)
    }

    @Test
    fun `duplicates are removed and the first position is kept`() {
        val filter = parseItemFilter(listOf("a:b", "minecraft:iron_sword", "A:B", "iron_sword", "#a:t", "#A:T"))
        assertEquals(listOf("a:b", "minecraft:iron_sword"), filter.ids)
        assertEquals(listOf("a:t"), filter.tags)
    }

    companion object {
        @JvmStatic
        fun validEntries(): Stream<Arguments> =
            Stream.of(
                Arguments.of("examplemod:ruby_sword", "examplemod:ruby_sword"),
                Arguments.of("iron_sword", "minecraft:iron_sword"),
                Arguments.of("  Minecraft:Iron_Sword  ", "minecraft:iron_sword"),
                Arguments.of("mod-id.x:tools/axes/big_one", "mod-id.x:tools/axes/big_one"),
            )

        @JvmStatic
        fun validTagEntries(): Stream<Arguments> =
            Stream.of(
                Arguments.of("#c:tools/melee_weapon", "c:tools/melee_weapon"),
                Arguments.of("#swords", "minecraft:swords"),
                Arguments.of("  #Examplemod:Swords ", "examplemod:swords"),
            )

        @JvmStatic
        fun invalidEntries(): Stream<String> =
            Stream.of(
                "",
                "   ",
                "#",
                ":",
                "a:",
                ":b",
                "a:b:c",
                "has space:x",
                "a:b c",
                "a/b:c",
                "a:b\$",
                "##a:b",
                "# a:b",
            )
    }
}
