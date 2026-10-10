package io.github.athegreat90.armorstatus

import com.mojang.logging.LogUtils
import net.neoforged.api.distmarker.Dist
import net.neoforged.fml.common.Mod

/** Client-only entrypoint, instantiated by KotlinLangForge (the `klf` language provider). */
@Mod(value = MOD_ID, dist = [Dist.CLIENT])
object ArmorStatusNeoForge {
    private val logger = LogUtils.getLogger()

    init {
        // remainingPercent lives in :common; calling it proves common's classes are on the runtime mod classpath.
        logger.info("Armor Status NeoForge loaded (Kotlin {}, common probe {}%)", KotlinVersion.CURRENT, remainingPercent(damage = 25, maxDamage = 100))
    }
}
