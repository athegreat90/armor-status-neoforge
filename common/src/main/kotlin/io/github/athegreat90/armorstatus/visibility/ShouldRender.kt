package io.github.athegreat90.armorstatus.visibility

import io.github.athegreat90.armorstatus.config.HudSettings

/**
 * Whether the HUD should be drawn this frame: only when enabled, in a world, not spectating and with the HUD shown,
 * and not while the F3 debug screen is displayed when [HudSettings.hideWhenDebugScreen] is on.
 */
fun shouldRender(
    settings: HudSettings,
    hasPlayer: Boolean,
    isSpectator: Boolean,
    hudHidden: Boolean,
    debugScreenOpen: Boolean,
): Boolean =
    settings.enabled &&
        hasPlayer &&
        !isSpectator &&
        !hudHidden &&
        !(debugScreenOpen && settings.hideWhenDebugScreen)
