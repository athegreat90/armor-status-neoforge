# Feature: armor-status-neoforge

Plan of record: `C:\Users\alexa\.claude\plans\dazzling-splashing-parnas.md` (approved 2026-10-10).

## Objective
Client-only NeoForge port of the Armor Status HUD idea (ShirjeelPlays, Fabric), written in idiomatic Kotlin on KotlinLangForge, for Minecraft 26.1.2 / 26.2 / 26.3, Java 25, with CI and Modrinth release automation.

## Constraints
- Clean-room reimplementation: no upstream code, icon or textures copied. Our code is MIT.
- No public push, no release, no force-push without separate explicit approval.
- Commits: Conventional Commits, no Co-Authored-By / AI attribution.
- Kotlin 2.4.21, ModDevGradle 2.0.148, KLF `2.15.0-k2.4.21-3.1+neoforge`, Gradle wrapper 9.2.1.
- Planning heuristic: ~400 authored changed lines per task (advisory only). Delivery budget: slice into chained PRs above ~400 lines (strategy: `ask-on-risk`, pending).

## Tasks
- [x] T1 Scaffold (commit 59cbe40): dir, git init (done), .gitignore, .editorconfig, wrapper 9.2.1, settings/build scripts, version props, empty `common` + `neoforge`. Accept: `gradlew.bat build` passes for 26.1.2.
- [x] T2 Spike: minimal `@Mod` object + mods.toml + KLF dep; `runClient` on 26.1.2, 26.2, 26.3. Resolves plan §17 unknowns.
- [x] T3 `common` domain: models, durability calc, layout, settings + validation (RED→GREEN). Accept: `:common:test` green.
- [ ] T4 Config: `ModConfigSpec` binding + `ConfigurationScreen` factory + Result-based fallback.
- [ ] T5 HUD renderer + `HudCanvas` adapter + layer registration. Accept: manual checklist on 26.1.2.
- [ ] T6 Port to 26.2 and 26.3; binary-compat check. Accept: 3 builds + 3 smoke tests.
- [ ] T7 Spotless + ktlint, metadata validation task, jar naming.
- [ ] T8 Docs: README, ATTRIBUTION, LICENSE, CHANGELOG, CONTRIBUTING, CLAUDE.md, rules, MEMORY.md.
- [ ] T9 CI workflow; push to PRIVATE remote; confirm Actions green.
- [ ] T10 Release workflow + Minotaur dry-run; licence/attribution review.
- [ ] T11 STOP: ask explicit approval before going public or publishing.

## Progress
- Workspace verified: native Windows 11, PowerShell 7.6.6, Java 25.0.2, Git 2.55, gh as `athegreat90`.
- Repo dir created and `git init -b main` done; local git identity set (athegreat90 / alexander.agudelo@outlook.com). Remote `athegreat90/armor-status-neoforge` confirmed absent (not yet created).
- T1 writer (2026-10-10): Gradle Kotlin DSL skeleton written (root, `common`, `neoforge`, `gradle/mc/*.properties`, wrapper 9.2.1, .gitignore, .editorconfig). All T1 builds pass on 26.1.2, 26.2 and 26.3.
- T1 closed: parent spot check `:common:test --rerun-tasks` exit 0; added `.gitattributes`; committed as `59cbe40` (`chore: scaffold gradle multi-target kotlin project`, 19 files, 650 lines).
- T1 native review: assessed high (`process_boundary`, `buildAll` Exec), consent granted by user, 4-lens review approved, acknowledged and burned (lineage `review-ef54dc2cf78b7862`, base = empty tree, since this was the root commit). Only advisory findings, tracked below as follow-ups.
- Follow-ups from advisory findings (non-blocking): wrapper has no `distributionSha256Sum` (R1-001, fix in T7); `buildAll` failure path (R3); placeholder `Durability.kt` and its single-case test are throwaway, replaced by real tests in T3 (R3 coverage, R2); `MOD_ID` duplicated between `ModConstants.kt` and properties (R2), revisit in T2; `jvmToolchain(25)` repeated (R2).
- T2 writer (2026-10-10): added `neoforge/src/main/templates/META-INF/neoforge.mods.toml` (rendered by `generateModMetadata`, MDK pattern), `ArmorStatusNeoForge.kt` (`@Mod(value = MOD_ID, dist = [Dist.CLIENT]) object`, logs one INFO line plus a throwaway `:common` probe), a `server` run (for the dedicated-server check only) and the metadata task in `neoforge/build.gradle.kts`. `MOD_ID` stays in `ModConstants.kt`; `mod_id` in `gradle.properties` is still a second copy (T7 metadata validation should assert they match).
- T2 environment finding: MDG `downloadAssets` needs a Java 21 toolchain; only JDK 25 is installed, so runs used `-Porg.gradle.java.installations.paths=<VS Code redhat.java JRE 21>`. Machine-specific; CI (T9) needs JDK 21 beside 25 or a toolchain resolver.
- T3 writer (2026-10-10): `:common` domain implemented test-first in packages `model` (ArmorSlot, DurabilityReading + `readDurability`), `display` (DurabilityDisplayMode, `label`, `durabilityColor`, `WHITE`), `layout` (HudAnchor + VerticalAlign, HudLayout.compute/rowY), `config` (HudSettings, `sanitized`, `shows`, MIN/MAX_SCALE), `visibility` (`shouldRender`), `error` (`isFatal`, `rethrowFatal`, `loadOrDefault`). Removed placeholder `Durability.kt`/`DurabilityTest.kt` and the `:common` probe from `ArmorStatusNeoForge.kt`; added `testImplementation(junit-jupiter-params)` (version from the JUnit BOM). T3 checkbox intentionally left unchecked (parent closes it).
- T3 decisions: PERCENT = round half up, but 0% only when remaining == 0 and 100% only when undamaged (clamped to 1..99 otherwise). Gradient stops (fraction -> RGB): 1.0 55FF55, 0.8 AAFF55, 0.6 FFFF55, 0.4 FF5555, 0.2 AA0000, 0.0 AA00AA; NaN treated as full durability. `HudSettings.scale` is `Float` and validated in `init` (illegal states unrepresentable); `sanitized` takes `Double` and coerces. `HudLayout.textX` is the left edge for left anchors and the right edge for right anchors (`textRightAligned`).

