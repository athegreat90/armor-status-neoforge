package io.github.athegreat90.armorstatus.weapon

private const val DEFAULT_NAMESPACE = "minecraft"
private const val TAG_PREFIX = '#'
private const val SEPARATOR = ':'

// The characters Minecraft allows in an identifier: namespace [a-z0-9_.-], path [a-z0-9_./-].
private val NAMESPACE_PATTERN = Regex("[a-z0-9_.-]+")
private val PATH_PATTERN = Regex("[a-z0-9_./-]+")

/**
 * A user-written list of items, as two lists of normalised identifiers: item ids (`modid:item`) and item tag ids
 * (the `modid:tag` part of `#modid:tag`). Every entry is lower case, has a namespace, and appears once.
 *
 * `common` only validates and normalises the text; the adapter turns the entries into registry keys, because
 * `common` has no Minecraft types. Build one from untrusted config text with [parseItemFilter].
 *
 * @property ids item ids such as `minecraft:iron_sword`.
 * @property tags item tag ids such as `c:tools/melee_weapon`, without the leading `#`.
 */
data class ItemFilter(
    val ids: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
) {
    /** True when the filter matches nothing, so the adapter can skip every per-stack check. */
    val isEmpty: Boolean get() = ids.isEmpty() && tags.isEmpty()

    companion object {
        val Empty: ItemFilter = ItemFilter()
    }
}

/**
 * Lenient parser for the `includeItems` and `excludeItems` config lists. Never throws.
 *
 * Each entry is trimmed and lower-cased. A leading `#` makes it a tag. A missing namespace defaults to `minecraft`
 * (`iron_sword` is `minecraft:iron_sword`). Entries that are blank or not a valid identifier (spaces, a second
 * colon, a `/` in the namespace, a lone `#`) are dropped without hiding the valid ones. Duplicates are removed and
 * the first position is kept.
 */
fun parseItemFilter(entries: List<String>): ItemFilter {
    if (entries.isEmpty()) return ItemFilter.Empty
    val ids = LinkedHashSet<String>()
    val tags = LinkedHashSet<String>()
    for (entry in entries) {
        val trimmed = entry.trim().lowercase()
        val isTag = trimmed.startsWith(TAG_PREFIX)
        val identifier = normalised(if (isTag) trimmed.substring(1) else trimmed) ?: continue
        if (isTag) tags.add(identifier) else ids.add(identifier)
    }
    return if (ids.isEmpty() && tags.isEmpty()) ItemFilter.Empty else ItemFilter(ids.toList(), tags.toList())
}

/** `namespace:path`, with the default namespace added when missing, or null when [raw] is not a valid identifier. */
private fun normalised(raw: String): String? {
    val separator = raw.indexOf(SEPARATOR)
    val namespace = if (separator < 0) DEFAULT_NAMESPACE else raw.substring(0, separator)
    val path = if (separator < 0) raw else raw.substring(separator + 1)
    if (!NAMESPACE_PATTERN.matches(namespace) || !PATH_PATTERN.matches(path)) return null
    return "$namespace$SEPARATOR$path"
}
