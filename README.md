# Infinite Dungeon Spawn Safety

Keeps chosen dimensions mostly safe without touching everything: spawns are
filtered by category and by whether the local ambient magicule can sustain the
mob.

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

## Credits

Bitwise Madness.

## License

GNU Lesser General Public License v2.1 - see `LICENSE`.
