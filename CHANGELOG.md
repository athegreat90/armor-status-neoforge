# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project intends to follow [Semantic Versioning](https://semver.org/spec/v2.0.0.html) once it is released.

## [Unreleased]

Nothing has been released yet. The list below describes what exists in the repository.

### Added

- Armor Status HUD layer for NeoForge: item icon and remaining durability for head, chest, legs and feet, in rows at one of six anchors, with scale 0.5 to 2.0.
- `RAW` and `PERCENT` display modes and optional green-to-red gradient colours (off by default).
- Visibility rule: hidden when disabled, without a player, while spectating, or when the HUD is hidden with F1.
- The HUD is hidden while the F3 debug screen is shown, so it does not cover the debug text. New setting `hideWhenDebugScreen` (default `true`); set it to `false` to keep the HUD visible.
- Client configuration file `armorstatusneoforge-client.toml` and an in-game configuration screen through the NeoForge `ConfigurationScreen`.
- Pure `common` module with the domain logic (durability, text, colour, layout, settings, caches, error recovery) and unit tests.
- Support for Minecraft 26.1.2 (NeoForge 26.1.2.115), 26.2 (NeoForge 26.2.0.89) and 26.3 (NeoForge 26.3.0.64-beta), one jar per version.
- KotlinLangForge as the language provider (`modLoader = "klf"`).
- Build tooling: Gradle wrapper 9.2.1 with a pinned checksum, Spotless with ktlint, a `verifyModMetadata` check on the built jar, and a `buildAll` task.
- Project documentation: README, ATTRIBUTION, CONTRIBUTING, CLAUDE.md and MEMORY.md.

### Known issues

- Visual output and the configuration screen have not been verified by hand yet.
- Minecraft 26.3 builds against a NeoForge beta.
