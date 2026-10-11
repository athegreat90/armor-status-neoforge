# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project intends to follow [Semantic Versioning](https://semver.org/spec/v2.0.0.html) once it is released.

## [Unreleased]

## [0.2.0] - 2026-10-11

Adds the weapon durability hover tooltip. The armor HUD is unchanged.

### Added

- Weapon durability line in the item hover tooltip (inventory, chests, creative menu and other container screens), for example `Durability: 1480 / 1561 (95%)`, right under the item name. Weapons are swords, axes, spears, the mace, the trident, the bow, the crossbow and modded items that use the standard weapon tags or the spear components. The line is skipped for advanced tooltips (F3+H) and for the creative search index. It is client side only and adds no networking.
- New `[weaponTooltip]` config section: `enabled`, `showMax`, `showPercent`, `showUndamaged`, `colorScheme` (`TIERS`, `WHITE`, `GRADIENT`), `lowDurabilityWarning`, `warningThresholdPercent` (`1` to `99`, default `15`) and `includeAxes`. Existing armor keys and defaults are unchanged.
- Colour tiers for the tooltip line: green above 50%, yellow 26 to 50%, orange down to the warning threshold, red at or below it.
- Shared durability helpers in `common` (`percentRounded`, `isLow`, `ColorScheme`), the weapon classification (`classifyWeapon`) and the tooltip decision (`tooltipDurability`), with unit tests (533 in total).

### Changed

- Gradle Wrapper updated from 9.2.1 to 9.8.1.
- Test run is guarded by a `verifyTestResults` check (no undiscovered, skipped or missing test classes) and CI always uploads the test reports with a summary.
- The mod jar check rejects JUnit and `kotlin-test` entries, `common` depends on the `junit-jupiter` aggregator (BOM 6.1.3), archives are built reproducibly, and CI validates the Gradle wrapper.

### Known issues

- Verified by a headless probe on a real dedicated dev server for each of Minecraft 26.1.2, 26.2 and 26.3 (2026-10-11; the mod was loaded on the server for the probe only, with a fake player and the real `ItemStack.getTooltipLines` firing the real `ItemTooltipEvent` into the real listener): 60 of 60 checks passed on each version with no mod errors. That covers the stack-to-weapon mapping over the whole item registry (the same 25 damageable vanilla weapons on all three versions, no misclassification), the line text, the colour tiers and their boundaries, the position under the item name, the other weapon types, the items that get no line, the skips for advanced tooltips and for a missing player, and the effect of the `[weaponTooltip]` settings.
- Seen on screen so far: the green, undamaged line on a Trident in the creative menu (26.1.2) and on a Netherite Axe (26.3), plus a user report for 26.2. Not checked on screen: the colours of the damaged tiers, GUI hover behaviour beyond those observations, the creative menu search itself, the `[weaponTooltip]` section in the config screen, config changes while the game runs (reload from the file), and a vanilla server with the tooltip. The config upgrade from a real 0.1.0 file was checked on 26.2 only, not on 26.1.2 or 26.3.
- Tooltip text is English only. A datapack item that hides only its damage component still gets the line, and a modded weapon without the standard tags or spear components gets none.

## [0.1.0] - 2026-10-11

First release. An unofficial, clean-room NeoForge reimplementation of the idea behind Armor Status by ShirjeelPlays.

### Added

- Original pixel-art chestplate and durability-meter mod icon, with editable SVG source and a 512x512 PNG displayed without blur in the mod list.
- Armor Status HUD layer for NeoForge: item icon and remaining durability for head, chest, legs and feet, in rows at one of six anchors, with scale 0.5 to 2.0.
- `RAW` and `PERCENT` display modes and optional green-to-red gradient colours (off by default).
- Visibility rule: hidden when disabled, without a player, while spectating, or when the HUD is hidden with F1.
- While the F3 debug screen is open the HUD stays visible but compact, so it covers less of the debug text. New settings `debugScreenMode` (`COMPACT` by default, or `HIDE`, `KEEP`) and `debugScreenScale` (`0.6` by default, `0.25` to `1.0`), the extra scale factor used in `COMPACT` mode. In `COMPACT` mode the HUD also moves to `debugScreenAnchor` (`BOTTOM_RIGHT` by default, any of the six anchors) so it is not hidden under the F3 text; with F3 closed `position` applies again.
- Client configuration file `armorstatusneoforge-client.toml` and an in-game configuration screen through the NeoForge `ConfigurationScreen`.
- Pure `common` module with the domain logic (durability, text, colour, layout, settings, caches, error recovery) and unit tests.
- Support for Minecraft 26.1.2 (NeoForge 26.1.2.115), 26.2 (NeoForge 26.2.0.89) and 26.3 (NeoForge 26.3.0.64-beta), one jar per version.
- KotlinLangForge as the language provider (`modLoader = "klf"`).
- Build tooling: Gradle wrapper 9.2.1 with a pinned checksum, Spotless with ktlint, a `verifyModMetadata` check on the built jar, and a `buildAll` task.
- Project documentation: README, ATTRIBUTION, CONTRIBUTING, CLAUDE.md and MEMORY.md.

### Known issues

- Checked in game on Minecraft 26.1.2, 26.2 and 26.3, but only the default layout and the F3 compact layout. Damaged armor, GUI scales, the other anchors, `HIDE`/`KEEP` and spectator mode have not been checked on screen.
- The configuration screen has not been opened in a recorded test, and it shows enum values by their raw names (for example `MIDDLE_LEFT`).
- Joining a vanilla Minecraft 26.2 server with the mod installed works (checked in the logs). Vanilla 26.1.2 and 26.3 servers have not been tried, and the HUD was not checked on screen on a vanilla server.
- Minecraft 26.3 builds against a NeoForge beta, so that file is published on the beta channel.
