package io.github.athegreat90.armorstatus.visibility

import io.github.athegreat90.armorstatus.config.DebugScreenMode
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
        assertEquals(expected, shouldRender(settings, hasPlayer, isSpectator, hudHidden, debugScreenOpen = false))
    }

    @ParameterizedTest(name = "mode={0} debugScreenOpen={1} -> {2}")
    @CsvSource(
        "COMPACT, true, true",
        "COMPACT, false, true",
        "HIDE, true, false",
        "HIDE, false, true",
        "KEEP, true, true",
        "KEEP, false, true",
    )
    fun `the debug screen hides the hud only in hide mode`(
        mode: DebugScreenMode,
        debugScreenOpen: Boolean,
        expected: Boolean,
    ) {
        val settings = HudSettings.Default.copy(debugScreenMode = mode)
        assertEquals(
            expected,
            shouldRender(settings, hasPlayer = true, isSpectator = false, hudHidden = false, debugScreenOpen),
        )
    }

    @ParameterizedTest(name = "mode={0} debugScreenOpen={1} never overrides a hidden hud")
    @CsvSource(
        "COMPACT, true",
        "COMPACT, false",
        "HIDE, true",
        "HIDE, false",
        "KEEP, true",
        "KEEP, false",
    )
    fun `the debug flag cannot make a hidden hud visible`(
        mode: DebugScreenMode,
        debugScreenOpen: Boolean,
    ) {
        val settings = HudSettings.Default.copy(debugScreenMode = mode)
        assertEquals(
            false,
            shouldRender(settings, hasPlayer = true, isSpectator = true, hudHidden = false, debugScreenOpen),
        )
        assertEquals(
            false,
            shouldRender(settings, hasPlayer = true, isSpectator = false, hudHidden = true, debugScreenOpen),
        )
        assertEquals(
            false,
            shouldRender(settings, hasPlayer = false, isSpectator = false, hudHidden = false, debugScreenOpen),
        )
    }
}
