package io.github.athegreat90.armorstatus.hud

import net.minecraft.client.Minecraft

/** Minecraft 26.1.x keeps the F1 "hide GUI" flag in the options. */
internal fun Minecraft.isHudHidden(): Boolean = options.hideGui
