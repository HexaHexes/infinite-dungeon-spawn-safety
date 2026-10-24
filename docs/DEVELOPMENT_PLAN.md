# infinite-dungeon-spawn-safety Development Plan

The living source of truth for this module's design. It records the config and
datapack surface, the gate rules, and why the module sits outside the core.

## 1. Identity and conventions

- Repository: `HexaHexes/infinite-dungeon-spawn-safety`.
- Mod id: `infinite_dungeon_spawn_safety`. Package:
  `org.bitwisemadness.infinitedungeonspawnsafety`.
- Minecraft 1.21.1, NeoForge 21.1.259, JDK 21.
- Versioning: `1.0.0-alpha.N` per finished feature; clean `1.0.0` at release.
- Process: one changeset per PR, squash merge, version bump per finished
  feature, public API only, no mixins, ASCII-only sources.
- CI: junit, gametest, version-bump; the two rulesets land after this repo's
  first CI PR merges.
- Dependency: `org.bitwisemadness:infinite_dungeon_api` from the org's GitHub
  Packages registry. No dependency on the core module.

## 2. Role

This module answers one question at spawn time: may this natural spawn happen
here? It is deliberately standalone:

- It consumes the API for ambient magicule and entity power values, so it works
  in a pack that has no dungeon installed.
- Whenever the core module is present, its dimension registers the dungeon's
  magicule curve as an ambient source and this module sees it without knowing
  about it.
- Whenever a stats provider is present (Tensura via its integration module),
  mobs are measured by AP/MP/EP; without one, every requirement is zero and the
  gate is a no-op rather than a guess.

## 3. Configuration

Config `safety.*`:

| Key | Default | Meaning |
|---|---|---|
| `enabled` | on | master toggle |
| `hostile` | off | hostiles may spawn at all |
| `neutral` | on | neutral mobs may spawn |
| `passive` | on | passive mobs may spawn |
| `dimensions.mode` | `whitelist` | `whitelist` or `blacklist` |
| `dimensions.list` | `[minecraft:overworld]` | affected dimensions |
| `magiculeGate.enabled` | on | attribute-driven magicule gate |
| `magiculeGate.stat` | `ep` | `ap` (aura), `mp` (magicule), or `ep` (stored energy) |
| `magiculeGate.deviationPercent` | 10 | tolerance band on the strict rule |
| `magiculeGate.attributeScale` | 1 | unit alignment; advanced |
| `magiculeGate.ambientFallback` | 0 | ambient value when no provider answers |

Dungeon dimensions are exempt by default: the API's dimension profiles mark
them, and the default dimension list only names the overworld.

## 4. Datapack surface

`data/<namespace>/infinite_dungeon/safety/`:

- `classification.json` - override-only classification entries keyed by entity
  type id.
- `hostile.json`, `neutral.json`, `passive.json` - each carrying `whitelist`
  and `blacklist` entity references, with the shared `required_mod` gate and
  id-or-tag references from the API's data utilities.

A category file:

```json
{
  "required_mod": "tensura",
  "whitelist": ["minecraft:zombie", "#minecraft:raiders"],
  "blacklist": ["minecraft:creeper"]
}
```

`classification.json` maps entity type ids to categories:

```json
{
  "minecraft:creeper": "neutral"
}
```

Precedence: a disabled category blocks its mobs; a blacklist beats a whitelist;
a non-empty whitelist restricts to its members.

Multi-pack rules: a `classification.json` merges by entity id, later location
winning per key; each category file replaces that category wholesale, so a
higher-priority pack supersedes rather than appends. Files are collected in
sorted location order, which makes both deterministic.

## 5. Classification

Derived, never curated, through the API's chain: datapack override, then the
`NeutralMob` interface, then the `Monster` class or the vanilla `MONSTER`
category, then passive. The rules live in the API
(`ClassificationRules`/`MobClassification`); this module only applies them.

## 6. The gate

- Scope: natural spawns only, through the API's positive allowlist
  (`NaturalSpawns`, `MobSpawnType.NATURAL`). Spawner, structure, command, egg,
  event, and patrol spawns are never blocked or allowed by magicule
  concentration, and chunk generation is untouched.
- Rule: a natural spawn is allowed when

  `ambient >= statValue * attributeScale * (1 - deviationPercent / 100)`

  where `ambient` is the API's ambient magicule at the spawn position and
  `statValue` is the mob's own `magiculeGate.stat` value.
- Absent values: no stats provider means the requirement is zero (gate passes);
  no ambient provider means the value is `ambientFallback` (default zero, which
  blocks only when a stats provider supplies a positive requirement).
- The gate is intercepted through `MobSpawnEvent.PositionCheck`, the hook
  NeoForge 21.1 actually posts on the natural path - `FinalizeSpawnEvent` fires
  only for spawners. The spawner posts it after the mob is built and positioned
  but before it joins the world, so its attributes are readable and a rejected
  spawn leaves nothing behind.

## 7. Behavior matrix

| Pack contents | Result |
|---|---|
| Spawn safety alone | Category toggles and dimension filter work; the gate sees ambient 0 and requirements 0, so it blocks nothing |
| + Tensura | Real magicule and AP/MP/EP; the gate becomes meaningful in affected dimensions |
| + dungeon core | The dungeon dimension registers the magicule curve; the dimension list keeps the gate out of it by default |

## 8. Delivery waves

- Wave S1: config schema, dimension filter, and category toggles.
- Wave S2: the magicule gate with the deviation band and scale.
- Wave S3: datapack category lists and classification overrides loading and
  `/reload`.
- Wave S4: the natural spawn interception through the API's services.
- Wave S5: diagnostics (a debug command or log line explaining why a spawn was
  blocked) and the docs polish.

Each wave is one feature per PR and one alpha bump.

## 9. Test coverage plan

JUnit (pure logic, no game runtime):

- config precedence and dimension filter resolution;
- category precedence over whitelist and blacklist;
- gate math: strict rule, deviation band, scale, zero-value no-op, fallback;
- classification application through the API's rules;
- list loading through the Minecraft-free reducer: required-mod gating,
  whole-file replacement per category, and the keyed classification merge.

GameTest (live server):

- a natural spawn is gated and a spawner spawn is not, with stub providers.

The live reload's observable effect lands with the interception wave: until a
spawn consults the tables, a reload is only observable through the pure reducer,
which is why the loading rules are covered in JUnit instead of in game.

## 10. Decision record

- The gate is its own module so a pack can keep the overworld safe without
  installing the dungeon, and so the dungeon's own dimension can stay exempt.
- It depends on the API only; ambient magicule and entity power values are
  services, never direct references to another module's classes.
- Only natural spawns are gated, by allowlist, so non-natural spawns (spawners,
  structures, commands, eggs, events) always keep working.
- Classification is derived with datapack overrides, never a curated list.
- With nothing providing values, the gate is a no-op rather than a guess.
