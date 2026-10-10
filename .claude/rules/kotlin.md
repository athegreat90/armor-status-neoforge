# Kotlin conventions

Applies to all Kotlin in this repository (`common`, `neoforge`, Gradle Kotlin DSL). Summary lives in `CLAUDE.md`; this file has the detail.

## Principles

- Clarity, performance and correctness come before terseness.
- Do not replace straightforward code with a chain of calls just because a chain is possible.
- Prefer pure functions that take values and return values. Keep side effects at the edges (logging, drawing, reading the config file).

## Immutability and modelling

- Use `val` and read-only collections (`List`, `Map`); do not expose `MutableList`.
- Domain models are immutable `data class` values; change them with `copy`.
- Use `sealed interface`/`sealed class`/`enum` for finite states and `when` without `else`, so a new case breaks the build instead of being ignored.
- Make illegal states unrepresentable: validate in `init` with `require`, or provide a lenient factory that coerces (`HudSettings.sanitized`).
- Use extension functions for domain transforms (`DurabilityReading.label`, `HudSettings.shows`).
- Mutable state needs a written reason. Today's exceptions: the `@Volatile` `ActiveHudSettings` holder and the render-thread caches (see `CLAUDE.md`).

## Null safety

- Use `?.`, `?:` and early returns (`?: return`). Use `?.let { ... }` to work with a non-null value.
- Use `requireNotNull`/`checkNotNull` with a message when absence is a bug.
- Never use `!!`.

## Errors: `runCatching`, Result, try/catch

- Use `runCatching` for recoverable operations at a boundary, combined with `onFailure` (log once) and `getOrElse` (fall back to a default). In this repository that is `loadOrDefault`.
- Do not write `try`/`catch` where nothing can be recovered. Do not wrap code that cannot fail in `runCatching`.
- `runCatching` also catches `Throwable`. Preserve cancellation and serious errors: call `rethrowFatal()` (or use `loadOrDefault`), which rethrows `CancellationException`, `InterruptedException`, `VirtualMachineError` and `LinkageError`.
- Do not use exceptions for control flow. Validate ranges with `coerceIn`, `takeIf` or explicit checks.
- Never use `runCatching` in the render path; use plain total functions and, at the outer edge, one `try`/`catch` with a one-shot disable flag.
- Do not hide initialization failures behind a default.

## Scope functions

| Function | Use for |
|---|---|
| `let` | Run code on a nullable value (`value?.let { ... }`) or map a value once. |
| `apply` | Configure an object you just created. |
| `also` | Side effects such as logging that must not change the value. |
| `takeIf` / `takeUnless` | Inline validation that yields the value or `null`. |
| `run` / `with` | Rarely; only when it removes repetition without hiding the receiver. |

Do not nest scope functions more than one level. If `it`/`this` becomes ambiguous, use a named local.

## Collections and hot paths

- `map`, `filter`, `mapNotNull`, `firstOrNull` and friends are fine for setup, configuration and tests.
- Not on the per-frame HUD path. There, use indexed loops over fixed arrays, reuse buffers, and allocate nothing per frame (no lambdas or temporary lists).
- Avoid functional abstractions that add allocation or indirection where measured clarity does not improve.

## Concurrency and libraries

- No coroutines: there is no asynchronous work. If that changes, record the decision in `MEMORY.md` first.
- No Java Streams and no reflection.
- Visibility: `internal` for NeoForge-bound adapter objects, public only for what `common` exposes to `neoforge` and tests.

## Style

- Formatting is owned by ktlint (`ktlint_official`, 120 columns). Run `spotlessApply`; do not hand-format against it.
- KDoc explains the why and the contract (units, ranges, thread ownership), not what the code already says.

## In this codebase

- `common/.../error/Recovery.kt`: `isFatal`, `rethrowFatal` and `loadOrDefault`, the only use of `runCatching`.
- `neoforge/.../hud/ArmorStatusLayer.kt`: render-path rules in practice (indexed loops over `SLOTS`, cached layout and text, one-shot `failed` flag, documented mutable fields).
- `common/.../model/DurabilityReading.kt`: sealed interface, validation in `init`, and `readDurability` as a total conversion that clamps bad input.
- `common/.../config/HudSettings.kt`: immutable `data class` with `init` validation and the lenient `sanitized` factory.
