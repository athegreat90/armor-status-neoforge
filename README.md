# Armor Status NeoForge

A client-side NeoForge mod that shows the durability of your worn armor on the HUD: one row per piece with the item icon and the remaining durability. It also adds a durability line to the hover tooltip of weapons. It is written in Kotlin on top of KotlinLangForge and targets Minecraft 26.1.2, 26.2 and 26.3.

> **Unofficial.** This project is not affiliated with, endorsed by, or an official release of [ShirjeelPlays](https://github.com/ShirjeelPlays/Armor-Status), the author of the Fabric mod [Armor Status](https://modrinth.com/mod/armor-status) that inspired it. It is an independent, clean-room reimplementation. See [ATTRIBUTION.md](ATTRIBUTION.md).

**Status:** current version 0.3.0. It builds 26.3 against the stable NeoForge and adds item include and exclude lists for the weapon tooltip; version 0.2.0 added the weapon durability tooltip described below and 0.1.0 had the armor HUD only. Downloads are on the GitHub Releases page. The Modrinth project exists but its page was not public yet when this was written; check the project for its current state.

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
- Weapon durability in the item hover tooltip: one line right under the item name, for example `Durability: 1480 / 1561 (95%)`, with colour tiers. It appears wherever the game shows an item tooltip (inventory, chests, creative menu and other container screens); weapons are not drawn on the game HUD. See [Weapon durability tooltip](#weapon-durability-tooltip).
- Client-only: the server does not need the mod. The tooltip is also client side and adds no networking.

## Supported Minecraft versions

One jar is built per Minecraft version. Each jar declares an exact Minecraft range and a minimum NeoForge build.

| Minecraft | NeoForge build used for development | Notes |
|---|---|---|
| 26.1.2 | 26.1.2.115 | |
| 26.2 | 26.2.0.89 | |
| 26.3 | 26.3.0.84 | Stable NeoForge 26.3. |

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

The weapon tooltip has its own `[weaponTooltip]` section in the same file, described below.

## Weapon durability tooltip

Hover a weapon in the inventory, a chest, the creative menu or another container screen and the tooltip gets one extra line directly under the item name:

```text
Diamond Sword
Durability: 1480 / 1561 (95%)
```

The weapon durability is shown only in that hover tooltip. There are no weapon rows on the game HUD and the armor HUD is unchanged. The cursor is locked during normal play, so nothing about weapons is drawn on the game screen. The item-name popup that appears above the hotbar when you switch items is not a tooltip and is not covered.

### Which items count as weapons

The mod does not keep a list of items. A stack counts as a weapon when it is damageable (not empty and not unbreakable) and matches one of these standard signals:

- Item tags `c:tools/melee_weapon`, `c:tools/ranged_weapon`, `c:tools/bow`, `c:tools/crossbow`, `#minecraft:swords`, `#minecraft:axes`, `#minecraft:spears`.
- The enchantability tags for weapons, bows, crossbows and tridents (`#minecraft:enchantable/weapon`, `/bow`, `/crossbow`, `/trident`).
- The `kinetic_weapon` or `piercing_weapon` data components (spears and spear-like items).

By the vanilla tags that covers swords, axes, spears, the mace, the trident, the bow and the crossbow, and modded items that join those tags or carry those components. Axes can be switched off with `includeAxes`. Pickaxes, shovels, hoes, fishing rods, flint and steel, shears, shields, armor, elytra and unbreakable items get no line. The vanilla `weapon` data component is not used, because pickaxes, shovels and hoes carry it too.

### When the line is not added

- Advanced tooltips (F3+H) are on: vanilla already prints its own `Durability: x / y` line there, so the mod adds nothing.
- There is no player, which is how the game builds the creative menu search index. Adding a line then would make every weapon match a search such as "durability".
- `enabled` is off, or the weapon is undamaged and `showUndamaged` is off.

### Colours

The default scheme, `TIERS`, colours the line by the percent shown in the text:

| Displayed percent | Colour |
|---|---|
| above 50 | green |
| 26 to 50 | yellow |
| above `warningThresholdPercent` up to 25 | orange |
| at or below `warningThresholdPercent` (15 by default) | red |

A threshold of 25 or more removes the orange tier and 50 or more removes the yellow tier too. With `lowDurabilityWarning` off, the red tier is shown as orange. `WHITE` always draws white text. `GRADIENT` uses the same smooth colour gradient as the armor HUD's `gradientColors`.

### Weapon tooltip settings

These keys are in the `[weaponTooltip]` section of `config/armorstatusneoforge-client.toml`.

| Key | Default | Allowed values |
|---|---|---|
| `enabled` | `true` | `true`, `false`; adds the line at all |
| `showMax` | `true` | `true`, `false`; `1480 / 1561` instead of `1480` |
| `showPercent` | `true` | `true`, `false`; appends `(95%)`, rounded like the armor `PERCENT` mode |
| `showUndamaged` | `true` | `true`, `false`; when off, a weapon at full durability gets no line |
| `colorScheme` | `TIERS` | `TIERS`, `WHITE`, `GRADIENT` |
| `lowDurabilityWarning` | `true` | `true`, `false`; when off, red is shown as orange |
| `warningThresholdPercent` | `15` | `1` to `99`; displayed percent at or below which the line is low |
| `includeAxes` | `true` | `true`, `false` |
| `includeItems` | `[]` | list of item ids or `#tags`; these items always count as weapons |
| `excludeItems` | `[]` | list of item ids or `#tags`; these items never get the line |

`includeItems` and `excludeItems` exist for modded weapons that carry none of the standard tags (or modded items you do not want a line on). Write item ids as `"examplemod:ruby_sword"` and item tags with a leading `#`, as `"#examplemod:swords"`. A missing namespace means `minecraft`, case is ignored, and invalid entries are ignored. `excludeItems` wins over `includeItems` and over every tag. An included item still needs durability: an unbreakable or non-damageable item never gets a line. Changes apply while the game runs.

The remaining durability is always shown, so the line never reads as just a percent. An existing 0.1.0 config file keeps its armor values; NeoForge adds the new section and logs its normal `Correcting` line (checked on Minecraft 26.2).

### Weapon tooltip limitations

- The text exists in English only; other languages fall back to it.
- A datapack item that hides only its damage component from the tooltip still gets the line, because the mod does not read the tooltip display data (it exists only on 26.2 and 26.3).
- A modded weapon that is in none of the standard tags and has neither spear component gets no line until you add it to `includeItems`.
- The creative menu hands out undamaged items, so every weapon there reads 100% unless `showUndamaged` is off.
- Other tooltip mods build tooltips through the same path, so the line can also appear there; its position relative to other mods' lines is not controlled.
- Verified by a headless probe (2026-10-11): on a real dedicated dev server for each of Minecraft 26.1.2, 26.2 and 26.3 (the mod loaded on the server for the probe only, a NeoForge fake player, the real `ItemStack.getTooltipLines` firing the real `ItemTooltipEvent` into the real listener), 60 of 60 checks passed on each version with no mod errors. It covered the mapping from real stacks over the whole item registry (the same 25 damageable vanilla weapons on all three versions, no misclassification), the line text, tier colours and boundaries, the insertion under the item name, each weapon type, the items that get no line (pickaxe, shovel, hoe, armor, fishing rod, flint and steel, shears, shield, elytra, unbreakable item, empty stack), the skip for advanced tooltips (exactly one vanilla durability line) and for a missing player, and the effect of the `[weaponTooltip]` settings.
- Seen on screen: the green undamaged line under the name for a Trident on Minecraft 26.1.2 and a Netherite Axe on 26.3, and a user report for 26.2. The probe does not prove rendering on screen or GUI hover behaviour beyond that.
- Not checked: the on-screen colours of the damaged tiers, the creative menu search itself, the section in the config screen, config changes while the game runs (reload from the file), a vanilla server with the tooltip, and the config upgrade from a real 0.1.0 file on 26.1.2 and 26.3 (the upgrade kept the 12 armor keys on 26.2).

## Development

The project can live in any directory; the build does not depend on an absolute path. Commands below are run from the repository root.

### Prerequisites

- JDK 25.
- Network access on first build. Gradle downloads a JDK 21 through the Foojay toolchain resolver, because ModDevGradle's asset download task needs a Java 21 launcher.
- The Gradle wrapper (9.8.1, checksum pinned) is committed; do not use a locally installed Gradle. Kotlin Gradle Plugin 2.4.21 documents support only up to Gradle 9.7.0, so 9.8.1 is outside its documented range; it builds and tests all three targets in CI.
- Kotlin 2.4.21, with KotlinLangForge as the language provider, and JUnit Jupiter 6.1.3 for the unit tests.

### Commands

Run from the repository root. On Windows use `gradlew.bat`; on Linux and macOS use `./gradlew`.

```powershell
.\gradlew.bat build                            # compile, check and package for the default target (26.1.2)
.\gradlew.bat build "-PmcVersion=26.2"         # same, for another target
.\gradlew.bat :common:test                     # unit tests of the pure module
.\gradlew.bat :common:test --rerun-tasks       # force a real re-run instead of an up-to-date result
.\gradlew.bat check "-PmcVersion=26.2"         # Spotless (ktlint) check + jar metadata verification + tests + verifyTestResults
.\gradlew.bat spotlessApply                    # format Kotlin sources and Gradle scripts
.\gradlew.bat runClient "-PmcVersion=26.2"     # start a dev client with the mod loaded
.\gradlew.bat runClient "-PquickPlay=TestWorld"  # dev only: join the save neoforge/run/saves/TestWorld on launch
.\gradlew.bat buildAll                         # build 26.1.2, 26.2 and 26.3 in order
```

> **PowerShell quoting:** always quote `-PmcVersion`. Unquoted, PowerShell splits `-PmcVersion=26.2` at the dot and Gradle receives `26`.

Valid `mcVersion` values are the file names in `gradle/mc/` (`26.1.2`, `26.2`, `26.3`). Any other value fails fast with the list of available versions. Jars are written to `neoforge/build/libs/armor-status-neoforge-<version>+mc<minecraft>.jar`, for example `armor-status-neoforge-0.3.0+mc26.2.jar`.

### Module layout

| Path | Contents |
|---|---|
| `common/` | Pure Kotlin with no Minecraft dependency: durability model, text and colour (including the shared colour tiers), layout, settings, visibility rule, weapon classification and the tooltip decision, error recovery. Unit tested. |
| `neoforge/` | NeoForge adapter: `@Mod` entrypoint, `ModConfigSpec` binding, config screen, GUI layer, `HudCanvas`, and the weapon tooltip listener. |
| `neoforge/src/v26_1`, `neoforge/src/v26_2` | Per-version overlay with the only code that differs: the F1 "hide GUI" check. 26.2 and 26.3 share `v26_2`. |
| `gradle/mc/` | One properties file per Minecraft version: NeoForge build and Minecraft range. |

### Architecture

All decisions that can be expressed without Minecraft types live in `common` and are plain functions over immutable data. The `neoforge` module only reads game state, calls those functions, and draws through a small `HudCanvas` adapter.

The HUD runs every frame, so the render path uses indexed loops over fixed arrays, no collection pipelines or per-frame allocations, and caches the layout and the durability strings until their inputs change. Settings are an immutable snapshot swapped atomically when the config file loads.

The weapon tooltip follows the same split. The adapter only reads the stack (tag membership and data components) into a small bitmask, `WeaponSignals`; `classifyWeapon` in `common` decides whether it is a weapon, and `tooltipDurability` decides the line. A listener on NeoForge's `ItemTooltipEvent` inserts it at index 1. It keeps no cache and no state besides a one-shot failure flag, because the tooltip is rebuilt every frame while hovering and the tag sets are rebound on datapack reload. The listener needs no per-version overlay: the event is the same on all three targets.

### Tests

`common` has JUnit Jupiter 6.1.3 and kotlin-test unit tests covering durability reading, text and colour tiers, layout for every anchor, settings validation, the visibility rule, caches, error recovery, weapon classification and the tooltip decision. Run them with `.\gradlew.bat :common:test`.

The `verifyTestResults` task runs after `test` and is part of `check`. It fails the build if no test ran, a test was skipped, failed or errored, or a `*Test.kt` class produced no results (a filtered `--tests` run skips only that last check). `verifyModMetadata` also fails if the mod jar contains `org/junit/` or `kotlin/test/` entries. CI uploads the test reports on every run and prints a summary.

NeoForge-bound code, including the tooltip listener and the stack-to-signals mapping, has no unit tests. It is verified by compilation on all three targets, by the metadata check on the built jar, and by dev-client runs. Unit tests are not evidence of in-game behaviour.

## Known limitations

- In-game checks so far cover only the default look and the F3 compact layout, on Minecraft 26.1.2, 26.2 and 26.3. Damaged armor, GUI scales, the other anchors, the `HIDE` and `KEEP` F3 modes and spectator mode have not been checked on screen.
- The config screen shows enum values by their raw names (for example `MIDDLE_LEFT`), not translated labels.
- Joining a vanilla Minecraft 26.2 server with the mod installed works (checked in the logs). Vanilla 26.1.2 and 26.3 servers have not been tried, and the HUD was not checked on screen on a vanilla server.
- The weapon tooltip logic was verified by a headless probe on all three versions, but it was seen on screen only as the green undamaged line on a Trident (26.1.2) and a Netherite Axe (26.3), plus a user report for 26.2 (see [Weapon tooltip limitations](#weapon-tooltip-limitations)). The on-screen colours of the damaged tiers, the creative search itself, the config screen section, live config changes and a vanilla server with the tooltip have not been checked.
- Like the original mod, there is no offhand handling and no special handling for elytra beyond the chest slot.
- The config screen has not been opened in a recorded test.

## Credits

Inspired by Armor Status by ShirjeelPlays. See [ATTRIBUTION.md](ATTRIBUTION.md) for details and for the licence situation of the original project.

## License

[MIT](LICENSE). KotlinLangForge is a separate GPL-3.0 runtime dependency and is not bundled with this mod.
