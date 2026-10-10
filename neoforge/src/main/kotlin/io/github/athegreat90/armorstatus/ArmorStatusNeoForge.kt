package io.github.athegreat90.armorstatus

import com.mojang.logging.LogUtils
import io.github.athegreat90.armorstatus.config.ConfigRefresh
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.Mod

/**
 * Client-only entrypoint, instantiated by KotlinLangForge (the `klf` language provider).
 * It is a class, not an `object`, because the constructor receives the mod event bus and container.
 */
@Mod(value = MOD_ID, dist = [Dist.CLIENT])
class ArmorStatusNeoForge(modBus: IEventBus, container: ModContainer) {
    init {
        ConfigRefresh.register(modBus, container)
        LogUtils.getLogger().info("Armor Status NeoForge loaded (Kotlin {})", KotlinVersion.CURRENT)
    }
}
