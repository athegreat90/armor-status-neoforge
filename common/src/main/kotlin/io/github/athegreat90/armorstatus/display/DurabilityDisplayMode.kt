package io.github.athegreat90.armorstatus.display

/** How the remaining durability is written next to the armor icon. */
enum class DurabilityDisplayMode {
    /** The remaining durability as a plain integer, for example `123`. Matches the upstream mod. */
    RAW,

    /** The remaining durability as a percentage of the maximum, for example `34%`. */
    PERCENT,
    ;

    companion object {
        /** Upstream parity: plain integer. */
        val DEFAULT: DurabilityDisplayMode = RAW
    }
}
