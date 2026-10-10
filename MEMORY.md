# MEMORY - Armor Status NeoForge

Version-controlled project memory. Update at each milestone. Every result below was observed by running the stated command on the stated date. No tokens or secrets belong here.

## Project overview

- Client-only NeoForge mod in Kotlin that shows worn armor durability (item icon + number) on the HUD. Targets Minecraft 26.1.2, 26.2 and 26.3, Java 25, language provider KotlinLangForge (KLF).
- Unofficial, clean-room reimplementation inspired by ShirjeelPlays' Fabric mod Armor Status. Our code is MIT. See `ATTRIBUTION.md`.
- Mod id `armorstatusneoforge`, group `io.github.athegreat90`, version `0.1.0`, author `athegreat90`.
- Status on 2026-10-10: tasks T1 to T10 are done and merged to `main` (PR #1, merge commit `3c2e81e`). The private repository `athegreat90/armor-status-neoforge` exists and CI is green on `main`. No release, tag or Modrinth project exists; T11 (going public and publishing) awaits explicit user approval.

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

Documented mutable-state exceptions: `ActiveHudSettings` (`@Volatile` holder) and the render-thread caches in `ArmorStatusLayer` (`LayoutCache`, `DurabilityTextCache`, row arrays).

Open decision: `isFatal()` treats `LinkageError` as fatal, so a jar loaded on the wrong Minecraft version would crash instead of disabling the HUD. Kept as specified; the `minecraft` version range already blocks wrong-version loads. Revisit only if the user asks.

## Kotlin conventions

Summary (details in `.claude/rules/kotlin.md`, condensed in `CLAUDE.md`): immutable data, sealed types, pure functions, no `!!`, scope functions at most one level deep, no coroutines, `runCatching` only at recoverable boundaries via `loadOrDefault` and never in the render path, `isFatal()` for cancellation, interruption, VM errors and `LinkageError`, indexed loops and caches on the HUD hot path, ktlint-official formatting at 120 columns.

## Dependency matrix

| | 26.1.2 | 26.2 | 26.3 |
|---|---|---|---|
| NeoForge (dev build) | 26.1.2.115 | 26.2.0.89 | 26.3.0.64-beta |
| FML (seen in `runClient`) | 11.0.15 | 11.0.16 | 12.0.9 |
| `minecraft_version_range` | `[26.1.2]` | `[26.2]` | `[26.3]` |
| Source overlay | `v26_1` | `v26_2` | `v26_2` |

| Component | Version |
|---|---|
| Kotlin / Kotlin Gradle Plugin | 2.4.21 |
| KotlinLangForge | `2.15.0-k2.4.21-3.1+neoforge` (repo `https://repo.nyon.dev/releases`) |
| ModDevGradle (`net.neoforged.moddev`) | 2.0.148 |
| Gradle wrapper | 9.2.1 (checksum pinned; KGP 2.4.21 documents support up to 9.7.0) |
| Java | 25 (JDK 21 auto-downloaded for MDG asset tasks) |
| Spotless plugin / ktlint | 8.10.4 / 1.8.0 |
| JUnit BOM | 6.1.3 (plus `kotlin-test` 2.4.21) |
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

## Progress

- 2026-10-10: T1 scaffold, T2 spike (KLF loads on all three), T3 `common` domain, T4 client config, T5 HUD layer, T6 three-version runs, T7 Spotless/ktlint + metadata verification + wrapper checksum: done locally. Native reviews approved with only advisory findings.
- T8 documentation: README, ATTRIBUTION, LICENSE, CHANGELOG, CONTRIBUTING, CLAUDE.md, `.claude/rules/*` and this file are written.
- 2026-10-10 F3 follow-up: the default left anchor covered the F3 debug text. First attempt (`hideWhenDebugScreen`, hide the HUD) was replaced after the user's in-game test by `debugScreenMode` (`COMPACT` default, `HIDE`, `KEEP`) and `debugScreenScale` (`0.6`, range `0.25` to `1.0`). While F3 is open in `COMPACT` the HUD is drawn at `scale * debugScreenScale` (icons, text and row gaps shrink, anchor unchanged). Pure rules in `common` (`shouldRender(..., debugScreenOpen)` hides only in `HIDE`; `HudSettings.effectiveScale(debugScreenOpen)`), detection in `neoforge/.../hud/DebugScreen.kt`. Compiled on the three targets. The user's in-game check led to the position follow-up below.
- 2026-10-10 F3 position follow-up: the compact rows still sat at the normal anchor, under the left F3 text column. New setting `debugScreenAnchor` (`HudAnchor`, default `BOTTOM_RIGHT`) is used only in `COMPACT` mode while F3 is open; `HIDE`/`KEEP` ignore it and with F3 closed `position` applies. Pure rule in `common`: `HudSettings.effectiveAnchor(debugScreenOpen)`; `ArmorStatusLayer` computes it once per frame next to `effectiveScale` and passes it to `LayoutCache` (the key already includes the anchor). Compiled on the three targets. The user confirmed it in game on 26.2 (see "Manual verification").
- T9 CI and private remote: done; CI green on PR #1 and on `main`.
- T10 release workflow and Minotaur dry run: written and dry-run locally; the workflow itself has not run on GitHub.
- PR #1 merged (merge commit `3c2e81e`, 2026-10-10); CI on `main` green on all three targets (run 38064670429). The dev server used for testing was stopped.
- T11: stop and wait for explicit user approval before going public, tagging or publishing. A draft Modrinth summary and description were written for the user; nothing was published.

## Test results

All on native Windows 11, JDK 25.0.2, 2026-10-10.

| Check | Command | Outcome |
|---|---|---|
| Unit tests | `.\gradlew.bat :common:test --rerun-tasks` | 313 tests, 0 skipped, 0 failures (XML sum, 2026-10-10 after the F3 position follow-up) |
| Format + metadata + build | `.\gradlew.bat check "-PmcVersion=<v>"` and `build` for 26.1.2, 26.2, 26.3 | BUILD SUCCESSFUL each, no Kotlin warnings |
| Dev client | `runClient` on 26.1.2, 26.2, 26.3 with a test datapack and a save loaded through `quickPlay` | Render path reached, 0 `ERROR`/`Exception` lines, live config edit reloaded |
| Wrapper checksum | Fresh `GRADLE_USER_HOME` with the real and a wrong `distributionSha256Sum` | Right sum accepted, wrong sum rejected |
| `verifyModMetadata` negative demos | Deliberately broken template and constants | Exit 1 listing each wrong loader, side, id, license, range and unexpanded placeholder |
| `buildAll` failure path | Simulated compile error in an overlay | `buildAll` exits 1 and does not report success |
| Dedicated server | `runServer` on 26.1.2 | Reached `Done`; mod not instantiated |

F3 compact follow-up smoke (2026-10-10): `runClient "-PmcVersion=26.2" "-PquickPlay=TestWorld262"` reached the world. The stale `hideWhenDebugScreen` key in the local config was dropped by NeoForge (logged `Configuration file ... is not correct. Correcting`, no error) and the file gained `debugScreenMode = "COMPACT"` and `debugScreenScale = 0.6`. The settings log line shows both fields, live edits to `HIDE`, `KEEP` (restart) and back to `COMPACT`/`0.6` reloaded as logged, 0 `ERROR`/`Exception` lines. F3 itself was not pressed (compact behaviour UNVERIFIED on screen).

F3 position follow-up smoke (2026-10-10): `runClient "-PmcVersion=26.2" "-PquickPlay=TestWorld262"` reached the world (`Dev joined the game`). The settings log line shows `debugScreenAnchor=BOTTOM_RIGHT`, the toml gained `debugScreenAnchor = "BOTTOM_RIGHT"`, a live edit to `TOP_RIGHT` and back reloaded as logged, 0 `ERROR`/`Exception`/`HUD failed` lines. F3 was not pressed (placement UNVERIFIED on screen).

Detail of the render-path smoke: probe logs `SMOKE first draw rows=4` on 26.1.2 and 26.2 and `rows=3` (chest hidden, right-aligned) on 26.3; the probe was removed afterwards. Config corruption on 26.1.2 was corrected by NeoForge or recreated with defaults.

### Manual verification (2026-10-10, Minecraft 26.2 only)

The user ran the dev client against the NeoForge dev server (`runServer` and `runClient "-PmcVersion=26.2"`) and confirmed the HUD in game ("It's perfect"). For the F3 behaviour the user checked two screenshots: the compact HUD at `MIDDLE_LEFT` sat under the F3 text (which led to `debugScreenAnchor`), and with the default `BOTTOM_RIGHT` the F3 text is fully readable and the compact armor stack sits in the bottom-right corner.

**Not verified:** the same checks on 26.1.2 and 26.3 (compile and log smoke only); GUI scales 1 to 4, spectator, all six anchors and `debugScreenMode = HIDE`/`KEEP` on screen (no recorded check); the config screen (no recorded check); joining a vanilla (non-NeoForge) server with the mod installed.

## Known issues

- Visual checks are done on 26.2 only (default settings and the F3 compact layout). The rest of the manual checklist is pending, see "Manual verification".
- 26.3 depends on a NeoForge beta; no stable 26.3 build exists.
- Config screen shows raw enum names.
- Joining a vanilla server is untested; client-only behaviour relies on `dist = CLIENT` and `side = "CLIENT"`.
- No offhand handling and no special elytra handling, as upstream.
- Original mod icon added (`artwork/icon.svg`, `neoforge/src/main/resources/icon.png`); in-game mod-list appearance has not been verified.
- ktlint 1.8.0 embeds Kotlin 2.2.21; a future 2.3/2.4-only syntax could fail to parse. Bump ktlint when a newer one ships; do not suppress.
- Explicit `[[dependencies]]` on `klf` breaks the dev run (`Actual version: [MISSING]`).
- PowerShell splits an unquoted `-PmcVersion=26.2` at the dot; always quote it.
- Upstream licence information is inconsistent (CC0-1.0, MIT, CC-BY-4.0, "no modifying or reuploading"); mitigated by clean-room work.

## CI/CD

- Private repository `athegreat90/armor-status-neoforge` exists (visibility verified `PRIVATE` on 2026-10-10). `main` holds the full history up to the merge of PR #1 (`3c2e81e`); feature branches are merged and no longer needed.
- `ci.yml` exists: push to main, pull requests, manual dispatch; matrix over 26.1.2, 26.2, 26.3 (`fail-fast: false`); Temurin 25; `spotlessCheck`, `:common:test`, `check build "-PmcVersion=<v>"`, jar upload (test reports on failure); actions pinned by full SHA (checkout v7.0.1, setup-java v6.0.1, gradle/actions v6.4.0, upload-artifact v7.0.2); `permissions: contents: read`; concurrency cancel.
- First run on PR #1 (run 38059858897, 2026-10-10): all three jobs green, jars uploaded (about 53 KB each, nothing shaded). This confirmed Temurin 25 resolves on `ubuntu-latest` and that the Foojay JDK 21 download works on the runner. The `gradlew` executable bit had to be set in the git index (`100755`) because it is committed from Windows.
- Runner annotations to act on later: Gradle 9.2.1 is flagged "out of date" and `ubuntu-latest` migrates to Ubuntu 26 on 2026-10-19.
- CI on `main` after the merge (run 38064670429, `3c2e81e`): all three jobs green.
- `release.yml` is on `main` now but has NEVER run. Triggers: tag `v*.*.*` (semver regex, must equal `mod_version`, commit must be on the default branch, CHANGELOG section required, no existing GitHub Release) and `workflow_dispatch` (always a dry run). Jobs: `validate`, `build` matrix, `github-release` (only job with `contents: write`), `modrinth` matrix, `summary`. Modrinth upload through Minotaur 2.10.0 (`environment = client_only`, required dependency `kotlin-lang-forge`, version `<mod_version>+mc<mc>`, `release` for 26.1.2 and 26.2, `beta` for 26.3 via `modrinth_version_type` in `gradle/mc/*.properties`). Duplicate Modrinth versions fail the job. Third-party actions pinned by full SHA (adds softprops/action-gh-release v3.0.3 and download-artifact v8.0.2).
- Local proof (2026-10-10): `.\gradlew.bat modrinth "-PmcVersion=26.3" "-PmodrinthDryRun=true"` succeeds without a token and prints the planned payload; Minotaur debug mode makes two read-only GETs to api.modrinth.com. A real upload requires `MODRINTH_TOKEN` in the environment, `MODRINTH_PROJECT_ID` and `-PmodrinthDryRun=false`.
- First exercise to schedule (needs the user's go-ahead): run `release.yml` through `workflow_dispatch` (dry run). Before a real tag, add a `[0.1.0]` section to `CHANGELOG.md`.
- Needed from the user: Modrinth project, repository secret `MODRINTH_TOKEN`, repository variable `MODRINTH_PROJECT_ID`. Neither exists. Never store them here.

## Local development

- Windows path `C:\Users\alexa\Documents\GitRepo\armor-status-neoforge`; WSL path `/mnt/c/Users/alexa/Documents/GitRepo/armor-status-neoforge`.
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

- Default target when `-PmcVersion` is omitted: 26.1.2. Jars: `neoforge/build/libs/armor-status-neoforge-0.1.0+mc<mc>.jar`.
- `neoforge/run/` is gitignored and holds dev saves, the test datapack and the generated config.
- Dev client and server JVMs started for tests must be stopped by PID; Gradle daemons may stay running.

## Next steps

1. T11: stop and ask for explicit approval before each of these: making the repository public, creating the Modrinth project (secret `MODRINTH_TOKEN`, variable `MODRINTH_PROJECT_ID`), adding a `[0.1.0]` section to `CHANGELOG.md`, dry-running `release.yml` with `workflow_dispatch`, and tagging.
2. Verify the original chestplate-and-durability icon in the in-game mod list; metadata uses `iconFile`/`iconBlur` on 26.2 and 26.3 and `logoFile`/`logoBlur` on 26.1.2 (see "Version specifics learned").
3. Finish the manual visual checklist: 26.2 is done for the default layout and F3. Still open on 26.1.2 and 26.3, and on 26.2 for partial and near-broken damage, empty slots, spectator, F1, GUI scales 1 to 4, all six anchors, `debugScreenMode` `HIDE` and `KEEP`, the F3 anchor against a targeted block that lengthens the right column, the config screen, and world join and leave.
4. Join a vanilla server with the mod installed.
5. Decide whether `LinkageError` should stay fatal in `isFatal()` (see "Open decision").
6. Optional extras: low-durability warning, translated enum labels in the config screen, an upstream issue asking the author to clarify the licence (user decides).
