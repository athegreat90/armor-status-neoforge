package io.github.athegreat90.armorstatus.hud

import net.minecraft.client.Minecraft

/** Minecraft 26.2 and later keep the F1 "hide GUI" flag in the `Hud` object (`Options#hideGui` is gone). */
internal fun Minecraft.isHudHidden(): Boolean = gui.hud.isHidden()
