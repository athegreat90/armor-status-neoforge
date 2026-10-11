# Testing rules

## Stack

JUnit Jupiter 6.1.3 (JUnit BOM, `junit-jupiter` aggregator with API, params and engine, plus the Platform launcher) with `kotlin-test`, in `common/src/test`. No JUnit 4, Vintage or mocking library. Test classes must be named `<Name>Test` in `<Name>Test.kt` under the package path.

`tasks.test` sets `failOnNoDiscoveredTests = true`, logs passed/skipped/failed tests and writes XML and HTML reports. The `verifyTestResults` task (finalizer of `test`, also part of `check`) fails the build when no test ran, any test was skipped (`@Disabled`, assumptions), failed or errored, or a `*Test.kt` file has no result file or ran 0 tests. So every `*Test.kt` must produce results; do not leave a test disabled, delete it or fix it. A filtered run (`--tests "*Name"`) skips only the per-class completeness check, because it leaves the other classes without results on purpose; the skipped, failed and zero-tests checks still apply. `verifyModMetadata` also fails if the mod jar contains `org/junit/` or `kotlin/test/` entries.

In CI the `common/build/reports` and `test-results` folders are uploaded as artifact `reports-mc<version>` on every run (not only on failure), and the job summary shows suites, tests, failures, errors and skipped.

## What to test

- Pure logic in `common` is test-first: write a failing test (RED), implement (GREEN), then refactor. If RED can only be shown as a compile error, say so.
- Use parameterized tests for tables of cases (anchors x scales x GUI sizes, durability values, colour stops).
- Use immutable fixtures (`HudSettings(...)`, `DurabilityReading.Damageable(...)`); no shared mutable state between tests.
- Cover boundaries and invalid input: zero or negative max, damage above max, `NaN`, unknown enum names, scale limits.
- Error recovery: a recoverable failure yields the default and calls `onFailure` once; a fatal failure propagates and runs neither callback.

## What not to test

- Minecraft or NeoForge internals, and code that needs a running game (`ArmorStatusLayer`, `HudCanvas`, `ModConfigSpec` binding, the config screen). There is no unit-test harness for them.
- Do not mock Minecraft types to make a test pass; move the decision into `common` instead. Example: the weapon decision is tested in `weapon/WeaponClassifier.kt` and `tooltip/TooltipDurability.kt` (`common`), while `neoforge/.../tooltip/WeaponTooltip.kt` and the stack-to-signals mapping are not unit tested.
- Minecraft-bound code has no unit-test harness, so the in-game checklist is the proof. Anything not looked at on screen is recorded as unverified.

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
