# Repo rules — Infinite Dungeon Spawn Safety

Repo-specific rules and notes. The org-wide `AGENTS.md` beside this file is
maintained in [`neoforge-mod-template`](https://github.com/HexaHexes/neoforge-mod-template)
and pulled in verbatim - **never hand-edit it.** Everything in this file is this
repo's own and is edited freely; keep it aligned with the org rules rather than
restating them.

**Repo:** `HexaHexes/infinite-dungeon-spawn-safety` · **Mod ID:** `infinite_dungeon_spawn_safety` · **Branch:** `main`
**Platform:** Minecraft 1.21.1 / NeoForge 21.1.259 / JDK 21 · **Jar:** `build/libs/infinite_dungeon_spawn_safety-<version>.jar`

A standalone spawn gate for the infinite-dungeon suite. It blocks only natural
spawns, and only when the ambient magicule of the spawn chunk cannot sustain the
mob, filtered by per-category toggles and a dimension list. It consumes
`infinite-dungeon-api` for ambient magicule and entity power values and has no
other suite dependency: it works in a pack without the dungeon installed.

**Dependencies**
- Required: Minecraft, NeoForge, `infinite-dungeon-api` (GitHub Packages Maven).
- Optional: Tensura: Reincarnated (real magicule and AP/MP/EP values; without it
  every requirement is zero and the gate is a no-op); infinite-dungeon (its
  dimension registers the dungeon magicule curve as an ambient source).

**Test:** `./gradlew runGameTestServer` - `SpawnSafetyGameTests` (6 tests).
`expectedTests = 6` in `build.gradle` must match the `@GameTest` count.

**Remote:** `github.com/HexaHexes/infinite-dungeon-spawn-safety` (public, default
branch `main`).

---

## Gotchas (do not regress them)

- The gate must never touch non-natural spawns. The allowlist lives in
  `infinite-dungeon-api`'s `NaturalSpawns`; adding a spawn type there is an API
  decision, not a local one.
- Unit tests run without a Minecraft runtime: keep pure logic (gate math,
  classification, config resolution, list precedence, the data reducer) on
  classes that import no Minecraft or Mojang types, or JUnit fails with
  `NoClassDefFoundError`. `infinite-dungeon-api` **is** on the JUnit classpath
  (`testImplementation`), so its pure types - `ResourceRef`, `TagExpansion`,
  `RequiredModFilter`, `ClassificationOverrides`, `SpawnCategory` - are usable
  in tests; its game-facing types and every codec are not, because Minecraft and
  Mojang stay off the test classpath.

## Naming - this repo

- Repo and folder: `infinite-dungeon-spawn-safety`; `mod_id`:
  `infinite_dungeon_spawn_safety`; Java base package
  `org.bitwisemadness.infinitedungeonspawnsafety`, with the Maven `group` in
  `build.gradle` matching.

## CI & release model - this repo

- `ci.yml` runs the template's three checks (junit, gametest, version-bump).
- `release.yml` cuts the GitHub release for `v<mod_version>` on a push to `main`
  (pre-releases only when the repo variable `ALPHA_RELEASES` is true), and
  `publish.yml` publishes the Maven artifact to the org's GitHub Packages
  registry when that release is published - see the org rulebook for the
  consumer-side auth details (`PACKAGE_READ_TOKEN`).