## Route declaration
| Task | Route | Trigger evidence |
|---|---|---|
| T1 | delegated direct (one writer) | 2+ non-trivial files |
| T2 | delegated direct (one writer) | needs runClient x3 + reading KLF sources (>5 lookups) |
| T3 | delegated direct (one writer) | 19 non-trivial files |

## Verification evidence
T1 (observed 2026-10-10, native Windows 11, JDK 25.0.2):
- `.\gradlew.bat --version`: Gradle 9.2.1, Kotlin 2.2.20 (Gradle-embedded), Launcher JVM 25.0.2.
- `.\gradlew.bat :common:test`: BUILD SUCCESSFUL; 1 test, 0 skipped, 0 failures (JUnit Platform 6.1.3 BOM + kotlin-test 2.4.21).
- `.\gradlew.bat build -PmcVersion=26.1.2`: BUILD SUCCESSFUL (3m56s, includes first MC/NeoForge decompile + KLF 2.15.0 resolution).
- `.\gradlew.bat build -PmcVersion=26.2`: BUILD SUCCESSFUL (2m32s).
- `.\gradlew.bat build -PmcVersion=26.3` (neo 26.3.0.64-beta): BUILD SUCCESSFUL (3m03s).
- Jars: `neoforge/build/libs/armor-status-neoforge-0.1.0+mc{26.1.2,26.2,26.3}.jar`; each contains `ModConstantsKt.class` and common's `DurabilityKt.class`; no third-party classes shaded.
- `.\gradlew.bat build -PmcVersion=99.9`: fails fast with "Unsupported mcVersion '99.9': gradle/mc/99.9.properties not found (available: [26.1.2, 26.2, 26.3])".
- `.\gradlew.bat buildAll`: BUILD SUCCESSFUL; runs build-mc26.1.2, build-mc26.2, build-mc26.3 in order (second run, tasks up-to-date).
- Plan §17 unknown resolved: Gradle 9.2.1 + MDG 2.0.148 + KGP 2.4.21 + JDK 25 work together for all three targets (compile only; runtime not yet exercised).

