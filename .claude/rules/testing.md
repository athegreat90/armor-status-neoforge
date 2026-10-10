# Testing rules

## Stack

JUnit 5 (JUnit BOM 6.1.3, Platform launcher) with `kotlin-test` and `junit-jupiter-params`, in `common/src/test`.

## What to test

- Pure logic in `common` is test-first: write a failing test (RED), implement (GREEN), then refactor. If RED can only be shown as a compile error, say so.
- Use parameterized tests for tables of cases (anchors x scales x GUI sizes, durability values, colour stops).
- Use immutable fixtures (`HudSettings(...)`, `DurabilityReading.Damageable(...)`); no shared mutable state between tests.
- Cover boundaries and invalid input: zero or negative max, damage above max, `NaN`, unknown enum names, scale limits.
- Error recovery: a recoverable failure yields the default and calls `onFailure` once; a fatal failure propagates and runs neither callback.

## What not to test

- Minecraft or NeoForge internals, and code that needs a running game (`ArmorStatusLayer`, `HudCanvas`, `ModConfigSpec` binding, the config screen). There is no unit-test harness for them.
- Do not mock Minecraft types to make a test pass; move the decision into `common` instead.

## Running tests

```powershell
.\gradlew.bat :common:test                      # unit tests; independent of the Minecraft version
.\gradlew.bat check "-PmcVersion=26.2"          # Spotless + tests + jar metadata check for one target
.\gradlew.bat :common:test --rerun-tasks        # force a real re-run instead of an up-to-date result
```

Results are in `common/build/test-results/test` (XML). Quote `-PmcVersion` in PowerShell.

For changes in `neoforge`, run `build` or `check` on each of `26.1.2`, `26.2` and `26.3` that the change can affect.

## Evidence and honesty

- Unit tests are not runtime verification. Never claim in-game behaviour from them.
- Record every claimed result in `MEMORY.md` with the command, date and outcome.
- Visual correctness (icons, position, shadow, overlap, F1, GUI scales, config screen) needs a manual check and is reported as unverified until done.

## Smoke tests (dev client)

1. Create a save through `runServer` or the client and copy it to `neoforge/run/saves/<name>` (`neoforge/run` is gitignored).
2. Add a test datapack that equips armor with chosen damage values.
3. Start `.\gradlew.bat runClient "-PmcVersion=<ver>" "-PquickPlay=<name>"`.
4. Check `latest.log` for the mod's log lines and for `ERROR` or `Exception`.
5. Edit `neoforge/run/config/armorstatusneoforge-client.toml` while the client runs to exercise the reload path.

A log-based smoke test proves the render path runs without errors; it does not prove the output looks right.
