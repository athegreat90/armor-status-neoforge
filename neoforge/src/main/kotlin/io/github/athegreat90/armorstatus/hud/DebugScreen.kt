package io.github.athegreat90.armorstatus.hud

import net.minecraft.client.Minecraft

/**
 * Whether the F3 debug screen is toggled on.
 *
 * `Minecraft#debugEntries.isOverlayVisible()` is the flag the F3 key flips (`KeyboardHandler` calls
 * `toggleDebugOverlay()`), so it is true exactly while the F3 text pane is shown. `DebugScreenOverlay#showDebugScreen()`
 * is deliberately not used: it is also true when some entry is pinned "always on" (for example the performance
 * profile's FPS line) and it copies a list on every call. The declaration is identical on 26.1.2, 26.2 and 26.3, so
 * it needs no per-version overlay.
 */
internal fun Minecraft.isDebugScreenOpen(): Boolean = debugEntries.isOverlayVisible()
