# Attribution

## Inspiration

Armor Status NeoForge is inspired by **Armor Status** by **ShirjeelPlays**, a client-side Fabric mod that shows armor durability on the HUD.

- Source repository: https://github.com/ShirjeelPlays/Armor-Status
- Modrinth page: https://modrinth.com/mod/armor-status (the page lists the same GitHub repository as its source)

This project is **not an official release** by the original author and is not affiliated with or endorsed by them.

## What was and was not reused

This is a **clean-room reimplementation**. No source code, textures or icon from the original project were copied. Only the observable behaviour was reproduced: which armor pieces are shown, in which order, the icon-plus-number rows, the six screen anchors, the scale range, the per-slot toggles, and the conditions under which the HUD is hidden. The code, structure, configuration format and tests are original and were written for NeoForge and Kotlin.

The mod's name keeps the "NeoForge" suffix and the description says "unofficial" so it cannot be mistaken for the original.

## Why clean-room

On 2026-10-10 the original project's licence information was inconsistent:

| Where | What it says |
|---|---|
| GitHub `LICENSE` file | CC0-1.0. It replaced an earlier custom, restrictive licence in the second upstream commit. |
| `fabric.mod.json` | CC0-1.0 |
| Upstream README | MIT |
| Modrinth project metadata | CC-BY-4.0 |
| Modrinth project description | "Free to use. No modifying, selling, or reuploading." |

These statements conflict. Rather than rely on any one of them, this project copies nothing from the original and credits it for the idea. The original author could clarify the licence by opening an issue upstream; that has not been done.

This section describes the situation found on that date. It is not legal advice.

## Original work in this repository

All code, documentation, build scripts and original icon artwork here are released under the [MIT licence](LICENSE), copyright 2026 athegreat90. The steel-blue chestplate and segmented durability meter were created for this mod, without copying Minecraft textures or upstream artwork. The editable source is `artwork/icon.svg`; `neoforge/src/main/resources/icon.png` is its 512x512 nearest-neighbor export from a 32x32 grid.

## Third-party tools and dependencies

Nothing below is copied into this repository. Licence statements appear only where they were checked.

| Component | Role | Licence |
|---|---|---|
| [NeoForge](https://neoforged.net/) | Mod loader and Minecraft modding API | Not verified here |
| [KotlinLangForge](https://modrinth.com/mod/kotlin-lang-forge) | Language provider that loads Kotlin mods on NeoForge. Required at runtime, **not bundled**. | GPL-3.0-only (as listed on its Modrinth page, checked 2026-10-10) |
| Kotlin | Implementation language and standard library | Not verified here |
| [ModDevGradle](https://github.com/neoforged/ModDevGradle) | Gradle plugin for NeoForge development | Not verified here |
| Spotless with ktlint | Formatting and lint checks (build time only) | Not verified here |
| JUnit 5 and kotlin-test | Unit tests (build time only) | Not verified here |
| Minecraft | The game this mod runs inside; assets are not redistributed | Mojang / Microsoft terms |

Minecraft is a trademark of Mojang Synergies AB. This project is not affiliated with Mojang or Microsoft.
