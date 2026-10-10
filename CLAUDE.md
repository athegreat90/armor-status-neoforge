# Armor Status NeoForge - engineering rules

@MEMORY.md

Durable rules for Claude Code in this repository. Project state, versions and history live in `MEMORY.md`. Full Kotlin conventions: `.claude/rules/kotlin.md`. Testing: `.claude/rules/testing.md`.

## Repository

- Native Windows path: `C:\Users\alexa\Documents\GitRepo\armor-status-neoforge`. WSL form: `/mnt/c/Users/alexa/Documents/GitRepo/armor-status-neoforge`. The build uses no absolute paths.
- Shell is PowerShell on Windows 11. Use `gradlew.bat`. **Quote `"-PmcVersion=26.2"`**: unquoted, PowerShell splits at the dot.
- Goal: a client-only NeoForge mod (Kotlin, KotlinLangForge) that shows armor durability on the HUD, for Minecraft 26.1.2, 26.2 and 26.3. Unofficial, clean-room inspired by ShirjeelPlays' Armor Status.
- Modules: `common` (pure Kotlin, zero Minecraft dependency) and `neoforge` (adapter). Pure decisions go in `common`.

## Kotlin policy (condensed)

- Kotlin first. Prefer `val`, immutable `data class` + `copy`, sealed types for finite states, extension functions for domain transforms, pure calculation functions, read-only collections.
- Make illegal states unrepresentable (validate in `init`, or coerce in a `sanitized` factory).
- Null safety: `?.`, `?:`, early returns, `requireNotNull`/`checkNotNull`. **Never `!!`.**
- Scope functions: `let` for nullable values, `apply` to configure, `also` for side effects, `takeIf`/`takeUnless` for validation. No nesting beyond one level; use plain code when clearer.
- No coroutines (no async need), no Java Streams, no reflection.
- Clarity, performance and correctness beat terseness. Do not replace straightforward code with chains.

## Result and `runCatching`

- Use `runCatching` only at recoverable boundaries, through `loadOrDefault` in `common/.../error/Recovery.kt` (for example reading the config file into `HudSettings`).
- Never swallow fatal failures: `isFatal()` covers `CancellationException`, `InterruptedException`, `VirtualMachineError` and `LinkageError`; `rethrowFatal()` rethrows them.
- **Never in the render path.** Render code is total; a one-shot `failed` flag disables the layer and logs once.
- Do not mask mod init failures.
- Validate ranges explicitly (`coerceIn`), not with exceptions.

## Immutability and documented mutable exceptions

State is immutable by default. Two deliberate exceptions, both documented in code:

1. `ActiveHudSettings`: one `@Volatile` reference to an immutable `HudSettings`, replaced whole on config load or reload.
2. The render-thread caches in `ArmorStatusLayer` (`LayoutCache`, `DurabilityTextCache`, row arrays), owned by one instance and touched only by the render thread.

Adding any other mutable shared state needs a stated reason in `MEMORY.md`.

## KotlinLangForge and NeoForge facts

- `neoforge.mods.toml`: `modLoader = "klf"`, `loaderVersion = "[1,)"`, dependencies with `side = "CLIENT"`. Generated from `neoforge/src/main/templates`.
- **No explicit `[[dependencies]]` entry for `klf`**: it fails in the dev run (`Actual version: [MISSING]`). The `modLoader` key already requires the provider.
- KLF is `implementation("dev.nyon:KotlinLangForge:2.15.0-k2.4.21-3.1+neoforge")`, never `modImplementation`, never shaded.
- Use `@Mod(value = MOD_ID, dist = [Dist.CLIENT])` on a **class** when constructor injection (`IEventBus`, `ModContainer`) is needed; it cannot be an `object`.
- `MOD_ID` is in `ModConstants.kt` and `mod_id` in `gradle.properties`; `verifyModMetadata` asserts they match.

## Toolchain

- Java 25 for the project. Gradle also needs a JDK 21 launcher for ModDevGradle's `downloadAssets`; the Foojay resolver (`1.0.0`) downloads it.
- Gradle wrapper **9.2.1**, pinned with `distributionSha256Sum`. Kotlin Gradle Plugin 2.4.21 documents support up to Gradle 9.7.0. Bump only after a verified build on all targets.
- Formatting: Spotless with ktlint 1.8.0, `ktlint_official`, 120 columns (`.editorconfig`). Run `spotlessApply`, then `check`.

## Minecraft version compatibility

- Targets are the files in `gradle/mc/*.properties`; select one with `-PmcVersion=<ver>` (default `26.1.2`). One jar per version, named `armor-status-neoforge-<modVersion>+mc<mc>.jar`.
- The only version-dependent code is the hide-GUI check: overlay `neoforge/src/v26_1` (26.1.x, `Options#hideGui`) and `neoforge/src/v26_2` (26.2 and 26.3, `Hud#isHidden`). Add an overlay only when a real compile break appears.
- Keep version-sensitive Minecraft calls behind `HudCanvas` and the overlays. 26.3 uses a NeoForge beta.

## HUD rendering performance

- The layer runs every frame: indexed loops over fixed arrays, no `map`/`filter`/`sequence`, no lambdas or lists created per frame.
- Cache the layout (by screen size, scale, anchor, row count) and the durability strings (by maxDamage, damage, mode, gradient); rebuild only when inputs change.
- Read settings from the `ActiveHudSettings` snapshot, never from `ModConfigSpec`, in the render path.

## Testing

- Pure logic in `common`: write the failing test first (RED), then implement (GREEN), then refactor. JUnit 5 + kotlin-test, parameterized tests, immutable fixtures.
- Exception: NeoForge-bound code has no unit-test harness. Prove it by compiling all three targets, `check`, and `runClient`. Say so in the commit or `MEMORY.md`.
- Never report unit tests as runtime verification. Record what was run, with date and outcome, in `MEMORY.md`.

## GitHub Actions and Modrinth

- `.github/workflows/ci.yml` (running, green on `main`): matrix over the three versions; Spotless, `:common:test`, `check build`, upload jars; actions pinned by full SHA; `permissions: contents: read`.
- `.github/workflows/release.yml` (on `main`, **never run**): tag `v*.*.*`, duplicate guard, GitHub Release, Modrinth upload through Minotaur per target; 26.3 as `beta` until NeoForge 26.3 has a stable build. `workflow_dispatch` is always a dry run. A real tag needs a `[0.1.0]` section in `CHANGELOG.md`.
- Secret `MODRINTH_TOKEN`, variable `MODRINTH_PROJECT_ID`. Neither exists yet; never store them in the repository.

## Attribution and licensing

- Our code is MIT. This is a clean-room reimplementation: **never copy upstream code, textures or the icon.** Reproduce observable behaviour only.
- Keep the "unofficial, not affiliated" wording in README and `neoforge.mods.toml`. KLF (GPL-3.0) is a runtime dependency only.

## Security and delivery

- No secrets, tokens or credentials in the repository, logs or docs.
- **No public push, public repository, release or publish without explicit user approval.** Never force-push. Remote work needs explicit authorization for destination, operation and credentials.
- Work on a feature branch; keep pull requests near 400 changed lines.

## Commits

- Conventional Commits (`feat`, `fix`, `docs`, `build`, `style`, `test`, `refactor`, `chore`), scope optional.
- **Never add `Co-Authored-By` or any AI attribution** to commit messages.
