package io.github.athegreat90.armorstatus

import com.mojang.logging.LogUtils
import net.neoforged.api.distmarker.Dist
import net.neoforged.fml.common.Mod

/** Client-only entrypoint, instantiated by KotlinLangForge (the `klf` language provider). */
@Mod(value = MOD_ID, dist = [Dist.CLIENT])
object ArmorStatusNeoForge {
    private val logger = LogUtils.getLogger()

    init {
        logger.info("Armor Status NeoForge loaded (Kotlin {})", KotlinVersion.CURRENT)
    }
}
