package io.github.athegreat90.armorstatus.tooltip

import io.github.athegreat90.armorstatus.weapon.ItemFilter
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack

/**
 * An [ItemFilter] turned into registry keys, so the tooltip can test a stack without parsing or allocating.
 *
 * Only the keys are kept, never the items or the tag contents: tags are rebound on datapack reload, so membership is
 * asked of the stack each time. Immutable; built once per config refresh by [resolve].
 */
internal class ResolvedItemFilter private constructor(
    private val ids: Set<Identifier>,
    private val tags: Array<TagKey<Item>>,
) {
    val isEmpty: Boolean get() = ids.isEmpty() && tags.isEmpty()

    /** True when the stack's item id is listed, or the stack is in any listed tag. Cheap bit of work per call. */
    fun matches(stack: ItemStack): Boolean {
        if (ids.isNotEmpty() && BuiltInRegistries.ITEM.getKey(stack.item) in ids) return true
        for (i in tags.indices) {
            if (stack.`is`(tags[i])) return true
        }
        return false
    }

    companion object {
        val Empty: ResolvedItemFilter = ResolvedItemFilter(emptySet(), emptyArray())

        /** Entries that do not parse as an identifier are skipped; [ItemFilter] already normalised their syntax. */
        fun of(filter: ItemFilter): ResolvedItemFilter {
            if (filter.isEmpty) return Empty
            val ids = LinkedHashSet<Identifier>()
            for (id in filter.ids) Identifier.tryParse(id)?.let(ids::add)
            val tags = ArrayList<TagKey<Item>>(filter.tags.size)
            for (tag in filter.tags) Identifier.tryParse(tag)?.let { tags.add(TagKey.create(Registries.ITEM, it)) }
            return ResolvedItemFilter(ids, tags.toTypedArray())
        }
    }
}
