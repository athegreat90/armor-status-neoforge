package io.github.athegreat90.armorstatus.config

/** What the armor HUD does while the F3 debug screen is open. */
enum class DebugScreenMode {
    /** Stay visible but drawn smaller (see [HudSettings.debugScreenScale]) so it covers less of the debug text. */
    COMPACT,

    /** Not drawn at all. */
    HIDE,

    /** Unchanged: the debug screen has no effect on the HUD. */
    KEEP,
}
