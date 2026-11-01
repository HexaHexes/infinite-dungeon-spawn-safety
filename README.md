# Infinite Dungeon Spawn Safety

Keeps chosen dimensions mostly safe by filtering natural spawns: by category,
and by whether the local ambient magicule can sustain the mob.

## What it does

- Per-category toggles: hostiles are off by default, neutrals and passives on.
- A dimension filter: whitelist or blacklist, with the overworld affected and
  dungeon dimensions exempt by default.
- A magicule gate: a natural spawn happens only when the ambient magicule of
  the spawn chunk can sustain the mob's own AP, MP, or EP (configurable, EP by
  default) within a tolerance band.
- Only natural spawns are ever gated. Spawners, structure placement, commands,
  spawn eggs, and events are untouched, and chunk generation never changes.
- Whitelists and blacklists per category, plus classification overrides, come
  from datapacks and reload with `/reload`.
- List precedence is simple: a switched-off category never spawns, a blacklist
  entry always blocks, and a whitelist with entries limits its category to what
  it lists.
- Exemptions (`exempt.json`): entities the module never blocks, wherever they
  spawn - showcase mobs like dragons and structure bosses skip every rule this
  mod applies. Packs ship rows in their own namespace and they union.
- Structure elevation (`structure_magicule/structures.json`): listed structures
  raise the magicule concentration of their surroundings **permanently**, by
  installing an anchored block modifier in each affected chunk's Tensura
  storage - Tensura's own computation re-applies that contribution on every
  recompute, so re-initialisation and regeneration cannot clamp it away, and
  Tensura's reads, the xaero concentration overlay and this mod's gate all see
  the same elevated value. Values **radiate** from the structure's pieces: the
  nearest chunks read 1.5x the baseline, the rim of the reach reads exactly the
  baseline (the rule's "equal or higher"), with a deterministic upward
  variation of up to 12 percent per chunk so the field is organic. The **reach
  scales with the baseline** - `clamp(sqrt(baseline) * 0.35, 48, 320)` blocks,
  so a 350,000 structure reaches ~207 blocks and an 800,000 one caps at 320,
  while a 20,000 one stays at ~49. Rows carry only the structure and the
  baseline (an old `radius` key is ignored); writes are skipped when Tensura is
  absent.

## Datapack reference

`data/<namespace>/infinite_dungeon/safety/`:

```jsonc
// exempt.json - the module never blocks these entities
{ "entities": ["iceandfire:fire_dragon", "#iceandfire:dragon_targets"] }
```

`data/<namespace>/infinite_dungeon/structure_magicule/`:

```jsonc
// structures.json - baseline floors around structure pieces (reach derived from the baseline)
{ "structures": [ { "structure": "iceandfire:fire_dragon_cave", "elevation": 800000.0 } ] }
```

Both support `required_mod` gating, so a pack can ship data for optional
content that applies only when the mod is present.

## Dependencies

| | Mod | Why |
|---|---|---|
| Required | Minecraft | - |
| Required | NeoForge | - |
| Required | Infinite Dungeon API | Ambient magicule and entity power services |
| Optional | Tensura: Reincarnated | Supplies real per-chunk magicule and AP/MP/EP values; without it every requirement is zero and the gate is a no-op |
| Optional | Infinite Dungeon | Its dimension registers the dungeon's magicule curve as an ambient source |

## Configuration

The config covers the master toggle, the per-category toggles, the dimension
list, and the gate (`enabled`, `stat`, `deviationPercent`, `attributeScale`,
`ambientFallback`). Datapack data under
`data/<namespace>/infinite_dungeon/safety/` supplies the category lists and
classification overrides.

## Diagnostics

A blocked spawn explains itself at `debug` level. The line names the entity, the
dimension, the classification category, the `GateReason`, and (for the magicule
gate) the ambient value, the requirement it was compared against, and the mob's
own stat:

```
Blocked natural spawn: entity=minecraft:sheep dimension=minecraft:overworld category=PASSIVE reason=INSUFFICIENT_MAGICULE ambient=0.00 required=90.00 stat=100.00
```

Nothing is logged for a passing gate or an untouched spawn, and the line sits at
`debug` rather than `info`, so normal logging stays quiet. To see it, raise the
mod's logger (`org.bitwisemadness.infinitedungeonspawnsafety`) to `debug` in
your log configuration.

## Credits

Bitwise Madness.

## License

GNU Lesser General Public License v2.1 - see `LICENSE`.

## AI disclosure

Parts of this project were written with the assistance of AI coding tools.
AI-assisted code, data and documentation ship only after human review by the
maintainers.

## In-game config editing

Mods built with this template can be edited in-game with
[Configured](https://www.curseforge.com/minecraft/mc-mods/configured) - install
it like any other mod; it adds no code to this one. Open the mod list, select a
mod, press Config, and change values directly: Configured writes through
NeoForge's config system, so a reload applies the new values without restarting
the game.

This template pre-wires the config-screen background in
`src/main/templates/META-INF/neoforge.mods.toml`
(`[modproperties.${mod_id}] configuredBackground`) - point it at any texture you
like. Note Configured only shows a menu for mods that actually register a
config: a template-derived mod with no registered settings has nothing to show.
