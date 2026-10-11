# Feature: gradle-junit-weapon-tooltip

Plan of record: the approved session plan, revision 2, of 2026-10-11 (kept outside the repository; its decisions are summarised in this document).

## Objective
Three maintenance/feature items on the existing Armor Status NeoForge mod: (1) upgrade Gradle to the newest stable version verified on all three targets (Minecraft 26.1.2, 26.2, 26.3); (2) harden the JUnit 6 setup (JUnit 6.1.3 is already in place, so no migration); (3) show weapon durability in the item hover tooltip (inventory, chests, creative menu and other container screens), client side only. The armor HUD is not changed.

## Constraints
- Preserve existing armor behaviour, config keys/defaults, registry id `armorstatusneoforge:armor_status`, history, workflows and Modrinth setup. Nothing is released or published; no version bump.
- Weapon durability is informational only (no durability mechanics changed, no new networking, no HUD rows for weapons: the user chose the hover tooltip only on 2026-10-11).
- Commits: Conventional Commits, no Co-Authored-By or AI attribution. No force-push. Push, PR creation and merge stay the user's decisions.
- Kotlin rules in `CLAUDE.md` / `.claude/rules/*`; tests first for pure `common` logic (RED before GREEN); never claim a check passed unless it was run.
- Planning heuristic ~400 authored changed lines per task (advisory). Forecast ~0.8-1.1k lines in total.
- Delivery strategy: **stacked small PRs to `main`** (user-approved in the plan), slices below; one native-review assessment per work-unit commit.

## Tasks
- [x] T0 Baseline and tooltip spike (no commit): `check build` per version, jar SHA-256s, 313 tests; compile a throwaway tooltip listener on all three versions in a scratch copy.
- [x] T1 Gradle 9.8.1 (PR 1, branch `build/gradle-9.8.1`): wrapper twice + checksum, fix `tasks.wrapper` pin and comment, reproducible archives, wrapper validation in CI, docs for commands.
- [ ] T2 JUnit 6 hardening (PR 2): `testLogging`, explicit reports, `verifyTestResults`, jar assertion, CI always uploads reports and writes totals to the job summary.
- [ ] T3 Shared durability helpers (PR 3): public `percentRounded()`, `isLow`, `ColorScheme` (tests first, armor unchanged).
- [ ] T4 Weapon domain in `common` (PR 4, tests first): `WeaponSignals`, `classifyWeapon`, `WeaponTooltipSettings`, `tooltipDurability`, `HudSettings` extension.
- [ ] T5 Adapter and config (PR 5): `WeaponClassifier` + `WeaponTooltip` listener in `neoforge`, registration, `[weaponTooltip]` config section, `en_us.json`.
- [ ] T6 Verification on 26.1.2, 26.2, 26.3 (in-game matrix of the plan, vanilla-server join), fixes, results recorded.
- [ ] T7 Documentation (PR 6): README, CHANGELOG `[Unreleased]`, CLAUDE.md, MEMORY.md, this document.
- [ ] T8 Optional, only if asked: include/exclude item lists, always-on weapon HUD rows, detekt spike, version catalog, configuration-cache fix.

## Authorized scope
Files touched by the tasks above: `gradle/wrapper/*`, `gradlew`, `gradlew.bat`, root/`common`/`neoforge` `build.gradle.kts`, `.github/workflows/ci.yml` and `release.yml` (test/validation steps only), `common/src/**`, `neoforge/src/**` (new tooltip listener, config section, lang file), docs (`README.md`, `CHANGELOG.md`, `CLAUDE.md`, `MEMORY.md`, `odd/tasks/`). Nothing else.

## Acceptance criteria
See plan section 16: Gradle 9.8.1 on JVM 25 with the official checksum; `spotlessCheck`; `:common:test` (313 existing + new, 0 failed, 0 skipped, counts checked by `verifyTestResults`); `check build` for each of 26.1.2/26.2/26.3; `buildAll`; Minotaur dry run; jars with valid metadata, KLF `modLoader = "klf"`, no JUnit entries; CI green on every PR; in-game results recorded with unverified items listed.

## Routes (direct inline vs delegated direct) and trigger evidence
Recorded per task as work proceeds. T0 delegated (several builds, bounded output), T1 delegated writer (wrapper files + 3 build/CI files), T2-T5 delegated writers per slice, T7 inline or delegated by size.

## Progress
- 2026-10-11: plan approved; feature document and Engram mirror (`odd/gradle-junit-weapon-tooltip/tasks`) created before the first source write; branch `build/gradle-9.8.1` created from `main` (`fcea2d4`).

