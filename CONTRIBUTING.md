# Contributing

Thanks for your interest. This is a small project; keep changes focused.

## Setup

1. Install JDK 25. Gradle downloads a JDK 21 on first use (Foojay toolchain resolver) for the Minecraft asset task.
2. Clone the repository and use the committed Gradle wrapper.
3. Run the checks for one target:

```powershell
.\gradlew.bat check "-PmcVersion=26.2"
```

Quote `-PmcVersion` in PowerShell, otherwise it is split at the dot. See [README.md](README.md#development) for all commands.

## Code conventions

- Kotlin first, immutable data, pure functions, no `!!`. Details are in [.claude/rules/kotlin.md](.claude/rules/kotlin.md) and [CLAUDE.md](CLAUDE.md).
- Format with `.\gradlew.bat spotlessApply` before committing. `check` runs Spotless (ktlint, 120 columns) and fails on violations.
- Do not copy code, textures or the icon from the original Armor Status mod. See [ATTRIBUTION.md](ATTRIBUTION.md).

## Tests

- Logic that does not need Minecraft belongs in `common` and needs unit tests, written before the implementation. See [.claude/rules/testing.md](.claude/rules/testing.md).
- Code that touches Minecraft or NeoForge cannot be unit tested here; describe how you verified it (build on all three targets, `runClient`).
- `check` must pass for each Minecraft version your change can affect.

## Commits and pull requests

- Use [Conventional Commits](https://www.conventionalcommits.org/), for example `feat(hud): add low durability warning`.
- Keep a pull request to roughly 400 changed lines. Split larger work into a chain of smaller pull requests.
- Update `CHANGELOG.md` under `[Unreleased]` for user-visible changes.

## Reporting issues

Open a GitHub issue with the Minecraft version, NeoForge version, KotlinLangForge version, the mod version, what you expected, what happened, and the relevant part of `latest.log`. Do not paste tokens or personal paths.

## Licence of contributions

By contributing you agree that your contribution is licensed under the [MIT licence](LICENSE) of this project.
