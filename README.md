# Armor Status NeoForge

A client-side NeoForge mod that shows the durability of your worn armor on the HUD: one row per piece with the item icon and the remaining durability. It is written in Kotlin on top of KotlinLangForge and targets Minecraft 26.1.2, 26.2 and 26.3.

> **Unofficial.** This project is not affiliated with, endorsed by, or an official release of [ShirjeelPlays](https://github.com/ShirjeelPlays/Armor-Status), the author of the Fabric mod [Armor Status](https://modrinth.com/mod/armor-status) that inspired it. It is an independent, clean-room reimplementation. See [ATTRIBUTION.md](ATTRIBUTION.md).

**Status:** in development. No release has been published, and the Modrinth page for this mod does not exist yet.

## Features

- Up to four rows, in the order head, chest, legs, feet. Empty slots and items without durability are skipped.
- Each row shows the vanilla item icon and the remaining durability.
- Default text is raw durability points in white with a shadow.
- Optional `PERCENT` display mode and optional green-to-red gradient colours. Both are off by default.
- Six anchors: top, middle or bottom, on the left or right.
- Scale from 0.5 to 2.0, applied on top of Minecraft's GUI scale.
- Toggles per armor slot, plus a global enable toggle.
- Hidden while spectating, while the HUD is hidden with F1, and when there is no player.
- While the F3 debug screen is open the HUD stays visible but compact (scaled down, so the rows sit closer together and cover less of the debug text). Choose `COMPACT`, `HIDE` or `KEEP` with `debugScreenMode`. In `COMPACT` mode the HUD also moves to its own anchor (`debugScreenAnchor`, bottom right by default) so it does not sit under the F3 text.
- Client-only: the server does not need the mod.

## Supported Minecraft versions

One jar is built per Minecraft version. Each jar declares an exact Minecraft range and a minimum NeoForge build.

| Minecraft | NeoForge build used for development | Notes |
|---|---|---|
| 26.1.2 | 26.1.2.115 | |
| 26.2 | 26.2.0.89 | |
| 26.3 | 26.3.0.64-beta | NeoForge beta only. Treat this target as beta quality. |

## Requirements for players

- NeoForge for your Minecraft version (see the table above).
- Java 25 (normally provided by the launcher).
- **[KotlinLangForge](https://modrinth.com/mod/kotlin-lang-forge) is required.** The mod uses it as its language provider and does not bundle it. Development used `2.15.0-k2.4.21-3.1+neoforge`.
- The server does not need the mod.

## Configuration

Edit `config/armorstatusneoforge-client.toml`, or open **Mods > Armor Status NeoForge > Config** in game. Changes to the file are picked up while the game runs.

| Key | Default | Allowed values |
|---|---|---|
| `enabled` | `true` | `true`, `false` |
| `position` | `MIDDLE_LEFT` | `TOP_LEFT`, `TOP_RIGHT`, `MIDDLE_LEFT`, `MIDDLE_RIGHT`, `BOTTOM_LEFT`, `BOTTOM_RIGHT` |
| `scale` | `1.0` | `0.5` to `2.0` |
| `showHelmet` | `true` | `true`, `false` |
| `showChestplate` | `true` | `true`, `false` |
| `showLeggings` | `true` | `true`, `false` |
| `showBoots` | `true` | `true`, `false` |
| `displayMode` | `RAW` | `RAW` (remaining durability points), `PERCENT` |
| `gradientColors` | `false` | `true`, `false` |
| `debugScreenMode` | `COMPACT` | `COMPACT` (smaller while F3 is open), `HIDE` (not drawn while F3 is open), `KEEP` (unchanged) |
| `debugScreenScale` | `0.6` | `0.25` to `1.0`; extra factor applied on top of `scale` while F3 is open in `COMPACT` mode |
| `debugScreenAnchor` | `BOTTOM_RIGHT` | Same values as `position`; where the HUD is pinned while F3 is open in `COMPACT` mode. Ignored in `HIDE` and `KEEP`, and when F3 is closed `position` applies |

NeoForge corrects invalid values in the file: numbers are clamped to the nearest bound, and invalid enum or boolean values are reset to their defaults.

## Development

The project can live in any directory; the build does not depend on an absolute path. The author's local checkout is `C:\Users\alexa\Documents\GitRepo\armor-status-neoforge`.

### Prerequisites

- JDK 25.
- Network access on first build. Gradle downloads a JDK 21 through the Foojay toolchain resolver, because ModDevGradle's asset download task needs a Java 21 launcher.
- The Gradle wrapper (9.2.1) is committed; do not use a locally installed Gradle.

### Commands

Run from the repository root. On Windows use `gradlew.bat`; on Linux and macOS use `./gradlew`.

```powershell
.\gradlew.bat build                            # compile, check and package for the default target (26.1.2)
.\gradlew.bat build "-PmcVersion=26.2"         # same, for another target
.\gradlew.bat :common:test                     # unit tests of the pure module
.\gradlew.bat check "-PmcVersion=26.2"         # Spotless (ktlint) check + jar metadata verification + tests
.\gradlew.bat spotlessApply                    # format Kotlin sources and Gradle scripts
.\gradlew.bat runClient "-PmcVersion=26.2"     # start a dev client with the mod loaded
.\gradlew.bat runClient "-PquickPlay=TestWorld"  # dev only: join the save neoforge/run/saves/TestWorld on launch
.\gradlew.bat buildAll                         # build 26.1.2, 26.2 and 26.3 in order
```

> **PowerShell quoting:** always quote `-PmcVersion`. Unquoted, PowerShell splits `-PmcVersion=26.2` at the dot and Gradle receives `26`.

Valid `mcVersion` values are the file names in `gradle/mc/` (`26.1.2`, `26.2`, `26.3`). Any other value fails fast with the list of available versions. Jars are written to `neoforge/build/libs/armor-status-neoforge-<version>+mc<minecraft>.jar`.

### Module layout

| Path | Contents |
|---|---|
| `common/` | Pure Kotlin with no Minecraft dependency: durability model, text and colour, layout, settings, visibility rule, error recovery. Unit tested. |
| `neoforge/` | NeoForge adapter: `@Mod` entrypoint, `ModConfigSpec` binding, config screen, GUI layer, `HudCanvas`. |
| `neoforge/src/v26_1`, `neoforge/src/v26_2` | Per-version overlay with the only code that differs: the F1 "hide GUI" check. 26.2 and 26.3 share `v26_2`. |
| `gradle/mc/` | One properties file per Minecraft version: NeoForge build and Minecraft range. |

### Architecture

All decisions that can be expressed without Minecraft types live in `common` and are plain functions over immutable data. The `neoforge` module only reads game state, calls those functions, and draws through a small `HudCanvas` adapter.

The HUD runs every frame, so the render path uses indexed loops over fixed arrays, no collection pipelines or per-frame allocations, and caches the layout and the durability strings until their inputs change. Settings are an immutable snapshot swapped atomically when the config file loads.

### Tests

`common` has JUnit 5 and kotlin-test unit tests covering durability reading, text and colour, layout for every anchor, settings validation, the visibility rule, caches, and error recovery.

NeoForge-bound code has no unit tests. It is verified by compilation on all three targets, by the metadata check on the built jar, and by dev-client runs. Unit tests are not evidence of in-game behaviour.

## Known limitations

- In-game checks so far cover only the default look and the F3 compact layout, on Minecraft 26.1.2, 26.2 and 26.3. Damaged armor, GUI scales, the other anchors, the `HIDE` and `KEEP` F3 modes and spectator mode have not been checked on screen.
- Minecraft 26.3 builds against a NeoForge beta; there is no stable NeoForge 26.3 build.
- The config screen shows enum values by their raw names (for example `MIDDLE_LEFT`), not translated labels.
- Joining a vanilla Minecraft 26.2 server with the mod installed works (checked in the logs). Vanilla 26.1.2 and 26.3 servers have not been tried, and the HUD was not checked on screen on a vanilla server.
- Like the original mod, there is no offhand handling and no special handling for elytra beyond the chest slot.
- The config screen has not been opened in a recorded test.
- No release has been published.

## Credits

Inspired by Armor Status by ShirjeelPlays. See [ATTRIBUTION.md](ATTRIBUTION.md) for details and for the licence situation of the original project.

## License

[MIT](LICENSE). KotlinLangForge is a separate GPL-3.0 runtime dependency and is not bundled with this mod.
