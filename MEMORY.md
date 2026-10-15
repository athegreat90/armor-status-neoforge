# MEMORY - Armor Status NeoForge

Version-controlled project memory. Update at each milestone. Every result below was observed by running the stated command on the stated date. No tokens or secrets belong here.

## Project overview

- Client-only NeoForge mod in Kotlin that shows worn armor durability (item icon + number) on the HUD and weapon durability in the item hover tooltip. Targets Minecraft 26.1.2, 26.2 and 26.3, Java 25, language provider KotlinLangForge (KLF).
- Unofficial, clean-room reimplementation inspired by ShirjeelPlays' Fabric mod Armor Status. Our code is MIT. See `ATTRIBUTION.md`.
- Mod id `armorstatusneoforge`, group `io.github.athegreat90`, version `0.3.0` in the repository from 2026-10-15 (not tagged or released yet; `0.2.0` is the latest release, `0.1.0` was the first), author `athegreat90`.
- Status on 2026-10-10: tasks T1 to T10 are done and merged to `main` (PR #1, merge commit `3c2e81e`). The repository `athegreat90/armor-status-neoforge` was private at that time and CI was green on `main`. (Later the same day it was made public and `v0.1.0` was released, see the 2026-10-11 notes below and "CI/CD".)
- Status on 2026-10-11: the maintenance and feature work recorded in `odd/tasks/gradle-junit-weapon-tooltip.md` is merged to `main` through PRs #8 to #13 (Gradle 9.8.1, JUnit 6 test hardening, shared durability helpers and weapon domain, weapon tooltip adapter and config, documentation, CI follow-ups). The maintainer approved releasing it as version 0.2.0 (SemVer minor, new feature) on 2026-10-11. The release preparation change (`mod_version=0.2.0`, CHANGELOG `[0.2.0]`, docs) was merged as PR #14 (merge commit `ac74d84`), the tag `v0.2.0` was created on `ac74d84` and `release.yml` published the GitHub Release `v0.2.0` with three jars the same day (see "2026-10-11 release of 0.2.0" in "Progress"). The Modrinth page is still a draft and is the maintainer's step. Weapon tooltip verification is largely automated by a headless probe, see "Test results" and "Known issues".

## Architecture decisions

| Decision | Reason |
|---|---|
| `common` (pure Kotlin, no Minecraft dependency) + `neoforge` (adapter) | Pure logic is unit-testable on plain JUnit; Minecraft types stay in one module. |
| Version overlays `neoforge/src/v26_1` and `v26_2`, not three copied modules | `GuiLayer`, `RegisterGuiLayersEvent`, `GuiGraphicsExtractor`, `Font`, `ItemStack`, `EquipmentSlot` and `Identifier` are identical on the three targets; only the hide-GUI check differs. 26.3 compiles unchanged against `v26_2`. |
| One jar per Minecraft version, exact `minecraft` range in `neoforge.mods.toml` | A binary-compat check across jars is moot; wrong-version loads are blocked by the range. Verified by `runClient` on each version. |
| Layer registered with `registerBelow(VanillaGuiLayers.CHAT, ...)` | Draws above hotbar, bars and scoreboard, below chat, tab list and subtitles. |
| `HudCanvas` as a `@JvmInline value class` | Isolates version-sensitive draw calls without a per-frame allocation. |
| Entrypoint is a `class` with constructor injection | `IEventBus` and `ModContainer` cannot be injected into an `object`. |
| Settings snapshot swap | Immutable `HudSettings` in one `@Volatile` reference; the render path never touches `ModConfigSpec`. |
| `loadOrDefault` is the only `runCatching` | Recoverable boundary only; fatal errors rethrown. Render path guarded by a one-shot `failed` flag. |
| Enum config values shown by literal name | `common` has no Minecraft dependency, so it cannot implement `TranslatableEnum`. |
| Metadata templated from `gradle.properties` and `gradle/mc/*.properties`, verified by `verifyModMetadata` | Catches wrong loader, drifted mod id, stale range, unexpanded placeholders at build time. |
| Spotless applied at the root project only | One `spotlessCheck` for both modules. Detekt not added (the plan notes detekt 1.x does not work on JDK 25; not re-tested). |
| Weapon durability in the hover tooltip only, no weapon rows on the HUD | User decision (2026-10-11). The cursor is locked in normal play, so nothing about weapons is drawn there. The armor HUD is unchanged. Always-on weapon HUD rows are built only if the user asks. |
| `ItemTooltipEvent` listener on `NeoForge.EVENT_BUS`, line inserted at index 1 | Same event and call on 26.1.2, 26.2 and 26.3, so no per-version overlay. Index 1 puts the line right under the item name. |
| Skip rules: advanced tooltips (F3+H) and `event.entity == null` | Vanilla prints its own `Durability: x / y` in advanced mode, so ours would be a second line. A null player means the creative-menu search index is being built for every item; a line then would make every weapon match a search such as "durability". |
| Adapter builds `WeaponSignals` (a `@JvmInline value class` bitmask), `common` decides in `classifyWeapon` | Keeps the decision pure and unit-tested and the adapter free of allocation; the stack-to-signals mapping itself is not unit tested. |
| `DataComponents.WEAPON` is not a signal | Pickaxes, shovels and hoes carry it too. Spears are caught by `KINETIC_WEAPON`/`PIERCING_WEAPON` and the tags. |
| `includeAxes` default `true`, and `AXE_TAG` is a weapon signal while axes are included | Many modded axes are tagged only `#minecraft:axes`; with the option off an axe is excluded unless it is also a sword or spear. |
| No tooltip or classification cache | Tag sets are rebound on datapack reload, the listener can run off the render thread, and a cache would save less than vanilla's own per-frame tooltip allocation. Only state: a one-shot `@Volatile failed` flag. |
| `ItemTooltipEvent.getDisplay()` is not used | It exists only on 26.2 and 26.3. Cost: a datapack item that hides only its damage component still gets the line. |
| Shared helpers `percentRounded()`, `isLow`, `ColorScheme` | One displayed percent drives the text and the colour, so they cannot disagree. The armor HUD keeps its boolean `gradientColors`. |
| `includeItems`/`excludeItems` lists (ids and `#tags`) as two extra `WeaponSignals` bits (2026-10-15) | Covers modded weapons in no standard tag without a hardcoded list and keeps the decision pure: `classifyWeapon` rules are not damageable, then excluded (wins), then included, then the old rules. Parsing is lenient (`parseItemFilter` drops bad entries, so a typo never discards the good ones). The list validator only checks for strings. Tags are resolved per stack (`stack.is(TagKey)`), never cached, because they are rebound on datapack reload. |
| Resolved filters live in `ActiveHudSettings.Snapshot` next to the `HudSettings` | `HudSettings` is in `common` and cannot hold registry keys, so `ResolvedItemFilter` (ids as `Identifier`, tags as `TagKey`) travels with it in one immutable `Snapshot` behind the single `@Volatile` reference. A reader that needs both reads `snapshot` once, so settings and filters always come from the same refresh (a review note of 2026-10-15 found two separate volatile fields could briefly disagree). No new mutable object. |

Documented mutable-state exceptions: `ActiveHudSettings` (`@Volatile` holder of one immutable `Snapshot`: settings plus resolved weapon filters), the render-thread caches in `ArmorStatusLayer` (`LayoutCache`, `DurabilityTextCache`, row arrays) and the one-shot `@Volatile failed` flag in `WeaponTooltip`.

Open decision: `isFatal()` treats `LinkageError` as fatal, so a jar loaded on the wrong Minecraft version would crash instead of disabling the HUD. Kept as specified; the `minecraft` version range already blocks wrong-version loads. Revisit only if the user asks.

## Kotlin conventions

Summary (details in `.claude/rules/kotlin.md`, condensed in `CLAUDE.md`): immutable data, sealed types, pure functions, no `!!`, scope functions at most one level deep, no coroutines, `runCatching` only at recoverable boundaries via `loadOrDefault` and never in the render path, `isFatal()` for cancellation, interruption, VM errors and `LinkageError`, indexed loops and caches on the HUD hot path, ktlint-official formatting at 120 columns.

## Dependency matrix

| | 26.1.2 | 26.2 | 26.3 |
|---|---|---|---|
| NeoForge (dev build) | 26.1.2.115 | 26.2.0.89 | 26.3.0.84 |
| FML (seen in `runClient`) | 11.0.15 | 11.0.16 | 12.0.12 (12.0.9 on the old beta) |
| `minecraft_version_range` | `[26.1.2]` | `[26.2]` | `[26.3]` |
| Source overlay | `v26_1` | `v26_2` | `v26_2` |

| Component | Version |
|---|---|
| Kotlin / Kotlin Gradle Plugin | 2.4.21 |
| KotlinLangForge | `2.15.0-k2.4.21-3.1+neoforge` (repo `https://repo.nyon.dev/releases`) |
| ModDevGradle (`net.neoforged.moddev`) | 2.0.148 |
| Gradle wrapper | previous 9.2.1, selected 9.8.1 (checksum pinned; PR #8, merge commit `68b92a4`). Evidence: scratch trial 2026-10-11 on 26.1.2, 26.2 and 26.3, plus the verification run before the merge and CI green on PR #8 to #11 (commands, hashes and results are in `odd/tasks/gradle-junit-weapon-tooltip.md`). Caveat: KGP 2.4.21 documents support up to Gradle 9.7.0, so 9.8.1 is outside the documented range. Fallback: 9.7.1 (sha256 `acd53f1e...f804d20a`, not run) |
| Java | 25 (JDK 21 auto-downloaded for MDG asset tasks) |
| Spotless plugin / ktlint | 8.10.4 / 1.8.0 |
| JUnit BOM | previous 6.1.3, selected 6.1.3 (no version change; plus `kotlin-test` 2.4.21). `common` now depends on the `junit-jupiter` aggregator explicitly (API, params and engine at the BOM version) instead of `junit-jupiter-params` alone; `junit-platform-launcher` stays `testRuntimeOnly` |
| Foojay toolchain resolver | 1.0.0 |
| `loaderVersion` | `[1,)` (`[3,)` was rejected: KLF reports 2.15.0) |

## Migration notes

Fabric (upstream) to NeoForge (ours):

| Upstream (Fabric) | Ours |
|---|---|
| `HudRenderCallback` | `RegisterGuiLayersEvent` + `GuiLayer.render(GuiGraphicsExtractor, DeltaTracker)` |
| `DrawContext.drawItem` / `drawText` | `GuiGraphicsExtractor` `item` / `text`, plus `pose` for scaling |
| Yarn names | Mojang names; `ResourceLocation` is `Identifier` in 26.x |
| `player.getArmorItems()` | `player.getItemBySlot(EquipmentSlot.*)` |
| Gson JSON config | `ModConfigSpec` + client config (TOML) |
| Mod Menu + `/armorstatus config` | `ConfigurationScreen` through `IConfigScreenFactory` |
| `fabric.mod.json` | `neoforge.mods.toml` with `modLoader = "klf"` |

Version specifics learned: the F3 pane is shown exactly while `Minecraft#debugEntries.isOverlayVisible()` is true (`KeyboardHandler` calls `toggleDebugOverlay()`); the declaration is identical on 26.1.2, 26.2 and 26.3, so it lives in `neoforge/src/main`, not in an overlay. `DebugScreenOverlay#showDebugScreen()` is not used: it is also true for "always on" entries (performance profile FPS line) and copies a list per call. Hide-GUI is `Options#hideGui` on 26.1.x and `gui.hud.isHidden()` on 26.2 and 26.3. Mod-list icon keys: NeoForge 26.2 and 26.3 read `iconFile`/`iconBlur` (and `bannerFile`) and log a startup warning for the deprecated `logoFile` (`LogoFileWarningsHandler`); NeoForge 26.1.2 only knows `logoFile`/`logoBlur`. The keys come from `mod_icon_file_key`/`mod_icon_blur_key` in `gradle/mc/*.properties`, and `verifyModMetadata` rejects `logoFile` on 26.2 and 26.3. Constructor injection needs a class, not an `object`. NeoForge corrects invalid config values itself (clamp numbers, reset bad enums and booleans) before our code reads them.

Weapon tooltip specifics (read from the 26.1.2, 26.2 and 26.3 jars, then compiled on all three): `ItemTooltipEvent` (`net.neoforged.neoforge.event.entity.player`, game bus, not cancelable, mutable `getToolTip()`), `event.flags.isAdvanced`, `event.entity` (nullable), the tags `Tags.Items.MELEE_WEAPON_TOOLS`, `RANGED_WEAPON_TOOLS`, `TOOLS_BOW`, `TOOLS_CROSSBOW`, `ItemTags.SWORDS`, `AXES`, `SPEARS` and the `*_ENCHANTABLE` tags, and `DataComponents.KINETIC_WEAPON`/`PIERCING_WEAPON` are identical on the three targets, so the tooltip needs no overlay. `ItemTooltipEvent.getDisplay()` exists only on 26.2 and 26.3 and is not used. 26.3 renames `SWING_ANIMATION` to `ATTACK_ANIMATION`, which is unused here. `ItemStack.is(TagKey)` throws when tags are unbound, which is one more reason the listener returns early when `event.entity == null`. Vanilla adds its own `Durability: x / y` line only in advanced mode, and the in-game hotbar item-name popup does not build a tooltip, so it is not covered.

### Build migration problems and solutions

- Gradle 9.8.1 wrapper: `tasks.wrapper` in the root `build.gradle.kts` pinned 9.2.1, which would silently downgrade a re-run of the wrapper task, so it now pins 9.8.1, BIN and the checksum; run `wrapper` twice after changing them. The `gradle-wrapper.jar` on `main` already was Gradle's official 9.8.1 wrapper jar (sha256 matched the published checksum).
- KGP 2.4.21 documents Gradle only up to 9.7.0. 9.8.1 passed every check and CI, but it stays outside the documented range; fallback 9.7.1.
- Minotaur 2.10.0 (the newest) prints "Invocation of Task.project at execution time has been deprecated ... fail with an error in Gradle 10" on the `modrinth` dry run. It appears the same on `main` with Gradle 9.2.1, so it is pre-existing and upstream, not caused by the upgrade. The message says it will fail in Gradle 10, so Minotaur needs an upstream fix before that upgrade.
- Jar bytes depend on the compile mode, not only on the build cache: incremental builds and a clean build differ by about 42 bytes (`Intrinsics.checkNotNullExpressionValue` for a Java platform type in `ActiveHudSettings.class`), the same difference seen between CI and a local 26.3 build. Compare jar hashes only after `clean`, and only on one machine.
- A filtered test run (`--tests`) would fail the per-class completeness check of `verifyTestResults`, so that check is skipped when a filter is active (detected through the public start-parameter route; `commandLineIncludePatterns` is internal and did not compile).

## Progress

- 2026-10-10: T1 scaffold, T2 spike (KLF loads on all three), T3 `common` domain, T4 client config, T5 HUD layer, T6 three-version runs, T7 Spotless/ktlint + metadata verification + wrapper checksum: done locally. Native reviews approved with only advisory findings.
- T8 documentation: README, ATTRIBUTION, LICENSE, CHANGELOG, CONTRIBUTING, CLAUDE.md, `.claude/rules/*` and this file are written.
- 2026-10-10 F3 follow-up: the default left anchor covered the F3 debug text. First attempt (`hideWhenDebugScreen`, hide the HUD) was replaced after the user's in-game test by `debugScreenMode` (`COMPACT` default, `HIDE`, `KEEP`) and `debugScreenScale` (`0.6`, range `0.25` to `1.0`). While F3 is open in `COMPACT` the HUD is drawn at `scale * debugScreenScale` (icons, text and row gaps shrink, anchor unchanged). Pure rules in `common` (`shouldRender(..., debugScreenOpen)` hides only in `HIDE`; `HudSettings.effectiveScale(debugScreenOpen)`), detection in `neoforge/.../hud/DebugScreen.kt`. Compiled on the three targets. The user's in-game check led to the position follow-up below.
- 2026-10-10 F3 position follow-up: the compact rows still sat at the normal anchor, under the left F3 text column. New setting `debugScreenAnchor` (`HudAnchor`, default `BOTTOM_RIGHT`) is used only in `COMPACT` mode while F3 is open; `HIDE`/`KEEP` ignore it and with F3 closed `position` applies. Pure rule in `common`: `HudSettings.effectiveAnchor(debugScreenOpen)`; `ArmorStatusLayer` computes it once per frame next to `effectiveScale` and passes it to `LayoutCache` (the key already includes the anchor). Compiled on the three targets. The user confirmed it in game on 26.2 (see "Manual verification").
- T9 CI and private remote: done; CI green on PR #1 and on `main`.
- T10 release workflow and Minotaur dry run: written and dry-run locally; it later ran on GitHub, see the release entries of 2026-10-11 (0.1.0 and 0.2.0).
- PR #1 merged (merge commit `3c2e81e`, 2026-10-10); CI on `main` green on all three targets (run 38064670429). The dev server used for testing was stopped.
- T11: stop and wait for explicit user approval before going public, tagging or publishing. That approval was given on 2026-10-11 (see the next entry); a draft Modrinth summary and description were written for the user.
- 2026-10-11 release of 0.1.0 (user-approved): the history was scanned first (31 commits, no tokens, keys, `.env`, run or log files); the repository was made public (visibility verified `PUBLIC`); tag `v0.1.0` was created on `main` (`fcea2d4`) and `release.yml` ran for real (run 38093814088): Validate, the three builds, GitHub Release, the three Modrinth jobs and Summary all succeeded. The GitHub Release `v0.1.0` is published (not a draft) with three jars (`+mc26.1.2` 66012 B, `+mc26.2` 66100 B, `+mc26.3` 66104 B). The three Modrinth uploads ran without errors, so the token works; the Modrinth versions themselves were not looked at because the project is a draft (the public API answers 404). Completing the Modrinth page (summary, description, license, links, icon, gallery) and submitting it for review is the maintainer's step.
- 2026-10-11 feature `gradle-junit-weapon-tooltip` (task file `odd/tasks/gradle-junit-weapon-tooltip.md`, stacked small PRs to `main`, each with CI green on 26.1.2, 26.2 and 26.3):
  - T0 baseline and tooltip spike (no commit): baseline `check build` per version, then a throwaway listener compiled unchanged on all three.
  - T1 Gradle 9.8.1: PR #8, merge commit `68b92a4`.
  - T2 JUnit 6 hardening (`verifyTestResults`, jar guard, always-uploaded reports, CI test summary): PR #9, merge commit `097bf3a`.
  - T3 and T4 shared durability helpers and the weapon domain in `common`, test-first: PR #10, merge commit `ac4040d`.
  - T5 tooltip adapter and `[weaponTooltip]` config: PR #11, merge commit `d897df2`.
  - T6 verification: now largely automated. A headless probe on a real dedicated dev server for each of 26.1.2, 26.2 and 26.3 passed 60 of 60 checks per version (2026-10-11, see "Test results"); on screen only the green undamaged line was seen. What stays manual or unverified is listed in "Known issues".
  - T7 documentation (README, CHANGELOG `[Unreleased]`, CLAUDE.md, this file, `.claude/rules/*`): PR #12, merge commit `920d959`.
  - CI follow-ups (CI summary counts only `TEST-*.xml`, `verifyModMetadata` opens the jar once, comment on the test guard wiring): PR #13, merge commit `fc3b40f`.
  - T8 (include/exclude item lists, weapon HUD rows, detekt spike, version catalog, configuration cache) is optional and only if asked.
  - Release 0.2.0 preparation (feature document entry T9): `mod_version=0.2.0`, CHANGELOG `[0.2.0] - 2026-10-11`, README, this file and the feature document, PR #14 (merge commit `ac74d84`), released the same day, see the next entry.
- 2026-10-11 release of 0.2.0 (maintainer-approved; the maintainer asked the assistant to execute the release task): PR #14 was merged as `ac74d84` by `athegreat90` after CI was green on the PR and on `main` at that commit. The annotated tag `v0.2.0` was created on `ac74d84` and pushed. `release.yml` ran for real on the tag (run 38139661944): Validate, the three builds (26.1.2, 26.2, 26.3), GitHub Release, the three Modrinth jobs and Summary all succeeded. This was the first real run after the `validate-wrappers` change, and it worked. The GitHub Release `v0.2.0` ("Armor Status NeoForge 0.2.0") is published (not a draft, not a pre-release) with three jars: `armor-status-neoforge-0.2.0+mc26.1.2.jar` 94046 B, `+mc26.2.jar` 94117 B, `+mc26.3.jar` 94123 B; its notes are the `[0.2.0]` changelog section. The Modrinth jobs ran in real mode (`DRY_RUN: false`, token present, `HAS_TOKEN: true`, no `Modrinth DRY RUN` output, and no token value anywhere in the run log, 0 matches). The uploaded Modrinth versions themselves were not looked at because the project is still a draft (the public API answers 404); the maintainer must check them in the Modrinth dashboard (26.3 should be a beta). Unchanged: the Modrinth page is still to be completed and submitted for review by the maintainer; nothing else is released.
- 2026-10-15 stable 26.3 (branch `feat/26.3-stable-and-item-lists`, not pushed): `gradle/mc/26.3.properties` now has `neo_version=26.3.0.84` and `modrinth_version_type=release` (the maven lists stable 26.3.0.76 to 26.3.0.84). Observed: `.\gradlew.bat spotlessCheck check build "-PmcVersion=26.3"` BUILD SUCCESSFUL without any overlay change; `modrinth "-PmcVersion=26.3" "-PmodrinthDryRun=true"` plans `"versionType": "release"`; `runClient "-PmcVersion=26.3" "-PquickPlay=TestWorld263"` loaded the mod on NeoForge 26.3.0.84 (FML 12.0.12), the player joined, 0 `ERROR`/`Exception` lines. The HUD was not looked at on screen this time. The client JVM was stopped by PID.
- 2026-10-15 include/exclude item lists (same branch, second commit): written test-first (RED was a compile error: `parseItemFilter`, `includeItems` and `excludeItems` did not exist). Observed: `.\gradlew.bat spotlessApply :common:test --rerun-tasks` = 571 tests in 17 suites, 0 skipped, 0 failed (533 before plus 38 new); `spotlessCheck check build "-PmcVersion=<v>"` BUILD SUCCESSFUL on 26.3, 26.2 and 26.1.2, no overlay needed; `runClient "-PmcVersion=26.3"` appended `includeItems = []` and `excludeItems = []` to an existing config, and a live edit reloaded with the bad entry dropped, 0 `ERROR`/`Exception` lines. The tooltip hover with a listed item was not looked at on screen.
- 2026-10-15 review follow-ups (same branch): two native reviews (4 lenses each) were approved with only advisory notes. Fixed `cfa3005` (dangling KDoc link, `weaponSignals` filters made required) and a third commit that bundles settings and resolved filters into one immutable `ActiveHudSettings.Snapshot`. Observed after the bundling: `spotlessCheck check build` OK on 26.3, 26.2 and 26.1.2 with 571 tests, 0 skipped, 0 failed; `runClient "-PmcVersion=26.3"` joined a world and a live edit of `excludeItems` reloaded as `ids=[minecraft:iron_sword], tags=[c:tools/bow]`, 0 `ERROR`/`Exception` lines. At the maintainer's request the version was then raised to `0.3.0` (SemVer minor: new backwards-compatible feature, plus the 26.3 stable target) with CHANGELOG `[0.3.0] - 2026-10-15` and README; no tag, release or publish was made, and the date must be changed if the tag is cut later. Not acted on: a helper for the two duplicated list definitions, and adapter-side automated tests (no harness for NeoForge-bound code).

## Test results

All on native Windows 11, JDK 25.0.2, 2026-10-10.

| Check | Command | Outcome |
|---|---|---|
| Unit tests | `.\gradlew.bat :common:test --rerun-tasks` | 313 tests, 0 skipped, 0 failures (XML sum, 2026-10-10 after the F3 position follow-up; history, superseded by the weapon work below) |
| Format + metadata + build | `.\gradlew.bat check "-PmcVersion=<v>"` and `build` for 26.1.2, 26.2, 26.3 | BUILD SUCCESSFUL each, no Kotlin warnings |
| Dev client | `runClient` on 26.1.2, 26.2, 26.3 with a test datapack and a save loaded through `quickPlay` | Render path reached, 0 `ERROR`/`Exception` lines, live config edit reloaded |
| Wrapper checksum | Fresh `GRADLE_USER_HOME` with the real and a wrong `distributionSha256Sum` | Right sum accepted, wrong sum rejected |
| `verifyModMetadata` negative demos | Deliberately broken template and constants | Exit 1 listing each wrong loader, side, id, license, range and unexpanded placeholder |
| `buildAll` failure path | Simulated compile error in an overlay | `buildAll` exits 1 and does not report success |
| Dedicated server | `runServer` on 26.1.2 | Reached `Done`; mod not instantiated |

JUnit hardening (2026-10-11, Gradle 9.8.1, JDK 25.0.2): `common` now depends on the `junit-jupiter` aggregator (BOM 6.1.3; `testRuntimeClasspath` resolves engine, params, API and launcher to 6.1.3, no JUnit 4 or Vintage), `tasks.test` sets `failOnNoDiscoveredTests = true`, `testLogging` and explicit XML/HTML reports, and the new `verifyTestResults` task (finalizer of `test`, part of `check`) fails on zero tests, any skipped test, any failure/error, or a `*Test.kt` without a result file or with 0 tests. Observed (before the weapon tests existed): `spotlessCheck` OK; `:common:test --rerun-tasks` = 313 tests, 0 failures, 0 errors, 0 skipped, 11 suites (XML sum) and `verifyTestResults` ran; negative demos in the working tree (file removed afterwards): a `@Disabled` test made `verifyTestResults` fail with "1 test(s) were skipped", a `*Test.kt` class without `@Test` methods failed with "test class ... has no result file ..." (Gradle itself did not complain), and with the file deleted the build passed again; in a scratch copy, making the jar-entry prefix match `io/github/athegreat90/` made `verifyModMetadata` fail with "jar contains test-framework classes: io/github/athegreat90/"; `check build` for 26.1.2, 26.2 and 26.3 and `buildAll` BUILD SUCCESSFUL with `--warning-mode all` (no warnings printed), no `org/junit` or `kotlin/test` entries in the three jars. CI `ci.yml` now uploads the reports always and writes a totals summary; the awk summary step was run locally against the real XML files (11 suites, 313 tests, 0/0/0) and the workflow parses with snakeyaml 2.6. The new CI steps then ran on GitHub: see the table below.

F3 compact follow-up smoke (2026-10-10): `runClient "-PmcVersion=26.2" "-PquickPlay=TestWorld262"` reached the world. The stale `hideWhenDebugScreen` key in the local config was dropped by NeoForge (logged `Configuration file ... is not correct. Correcting`, no error) and the file gained `debugScreenMode = "COMPACT"` and `debugScreenScale = 0.6`. The settings log line shows both fields, live edits to `HIDE`, `KEEP` (restart) and back to `COMPACT`/`0.6` reloaded as logged, 0 `ERROR`/`Exception` lines. F3 itself was not pressed (compact behaviour UNVERIFIED on screen).

F3 position follow-up smoke (2026-10-10): `runClient "-PmcVersion=26.2" "-PquickPlay=TestWorld262"` reached the world (`Dev joined the game`). The settings log line shows `debugScreenAnchor=BOTTOM_RIGHT`, the toml gained `debugScreenAnchor = "BOTTOM_RIGHT"`, a live edit to `TOP_RIGHT` and back reloaded as logged, 0 `ERROR`/`Exception`/`HUD failed` lines. F3 was not pressed (placement UNVERIFIED on screen).

Detail of the render-path smoke: probe logs `SMOKE first draw rows=4` on 26.1.2 and 26.2 and `rows=3` (chest hidden, right-aligned) on 26.3; the probe was removed afterwards. Config corruption on 26.1.2 was corrected by NeoForge or recreated with defaults.

### Manual verification (2026-10-10, Minecraft 26.2)

The user ran the dev client against the NeoForge dev server (`runServer` and `runClient "-PmcVersion=26.2"`) and confirmed the HUD in game ("It's perfect"). For the F3 behaviour the user checked two screenshots: the compact HUD at `MIDDLE_LEFT` sat under the F3 text (which led to `debugScreenAnchor`), and with the default `BOTTOM_RIGHT` the F3 text is fully readable and the compact armor stack sits in the bottom-right corner.

### Manual verification (2026-10-11, Minecraft 26.1.2 and 26.3)

Same setup on each version: a NeoForge dev server in its own folder (`runServer "-PmcVersion=<v>" "-PserverDir=run/server-<v>"` through the out-of-repo init script, fresh world, because a 26.1.2 server cannot load the 26.2 world) and `runClient "-PmcVersion=<v>" "-PquickJoin=localhost:25565"`. The user equipped armor and took screenshots; they were reviewed afterwards (the user gave no verbal verdict this time).

- 26.1.2 (NeoForge 26.1.2.115), `neoforge/run/screenshots/2026-10-11_00.38.00.png` and `00.38.17.png`: four rows at `MIDDLE_LEFT`, each with an icon and a shadowed number (407, 592, 555, 481, which look like full-durability values), nothing overlapping the vanilla HUD. With F3 open the debug text is readable and the compact stack sits in the bottom-right corner, number left of the icon. Client log: mod loaded, no warnings, no `ERROR`/`Exception`.
- 26.3 (NeoForge 26.3.0.64-beta, FML 12.0.9), `2026-10-11_00.43.12.png` and `00.43.14.png`: the same HUD and the same F3 result. Client log: mod loaded, no `logoFile` warning, no `ERROR`/`Exception`.

### Vanilla server join (2026-10-11, Minecraft 26.2)

Server: the official vanilla 26.2 `server.jar` from Mojang's version manifest (SHA-1 `823e2250d24b3ddac457a60c92a6a941943fcd6a`, checked against the manifest), started with `java -Xmx2G -jar server.jar nogui` on Java 25 in a scratch folder outside the repo, `online-mode=false`. Client: `runClient "-PmcVersion=26.2" "-PquickJoin=localhost:25565"`, the same dev client with the mod loaded.

Result from the logs: the server printed `Dev logged in` and `Dev joined the game` and never mentions NeoForge or Forge; the client log shows the mod loaded, `Connecting to localhost, 25565`, no `ERROR`/`Exception`, and one expected warning (`Ignoring chat session from Dev due to missing Services public key`, a consequence of offline mode). So the client-only declaration (`dist = CLIENT`, `side = "CLIENT"`) does not stop a NeoForge client from joining a vanilla server. The HUD was not checked on screen during this join (no screenshot), and only 26.2 was tried; vanilla 26.1.2 and 26.3 servers were not.

**Not verified:** damaged and near-broken armor, empty slots, GUI scales 1 to 4, spectator, F1, all six anchors and `debugScreenMode = HIDE`/`KEEP` on screen, on any version (no recorded check); the mod-list icon and the config screen (not visible in the screenshots); the HUD on screen while connected to a vanilla server; joining vanilla 26.1.2 and 26.3 servers.

### Gradle 9.8.1, JUnit hardening and weapon tooltip (2026-10-11)

Native Windows 11, JDK 25.0.2, dates 2026-10-11. Every row was observed; details and hashes are in `odd/tasks/gradle-junit-weapon-tooltip.md`.

| Check | Command | Outcome |
|---|---|---|
| Format | `.\gradlew.bat spotlessCheck` | OK at baseline and after T1, T3+T4 and T5 |
| Unit tests | `.\gradlew.bat :common:test --rerun-tasks` | Baseline, T1 and T2: 313 tests in 11 suites, 0 skipped, 0 failed. After T3+T4 and again after T5: 533 tests in 16 suites, 0 skipped, 0 failed (313 existing, unchanged, plus 220 new) |
| Build per version | `.\gradlew.bat check build "-PmcVersion=<v>"` for 26.1.2, 26.2, 26.3 | BUILD SUCCESSFUL on every version at baseline (Gradle 9.2.1), after T1, T2, T3+T4 and T5; no warnings other than Gradle's configuration-cache hint; no `org/junit` or `kotlin/test` entries in the jars |
| All targets | `.\gradlew.bat buildAll` | OK after T1 and T2 |
| Minotaur dry run | `.\gradlew.bat modrinth "-PmcVersion=26.3" "-PmodrinthDryRun=true"` | OK on Gradle 9.8.1 ("Modrinth DRY RUN"), with the upstream `Task.project` deprecation message |
| Reproducible archives | two clean 26.2 rebuilds 4 s apart | identical bytes, also identical to the baseline jar |
| CI per pull request | `ci.yml` on PR #8, #9, #10, #11 | Green on 26.1.2, 26.2 and 26.3 each. PR #8 runner log: "Welcome to Gradle 9.8.1!" and "All Gradle Wrapper jars are valid". PR #9: `verifyTestResults: 313 tests in 11 suites, 0 skipped, 0 failed`, the `Test summary` and `Upload test reports` steps succeeded and the artifacts `reports-mc26.1.2`, `reports-mc26.2`, `reports-mc26.3` exist |
| `verifyTestResults` negative demos | a `@Disabled` test, a `*Test.kt` class without `@Test`, and in a scratch copy a jar-prefix match for the test-framework guard | Each failed with its specific message ("1 test(s) were skipped", "has no result file", "jar contains test-framework classes"); demos removed afterwards, the build passed again. A filtered `--tests "*ShouldRenderTest"` run passed (18 tests) with the per-class check skipped |
| Mutation checks (T3+T4) | `isLow` `<=` changed to `<`; axe policy inverted | 15 and 7 tests failed respectively; both restored and the suite was green again. The axe-signal fix was made test-first (2 tests failed, then passed) |
| Dev client smoke, Gradle 9.8.1 | `runClient "-PmcVersion=26.2" "-PquickPlay=TestWorld262"` | Mod loaded, player joined, no warnings or errors in the log |
| Dev client smoke, config upgrade | same, with a 0.1.0-format config (T5 build) | Mod loaded; NeoForge logged its normal `Configuration file ... is not correct. Correcting` line; the 12 armor keys kept their values and order and the `[weaponTooltip]` section with defaults was added; no `weapon tooltip failed` line; checked on 26.2 only |
| CI for PR #13 (follow-ups) | `ci.yml` on the runner | Green; the `Test summary` step counting only `TEST-*.xml` ran and was confirmed on the GitHub runner |
| Release run for tag `v0.2.0` | `release.yml` on the GitHub runner, run 38139661944 (2026-10-11) | Validate, the three builds, GitHub Release, the three Modrinth jobs and Summary all succeeded (first real run after `validate-wrappers`); Release `v0.2.0` published with three jars (94046, 94117 and 94123 B); Modrinth jobs in real mode, 0 token matches in the log; the uploaded Modrinth versions were not looked at (draft project) |
| Headless tooltip probe, Minecraft 26.1.2 | scratch copy of `main`, real dedicated dev server with the mod loaded for the probe only, a NeoForge fake player, real `ItemStack.getTooltipLines` firing the real `ItemTooltipEvent` into the real listener (2026-10-11) | 60 of 60 checks passed, no mod errors |
| Headless tooltip probe, Minecraft 26.2 | same (2026-10-11) | 60 of 60 checks passed, no mod errors |
| Headless tooltip probe, Minecraft 26.3 | same (2026-10-11) | 60 of 60 checks passed, no mod errors |

In-game tooltip observations (dev server per version, `runClient ... "-PquickJoin=localhost:25565"`, the user driving the client, screenshots read afterwards; all client logs clean, no `ERROR`, `Exception`, `HUD failed` or `weapon tooltip failed`):

- 26.1.2 (NeoForge 26.1.2.115): a Trident in the creative menu shows the green line `Durability: 250 / 250 (100%)` directly under the name; the armor HUD rows are still drawn.
- 26.3 (NeoForge 26.3.0.64-beta): a Netherite Axe shows the green line `Durability: 2031 / 2031 (100%)` (so axes count by default); the armor HUD rows are still drawn.
- 26.2: the user reported seeing the tooltip; there is no screenshot.
- Limits: all observed items were undamaged, so only the green tier was seen on screen; one weapon per version at most; the creative menu hands out undamaged items.

Headless tooltip probe (2026-10-11): run in a scratch copy of `main` on a real dedicated dev server for EACH of Minecraft 26.1.2, 26.2 and 26.3, with the mod loaded on the server for the probe only, a NeoForge fake player, and the real `ItemStack.getTooltipLines` firing the real `ItemTooltipEvent` into the real listener. All 60 checks passed on each version with no mod errors. What it proved:

- The real stack to signals to `classifyWeapon` mapping over the whole item registry: the same 25 damageable vanilla weapons on all three versions (bow, crossbow, trident, mace, and the sword, axe and spear of each existing material among wooden, stone, copper, golden, iron, diamond and netherite) and 59 damageable non-weapons (all pickaxes, shovels, hoes, all armor, brush, carrot on a stick, warped fungus on a stick, elytra, fishing rod, flint and steel, shears, shield). No misclassification, nothing needing review.
- Line text, tier colours and insertion at index 1, on a diamond sword (max 1561): damage 0 `1561 / 1561 (100%)` green `0x55FF55`, 800 `761 / 1561 (49%)` yellow `0xFFFF55`, 1200 `361 / 1561 (23%)` orange `0xFFAA00`, 1400 `161 / 1561 (10%)` red `0xFF5555`, 1560 `1 / 1561 (1%)` red; boundaries 51 green, 50 and 26 yellow, 25 and 16 orange, 15 red. Every other weapon type shows exactly one line at index 1.
- Negatives get no line: pickaxe, shovel, hoe, chestplate, fishing rod, flint and steel, shears, shield, elytra, an unbreakable sword, an empty stack. Advanced tooltips (the real advanced flag) get no line from the mod and exactly one vanilla `item.durability` line. A null player gets no line (the creative search index condition).
- Settings take effect: `enabled` off, `includeAxes` off (removes the axe line, not the sword's), `showUndamaged` off, `showMax` and `showPercent` off (`761`), `WHITE` scheme, `lowDurabilityWarning` off (red becomes orange), `warningThresholdPercent` 30 (the 23 % case turns red).

What the probe does NOT prove: rendering on screen and GUI hover behaviour beyond the earlier observations, the creative-menu search index itself, the config screen section, live reload from the TOML, a vanilla server with the tooltip, and the 26.1.2 and 26.3 config upgrade from a real 0.1.0 file.

## Known issues

- Weapon tooltip, verified by the headless probe on all three versions (2026-10-11, see "Test results"): the stack-to-weapon mapping over the whole registry, line text, tier colours and boundaries, position, the other weapon types, the negatives, the advanced-tooltip and null-player skips, and the settings variations. Still NOT verified: the on-screen colours of the damaged tiers and GUI hover beyond the green undamaged line seen on 26.1.2 and 26.3 (plus the 26.2 report); the creative-menu search itself; the `[weaponTooltip]` section in the config screen; config changes taking effect while the game runs (live reload from the TOML); a vanilla-server join with the tooltip; the config upgrade from a real 0.1.0 file on 26.1.2 and 26.3 (only 26.2 was checked).
- Tooltip text is English only (`en_us.json`); other languages fall back to it.
- A datapack item that hides only its damage component from the tooltip still gets the line, because `getDisplay()` is not read.
- A modded weapon that is in none of the standard tags and has no spear component gets no line until it is added to `includeItems` (lists added 2026-10-15). Verified so far: unit tests, compile on the three targets, and on 26.3 the config upgrade (new keys appended, `[]`) and a live edit reload that parsed `["minecraft:iron_pickaxe", "Not Valid", "#c:tools/pickaxe"]` to one id and one tag. NOT verified: the hover on screen with a listed item, the lists on 26.1.2 and 26.2 at runtime, a real modded weapon, and the config screen widgets for the lists.
- In the creative menu every weapon is undamaged, so each reads 100% unless `showUndamaged` is off. Other tooltip mods can show the line too, and its position relative to their lines is not controlled.
- The Minotaur `Task.project` deprecation (Gradle 10) comes from Minotaur 2.10.0, not from this repository.
- Gradle 9.8.1 is outside KGP 2.4.21's documented range (up to 9.7.0).
- The default layout and the F3 compact layout were checked on screen on 26.1.2, 26.2 and 26.3. The rest of the manual checklist is pending, see "Manual verification".
- 26.3 builds against stable NeoForge 26.3.0.84 since 2026-10-15 (it was `26.3.0.64-beta`); the mods.toml minimum is now 26.3.0.84, so older beta installs must update. The 0.2.0 jar on Modrinth is still the beta-built one.
- Config screen shows raw enum names.
- Joining a vanilla 26.2 server works (logs only); vanilla 26.1.2 and 26.3 servers are untested, and the HUD was not checked on screen on a vanilla server. Client-only behaviour relies on `dist = CLIENT` and `side = "CLIENT"`.
- No offhand handling and no special elytra handling, as upstream.
- Original mod icon added (`artwork/icon.svg`, `neoforge/src/main/resources/icon.png`); in-game mod-list appearance has not been verified.
- ktlint 1.8.0 embeds Kotlin 2.2.21; a future 2.3/2.4-only syntax could fail to parse. Bump ktlint when a newer one ships; do not suppress.
- Explicit `[[dependencies]]` on `klf` breaks the dev run (`Actual version: [MISSING]`).
- PowerShell splits an unquoted `-PmcVersion=26.2` at the dot; always quote it.
- Upstream licence information is inconsistent (CC0-1.0, MIT, CC-BY-4.0, "no modifying or reuploading"); mitigated by clean-room work.

## CI/CD

- Repository `athegreat90/armor-status-neoforge` is public (verified `PUBLIC` on 2026-10-11; it was private before). `main` holds the full history up to the merge of PR #1 (`3c2e81e`) and, since 2026-10-11, PRs #8 to #14 (last merge commit `ac74d84`, the 0.2.0 release preparation; tag `v0.2.0` on it); merged feature branches are no longer needed.
- `ci.yml` exists: push to main, pull requests, manual dispatch; matrix over 26.1.2, 26.2, 26.3 (`fail-fast: false`); Temurin 25; `spotlessCheck`, `:common:test`, `check build "-PmcVersion=<v>"`, jar upload, test summary and test reports (reports always uploaded); actions pinned by full SHA (checkout v7.0.1, setup-java v6.0.1, gradle/actions v6.4.0, upload-artifact v7.0.2); `permissions: contents: read`; concurrency cancel.
- First run on PR #1 (run 38059858897, 2026-10-10): all three jobs green, jars uploaded (about 53 KB each, nothing shaded). This confirmed Temurin 25 resolves on `ubuntu-latest` and that the Foojay JDK 21 download works on the runner. The `gradlew` executable bit had to be set in the git index (`100755`) because it is committed from Windows.
- Runner annotations: Gradle 9.2.1 was flagged "out of date" (addressed by the 9.8.1 upgrade, merged in PR #8) and `ubuntu-latest` migrates to Ubuntu 26 on 2026-10-19.
- Changes of 2026-10-11 (PR #8 and #9, confirmed on the runner): every `Set up Gradle` step has `validate-wrappers: true` (the runner log shows "All Gradle Wrapper jars are valid"); `ci.yml` always uploads the test reports as `reports-mc<version>` and writes a test summary to the job summary (both steps succeeded on PR #9); `verifyTestResults` runs inside `check`. `release.yml` got only `validate-wrappers: true` on its two `Set up Gradle` steps; the real run for tag `v0.2.0` (run 38139661944) was the first after that change and succeeded.
- Review follow-ups, done on 2026-10-11 and merged as PR #13 (`fc3b40f`): the CI summary now counts only `TEST-*.xml` (the same files `verifyTestResults` counts, checked locally with a stray `ignored.xml` that does not change the totals); `verifyModMetadata` opens the jar once and returns the toml text and the first test-framework entry together; `common/build.gradle.kts` explains why `verifyTestResults` is both a finalizer of `test` (direct `:common:test`) and a dependency of `check` (a finalizer only fires when `test` is part of the build, e.g. `check -x test`). The PR #13 CI run confirmed the changed summary step on the GitHub runner.
- CI on `main` after the merge (run 38064670429, `3c2e81e`): all three jobs green.
- `release.yml` has run for real twice (tag `v0.1.0`, run 38093814088, and tag `v0.2.0`, run 38139661944, see "Progress") and as a dry run via `workflow_dispatch`; the `v0.2.0` run was the first after the `validate-wrappers` change and succeeded. Triggers: tag `v*.*.*` (semver regex, must equal `mod_version`, commit must be on the default branch, CHANGELOG section required, no existing GitHub Release) and `workflow_dispatch` (always a dry run). Jobs: `validate`, `build` matrix, `github-release` (only job with `contents: write`), `modrinth` matrix, `summary`. Modrinth upload through Minotaur 2.10.0 (`environment = client_only`, required dependency `kotlin-lang-forge`, version `<mod_version>+mc<mc>`, `release` for 26.1.2 and 26.2, `beta` for 26.3 via `modrinth_version_type` in `gradle/mc/*.properties`). Duplicate Modrinth versions fail the job. Third-party actions pinned by full SHA (adds softprops/action-gh-release v3.0.3 and download-artifact v8.0.2).
- Local proof (2026-10-10): `.\gradlew.bat modrinth "-PmcVersion=26.3" "-PmodrinthDryRun=true"` succeeds without a token and prints the planned payload; Minotaur debug mode makes two read-only GETs to api.modrinth.com. A real upload requires `MODRINTH_TOKEN` in the environment, `MODRINTH_PROJECT_ID` and `-PmodrinthDryRun=false`.
- Release dry run (2026-10-11, run 38091137721, `workflow_dispatch` on `main`): Validate, the three builds, the three Modrinth jobs and Summary succeeded; the GitHub Release job was skipped as designed. Two warnings: the Modrinth project returned HTTP 404 (not public yet), so the duplicate guard could not check `0.1.0+mc<mc>`, and at that time `CHANGELOG.md` had no `[0.1.0]` section (added afterwards, dated 2026-10-11; change the date if the tag is cut later). A dry run never sends the token to Modrinth, so the token's validity is untested.
- Modrinth credentials: the project exists; repository secret `MODRINTH_TOKEN` and repository variable `MODRINTH_PROJECT_ID` are set (checked by name only, 2026-10-11). The project id is not secret; the token must never be a variable and never be stored or printed. A token was once saved as a plain variable by mistake and was revoked and replaced.

## Local development

- Work from the repository root, on native Windows (PowerShell) or WSL; no absolute path is needed.
- Commands (PowerShell, from the repo root; quote `-PmcVersion`):

```powershell
.\gradlew.bat build "-PmcVersion=26.2"
.\gradlew.bat :common:test
.\gradlew.bat check "-PmcVersion=26.2"
.\gradlew.bat spotlessApply
.\gradlew.bat runClient "-PmcVersion=26.2"
.\gradlew.bat runClient "-PmcVersion=26.2" "-PquickPlay=<save under neoforge/run/saves>"
.\gradlew.bat buildAll
```

- Default target when `-PmcVersion` is omitted: 26.1.2. Jars: `neoforge/build/libs/armor-status-neoforge-0.3.0+mc<mc>.jar`.
- `neoforge/run/` is gitignored and holds dev saves, the test datapack and the generated config.
- Dev client and server JVMs started for tests must be stopped by PID; Gradle daemons may stay running.

## Next steps

1. T11 for 0.1.0 and the 0.2.0 release are done with the maintainer's approval (repository public, tags `v0.1.0` and `v0.2.0`, GitHub Releases, Modrinth uploads). Still open for the maintainer: check the uploaded versions of both releases in the Modrinth dashboard (26.3 should show as beta), then complete the Modrinth page (summary, description, links) and submit it for review; see item 7.
2. Verify the original chestplate-and-durability icon in the in-game mod list; metadata uses `iconFile`/`iconBlur` on 26.2 and 26.3 and `logoFile`/`logoBlur` on 26.1.2 (see "Version specifics learned").
3. Finish the manual visual checklist: the default layout and F3 are done on all three versions. Still open on every version: partial and near-broken damage, empty slots, spectator, F1, GUI scales 1 to 4, all six anchors, `debugScreenMode` `HIDE` and `KEEP`, the F3 anchor against a targeted block that lengthens the right column, the config screen, and world join and leave.
4. Optional: look at the HUD on screen while connected to a vanilla server, and join vanilla 26.1.2 and 26.3 servers (a vanilla 26.2 join is done, see "Vanilla server join").
5. Decide whether `LinkageError` should stay fatal in `isFatal()` (see "Open decision").
6. Optional extras: a low-durability warning on the armor HUD (it exists only for the weapon tooltip), translated enum labels in the config screen, an upstream issue asking the author to clarify the licence (user decides).
7. Maintainer: in the Modrinth dashboard check the three uploaded 0.2.0 versions (26.3 was uploaded as a beta; later releases use the release channel), complete the project page with the updated description and submit it for review. The assistant could not check the versions because the project is still a draft.
8. A possible next release (new SemVer version, CHANGELOG section, tag and approval), only when the maintainer asks.
9. Weapon tooltip, remaining manual checks (see "Known issues"): the on-screen colours of a damaged weapon, the creative search itself, the config screen section, live config changes, a vanilla server with the tooltip, and the real 0.1.0 config upgrade on 26.1.2 and 26.3.
10. Include/exclude item lists are done (2026-10-15, see "Known issues" for what was and was not checked). Still only if asked: optional always-on weapon HUD rows behind a toggle. Manual check wanted: put a pickaxe id in `includeItems` and a sword in `excludeItems`, hover them, and open the config screen.
