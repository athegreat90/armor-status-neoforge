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
- [ ] T2 Spike: minimal `@Mod` object + mods.toml + KLF dep; `runClient` on 26.1.2, 26.2, 26.3. Resolves plan §17 unknowns.
- [ ] T3 `common` domain: models, durability calc, layout, settings + validation (RED→GREEN). Accept: `:common:test` green.
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

## Route declaration
| Task | Route | Trigger evidence |
|---|---|---|
| T1 | delegated direct (one writer) | 2+ non-trivial files |

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

## Next step
T2 spike: minimal `@Mod` object + `neoforge.mods.toml` + `runClient` on 26.1.2, 26.2, 26.3.
