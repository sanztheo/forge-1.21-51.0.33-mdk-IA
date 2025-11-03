# Repository Guidelines

## Project Structure & Module Organization
- Forge Version MC : 1.21
- Source code: `src/main/java/net/frealac/iamod/` (Java 21).
- Resources: `src/main/resources/` with `META-INF/mods.toml` and `pack.mcmeta`.
- Assets (textures, models, lang): `src/main/resources/assets/iamod/**` (all lowercase names).
- Build tooling: `gradlew`, `gradlew.bat`, `build.gradle`, `settings.gradle`, `gradle.properties`.
- Runtime work dir (generated): `run/` (not committed).

## Build, Test, and Development Commands
- `./gradlew clean build` (Windows: `.\\gradlew clean build`) - compile, process resources, and assemble JAR to `build/libs/`.
- `./gradlew runClient` - launch the Forge client with the mod loaded.
- `./gradlew runServer` - start a dedicated server with the mod.
- `./gradlew runGameTestServer` - execute Forge GameTests and exit.
- `./gradlew jar` - build the dev jar only.
- `./gradlew publish` - publish to local `mcmodsrepo/` as configured in `build.gradle`.

## Coding Style & Naming Conventions
- Language: Java 21; 4-space indentation; braces on same line.
- Packages: `net.frealac.iamod`. Classes `PascalCase`; methods/fields `camelCase`; constants `UPPER_SNAKE_CASE`.
- Mod id: keep `IAMOD.MOD_ID == "iamod"` and `gradle.properties#mod_id` in sync.
- Resource and asset filenames are lowercase with underscores (e.g., `assets/iamod/lang/en_us.json`).

## Testing Guidelines
- GameTests are preferred for in-game logic; run with `runGameTestServer`.
- Optional unit tests: place under `src/test/java` and run with `./gradlew test` (add JUnit 5 if needed).
- Name tests after the feature under test (e.g., `BlockRegistryTest`).

## Commit & Pull Request Guidelines
- History has no established convention yet; prefer Conventional Commits (e.g., `feat: add villager trade`).
- Keep subject in imperative mood; include brief body and references (e.g., `Closes #12`).
- PRs should include: purpose, user impact, testing steps, and screenshots for in-game changes.

## Security & Configuration Tips
- Do not change Minecraft/Forge versions without discussion (toolchain and mappings are coupled).
- Do not commit generated folders like `run/` or `mcmodsrepo/`.
- Update both `mods.toml` and `gradle.properties` when changing mod metadata.

## Agent-Specific Instructions
- Keep changes scoped; avoid unrelated refactors or formatting.
- Follow structure above; place assets under `assets/iamod/` and keep `mod_id` consistent.
- Prefer Gradle tasks listed here; do not modify build plugins or versions unless requested.