## Verification evidence
- T0 (2026-10-11, delegated worker, repo untouched; route: delegated direct, trigger: several builds and a scratch-copy compile): baseline on `fcea2d4`: Gradle 9.2.1, JVM 25.0.2 GraalVM CE; `spotlessCheck` OK; `:common:test --rerun-tasks` = 313 tests, 0 failures, 0 errors, 0 skipped, 11 suites; `check build` BUILD SUCCESSFUL for 26.1.2 (jar 66012 B, SHA-256 `ab7d8f0f...50aa2`), 26.2 (66100 B, `0170f5bb...fd81a`) and 26.3 (66062 B, `a2bdc375...a143c3`); no `org/junit` or `kotlin/test` entries in any jar; no warnings besides Gradle's configuration-cache hint. Baseline armor HUD screenshots already exist in `neoforge/run/screenshots` (26.2 `2026-10-10_17.31.05.png`, 26.1.2 `2026-10-11_00.38.00.png`/`00.38.17.png`, 26.3 `2026-10-11_00.43.12.png`/`00.43.14.png`).
- T0 tooltip spike: a throwaway listener (`NeoForge.EVENT_BUS.addListener(::onItemTooltip)`, `event.entity`, `event.flags.isAdvanced`, `event.itemStack`, `event.toolTip.add(1, Component.translatable(...).withColor(...))`, tag checks via ``stack.`is`(Tags.Items.MELEE_WEAPON_TOOLS)`` etc., `stack.has(DataComponents.KINETIC_WEAPON)`) compiled unchanged on 26.1.2, 26.2 and 26.3 (`:neoforge:compileKotlin`, no `e:`/`w:` lines). Compile only; nothing was run in a client yet. Imports: `net.minecraft.core.component.DataComponents`, `net.minecraft.network.chat.Component`, `net.minecraft.tags.ItemTags`, `net.neoforged.neoforge.common.NeoForge`, `net.neoforged.neoforge.common.Tags`, `net.neoforged.neoforge.event.entity.player.ItemTooltipEvent`.
- Finding (parent, 2026-10-11): the released 26.3 jar (66104 B) differs from the local 26.3 build (66062 B) only in `ActiveHudSettings.class`: the CI build emits `Intrinsics.checkNotNullExpressionValue(..., "getLogger(...)")` for the Java platform type, the local build does not (annotation visibility on the compile classpath). Harmless, but jar bytes are not identical across machines, so reproducibility is only checked as two builds on one machine.

- T1 (2026-10-11, route: delegated direct writer for 8 files incl. wrapper/CI/docs, then parent checks): `tasks.wrapper` now pins 9.8.1 + BIN + `distributionSha256Sum` (also in `gradle-wrapper.properties`); reproducible-archive block in the root build; `validate-wrappers: true` on every `Set up Gradle` step (ci.yml 1, release.yml 2; the pinned setup-gradle v6.4.0 has that input, default true); Gradle version mentions in README/CLAUDE/MEMORY/CHANGELOG updated (parent restored the released `[0.1.0]` changelog line that the writer had reworded). Worker results: `--version` = Gradle 9.8.1 on JVM 25.0.2 from Git Bash and PowerShell, no `exitWithErrorLevel` message; `spotlessCheck` OK; `:common:test --rerun-tasks` 313/0/0/0; `check build --warning-mode all` OK on 26.1.2 (66012 B), 26.2 (66100 B), 26.3 (66062 B) with the baseline SHA-256s and no warnings except the configuration-cache hint; `buildAll` OK; `modrinth ... -PmodrinthDryRun=true` OK ("Modrinth DRY RUN"); two clean 26.2 rebuilds 4 s apart gave identical bytes (also identical to the baseline). Parent checks: `gradle-wrapper.jar` sha256 `3b8a2577...e98` equals Gradle's official 9.8.1 wrapper checksum (`gradle-9.8.1-wrapper.jar.sha256`; the committed jar on `main` was already that jar, generated by the local 9.8.1 when the distribution was pinned to 9.2.1; `gradlew` mode still 100755); re-ran `check build "-PmcVersion=26.3" --no-build-cache --warning-mode all` = BUILD SUCCESSFUL, jar 66062 B `a2bdc375...` (baseline hash); dev client smoke `runClient "-PmcVersion=26.2" "-PquickPlay=TestWorld262"` on 9.8.1: `Armor Status NeoForge loaded`, settings line, `Dev joined the game`, no warnings/errors in the log, client then stopped by PID.
- T1 findings: (1) the Minotaur dry run prints "Invocation of Task.project at execution time has been deprecated ... fail with an error in Gradle 10"; the same message appears on `main` with Gradle 9.2.1 (parent ran it in a scratch copy of `main`), so it is pre-existing and comes from Minotaur 2.10.0 (newest), not from this upgrade; (2) the shared Gradle build cache can hand back a 26.2 jar of 66058 B (same bytes as a scratch-copy build) instead of the 66100 B fresh compile, the same ~42 B `checkNotNullExpressionValue` difference seen between CI and local 26.3: compile output depends on something outside the cache key, so compare jar hashes only with `--no-build-cache` or a clean build; (3) CI behaviour of `validate-wrappers` is unverified until the PR runs.

## Next step
Commit T1 (feature doc first, then the build change), run the review assessment, then ask the user to push and open PR 1. Next task: T2 JUnit 6 hardening on a new branch from this one.
