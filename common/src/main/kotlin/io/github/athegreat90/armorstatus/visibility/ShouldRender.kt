package io.github.athegreat90.armorstatus.visibility

import io.github.athegreat90.armorstatus.config.HudSettings

/** Whether the HUD should be drawn this frame: only when enabled, in a world, not spectating and with the HUD shown. */
fun shouldRender(
    settings: HudSettings,
    hasPlayer: Boolean,
    isSpectator: Boolean,
    hudHidden: Boolean,
): Boolean = settings.enabled && hasPlayer && !isSpectator && !hudHidden
