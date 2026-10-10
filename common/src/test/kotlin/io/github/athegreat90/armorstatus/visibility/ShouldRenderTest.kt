package io.github.athegreat90.armorstatus.visibility

import io.github.athegreat90.armorstatus.config.HudSettings
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.assertEquals

class ShouldRenderTest {
    @ParameterizedTest(name = "enabled={0} player={1} spectator={2} hudHidden={3} -> {4}")
    @CsvSource(
        "true, true, false, false, true",
        "false, true, false, false, false",
        "true, false, false, false, false",
        "true, true, true, false, false",
        "true, true, false, true, false",
        "false, false, true, true, false",
    )
    fun `renders only when enabled, in game, not spectating and the hud is visible`(
        enabled: Boolean,
        hasPlayer: Boolean,
        isSpectator: Boolean,
        hudHidden: Boolean,
        expected: Boolean,
    ) {
        val settings = HudSettings.Default.copy(enabled = enabled)
        assertEquals(expected, shouldRender(settings, hasPlayer, isSpectator, hudHidden))
    }
}
