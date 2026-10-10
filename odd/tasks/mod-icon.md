# Original mod icon

## Objective

Give Armor Status NeoForge an original, recognizable icon for the mod list and project listings: a pixel-art steel-blue chestplate with a segmented durability meter on navy.

## Scope and constraints

- User authorized creating an icon representing the mod.
- Create a 512x512 PNG and editable, self-contained SVG source.
- Integrate the PNG through the existing NeoForge metadata template.
- Update only icon-related attribution, changelog, and project-state statements.
- No copied Minecraft or upstream assets, external fonts, dependency installs, server launches, publishing, or push.
- Artifact language: English.

## Work unit

- [x] ICON-01: Create original artwork, integrate metadata, and verify the packaged icon.
  - Route: delegated direct; artwork creation and integration require preparation and multiple related edit surfaces.
  - Allowed surfaces: `artwork/icon.svg`, `neoforge/src/main/resources/icon.png`, `neoforge/src/main/templates/META-INF/neoforge.mods.toml`, `ATTRIBUTION.md`, `CHANGELOG.md`, `MEMORY.md`.
  - Acceptance: square 512x512 valid PNG; self-contained SVG; original readable chestplate and durability metaphor; metadata references root `icon.png` with nearest-neighbor sampling; JAR contains identical icon bytes.
  - Test-first policy: default ODD, no configured strict mode observed. Passive artwork has no meaningful automated aesthetic RED. Observe missing metadata logo assertion before edits, then verify it passes afterward.
  - Required functional command: `.\gradlew.bat :neoforge:verifyModMetadata "-PmcVersion=26.2" --console=plain`.
  - Structural command: Python standard-library checks for PNG chunks/dimensions/decompression, SVG XML and self-containment, rendered metadata, and packaged asset hash; record exact invocation and results.
  - Runtime scenario: N/A for passive image generation; in-game appearance remains manual unless observed.
  - Rollback boundary: remove new icon assets, revert only logo metadata and icon-related documentation.
  - Verification: observed metadata assertion RED before integration and GREEN after; Gradle metadata/JAR check passed for 26.2; Python validated PNG CRCs, dimensions, decompression, exact SVG pixels, all metadata stages, and identical packaged bytes; `git diff --check` passed. Parent repeated the structural verifier successfully and visually read the PNG.
  - Structural rerun: `python "C:\Users\alexa\AppData\Local\Temp\opencode\icon01-verify.py" "C:\Users\alexa\Documents\GitRepo\armor-status-neoforge"` (external check-only temporary script, not a permanent repository test).
  - PNG SHA-256: `367fb2c6fb5896e538254a14a3a3f65277fe925df5246fb798ac355c5b7c40ff`.
  - Limitations: in-game mod-list appearance unverified; only target 26.2 was rebuilt for this shared asset.
  - Commit: pending identity for `feat(mod): add original armor status icon`.
  - RDD mode: on, global source; staged candidate assessed medium (`executable_change`, SVG), 136 authored lines, `review_due=false`, `under_budget`. Committed-range assessment pending; no candidate consent requested yet.

## Delivery

- Strategy: ask-on-risk; forecast about 140 authored lines, generated PNG excluded.
- One coherent work-unit commit on `feat/mod-icon`; no remote operations.
- Base boundary: `66d2d3849d2c9b07b3b91da1d323f397280677c2`.
- Running authored lines: pending.
- Engram mirror: pending.

## Progress and next step

Artwork, metadata integration, and icon-related documentation are implemented and checked. The intended files are staged; native assessment classified this candidate medium and under budget. Commit the verified work unit, then assess that exact committed range and record its identity and outcome. In-game appearance remains a manual follow-up.