T2 (observed 2026-10-10, dev client, native Windows 11, JDK 25.0.2 + JRE 21 for MDG assets):
- `.\gradlew.bat build -PmcVersion={26.1.2,26.2,26.3}`: BUILD SUCCESSFUL each; jar holds `META-INF/neoforge.mods.toml` rendered with `modLoader = "klf"`, `loaderVersion = "[1,)"`, `license = "MIT"`, neoforge range `[26.1.2.115,)` / `[26.2.0.89,)` / `[26.3.0.64-beta,)` and minecraft range `[26.1.2]` / `[26.2]` / `[26.3]`, both `side = "CLIENT"`; classes `ArmorStatusNeoForge`, `ModConstantsKt`, `DurabilityKt` (common) in the jar.
- `.\gradlew.bat runClient -PmcVersion=26.1.2` (FML 11.0.15): Mod List shows `Armor Status NeoForge 0.1.0 (armorstatusneoforge)` and `KotlinLangForge 2.15.0-k2.4.21-3.1+neoforge (klf)`; `[modloading-worker-0/INFO] [io.github.athegreat90.armorstatus.ArmorStatusNeoForge/]: Armor Status NeoForge loaded (Kotlin 2.4.21, common probe 75%)`; reached the title screen/sound engine, no ERROR lines.
- `runClient -PmcVersion=26.2` (FML 11.0.16, NeoForge 26.2.0.89): same Mod List entries (Minecraft 26.2) and the same mod log line; `Sound engine started`; no ERROR lines.
- `runClient -PmcVersion=26.3` (FML 12.0.9, NeoForge 26.3.0.64-beta): same Mod List entries (Minecraft 26.3) and the same mod log line; `Sound engine started`; no ERROR lines.
- Loader range: provider jar manifest `Implementation-Version: 2.15.0`; with `loaderVersion = "[3,)"` FML fails: `Language loader klf version 2.15.0 is incompatible with [3,)` / `needs language provider klf:3 or above to load`. `[1,)` accepted (all runs above); `[2,)` accepted by inference (2.15.0 in range), not run.
- `runServer -PmcVersion=26.1.2` (DEDICATED_SERVER in DEV): Mod List lists both mods, server reached `Done (1.925s)!`, our log line absent (entrypoint not instantiated). Only ERROR was the first-run `NoSuchFileException: server.properties` from vanilla. No client connected (UNVERIFIED: vanilla-client-to-server join).
- Explicit `[[dependencies]]` on `klf` (`versionRange="[2,)"`, `type="required"`): rejected in dev: `Mod ID: 'klf', Requested by: 'armorstatusneoforge', Expected range: '[2,)', Actual version: '[MISSING]'`. Reverted; not needed.
- All processes started by the spike (wrapper, client/server JVMs) were killed by PID; no other java process touched.

- T2 parent spot check: `gradlew.bat build "-PmcVersion=26.2"` exit 0; rendered mods.toml read back from the jar. Windows gotcha: PowerShell splits an unquoted `-PmcVersion=26.2` at the dot (Gradle sees `26`), so docs and CI must quote it: `"-PmcVersion=26.2"`.
- T2 open items: throwaway `:common` probe in `ArmorStatusNeoForge.kt` is removed in T5; vanilla-client join to a server stays UNVERIFIED (needs manual test in Phase 4); `eza` is not installed locally (use Read/Glob tools).

T3 (observed 2026-10-10, native Windows 11, JDK 25.0.2):
- RED model: with a stub `readDurability` (always NotDamageable) and `fraction = 0.0`, `:common:test` ran 16 tests, 11 failed (assertion failures). After fixing two wrong CSV rows in my own fraction test (remaining/max were swapped) and implementing: green.
- RED display, layout, config+visibility+error: before any implementation the test source set failed to compile (`Unresolved reference ...`), i.e. compile-level RED only; no stubs for these groups.
- GREEN: first full run after implementing failed 1 of 223 (`rethrowFatal` test used `assertSame` on `Result`, a value class that boxes); fixed the test to compare by value, then `.\gradlew.bat clean :common:test` BUILD SUCCESSFUL.
- XML results (`common/build/test-results/test`): 223 tests, 0 skipped, 0 failures, 0 errors (ArmorSlot 1, DurabilityReading 15, DurabilityLabel 17, DurabilityColor 15, HudLayout 104, HudAnchor 7, HudSettings 28, Recovery 30, ShouldRender 6).
- `.\gradlew.bat build "-PmcVersion=26.1.2"`: BUILD SUCCESSFUL. `.\gradlew.bat build "-PmcVersion=26.3"`: BUILD SUCCESSFUL.
- Not verified: `build "-PmcVersion=26.2"` (not requested for T3); no runtime check (pure unit tests only).

- T3 closed: commit `43a3ef9` (`feat(common): add pure armor status domain with tests`). Parent spot check: `:common:test --rerun-tasks` exit 0, 223 tests / 0 failures (read from XML); no `!!` in main sources; `Recovery.kt` read back. Writer reported RED as compile-only for display/layout/config/visibility/error groups (assertion-level RED only for model).
- Review T2+T3 (slice base `59cbe40`, 26 files, 1075 lines): assessed medium (`slice_budget_reached`), consent granted, 1-lens (reliability) review approved, acknowledged and burned (lineage `review-2d8f48f5814b61ea`). Reviewed boundary advances to `43a3ef9`. Only advisory finding (doc checkbox wording) already resolved by this entry.
- Branching: T1–T3 landed on local `main` before the feature branch rule was applied; from T4 on, work happens on `feat/armor-status-hud`. Nothing is pushed yet.
- Running authored-lines count (additions+deletions, generated excluded): ~1.8k over T1–T3 (650 + 97 + 1075 approx.). Delivery strategy still `ask-on-risk` (chain choice pending).

## Next step
T4: `ModConfigSpec` client config + `ConfigurationScreen` factory, wired to `HudSettings.sanitized` via `loadOrDefault`.
