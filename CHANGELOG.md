# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project intends to follow [Semantic Versioning](https://semver.org/spec/v2.0.0.html) once it is released.

## [Unreleased]

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

- Checked in game on Minecraft 26.2 only (default layout and the F3 behaviour). 26.1.2 and 26.3 are built and tested automatically but have not been checked on screen yet.
- The configuration screen has not been opened in a recorded test, and it shows enum values by their raw names (for example `MIDDLE_LEFT`).
- Joining a vanilla server with the mod installed has not been tested.
- Minecraft 26.3 builds against a NeoForge beta, so that file is published on the beta channel.
