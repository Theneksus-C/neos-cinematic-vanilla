# Neo's Cinematic Vanilla

A client-side Fabric mod that makes the vanilla Minecraft world feel like a
place rather than a backdrop. Fog that responds to where and when you are,
motes drifting in the air, wind you can hear and see, and optional film grain.

Gameplay, progression, world generation, and the appearance of blocks and items
are unchanged. Nothing here affects what you can do, only what it looks like
while you do it.

**Minecraft 26.2 · Fabric · client-side only**

---

## What it does

**Dynamic fog.** Vanilla leaves atmospheric fog effectively switched off in the
Overworld. This fills that gap: haze gathers in low ground and clears as you
climb, caves thicken with depth, rain and storms add to it, and mist pools in
valleys at dawn before burning off through the morning.

**Ambient motes.** Fine particles suspended in the air, drifting on the wind and
fading in and out rather than popping. Sandy dust in desert country, pale
scattered motes where snow falls, defined shafts under a forest canopy, and
slow dust in deep caves.

**Biome character.** Deserts, swamps, forests, snowfields and peaks each get
their own fog weight, mote colour, and wind strength, so one place stops feeling
like the next. Biomes are classified by their climate rather than by name, so
modded biomes are handled too.

**Wind.** A single wind direction and strength that everything shares: an
ambient loop whose volume follows it, motes drifting with it, and leaves carried
on it. Wind is strongest on exposed peaks and open desert, weakest on a forest
floor or in a swamp. Openness is measured from your actual surroundings, not
guessed from the biome.

**Falling leaves.** Minecraft already drops leaves from every leaf block. This
adds a rate control and carries them on the wind so they fall together instead
of scattering independently.

**Film grain.** Optional, off by default. Strongest in shadows, absent over
menus and the HUD.

---

## Requirements

| | |
| --- | --- |
| Minecraft | 26.2 |
| Mod loader | Fabric Loader 0.19.5 or newer |
| Dependency | Fabric API |
| Java | 25 |

Client-side only. It does not need to be installed on a server, and it works on
servers that do not have it.

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft 26.2
2. Download [Fabric API](https://modrinth.com/mod/fabric-api) for 26.2
3. Put both jars in your `mods` folder

## Configuring it

Everything is adjustable in game. The settings screen is opened by a key
binding, which **defaults to unbound** so it cannot clash with a key you
already use:

**Options → Controls → Key Binds → Miscellaneous → Open Cinematic Vanilla
settings**

Bind a key, press it in game, and the screen opens. Every setting has a tooltip
explaining what it does.

Four presets sit at the top: **Off**, **Subtle**, **Balanced**, and
**Cinematic**. A preset is a starting point to tune from, not a mode, so your
debug toggles and Nether fog choice survive one.

Settings apply live as you drag a slider, and are written to
`config/neoscinematicvanilla.json` when the screen closes. Editing that file by
hand also works and is picked up within a second.

---

## Known limitations

Stated plainly rather than discovered later.

**Shader packs disable film grain.** Iris and similar replace the render
pipeline wholesale, so the grain pass has nothing to act on. Nothing breaks, it
simply will not appear. Every other feature is unaffected.

**The Vulkan backend is untested.** Minecraft 26.2 ships an experimental Vulkan
renderer. Film grain uses a GLSL post-processing shader that may not work on
that path.

**Modded swamps read as ordinary ground.** Biomes are classified from climate
data, which handles modded biomes well in general, but nothing in that data
distinguishes a swamp from a plains. The two vanilla swamps are recognised by
name; modded ones are not.

**Sodium's Fog Occlusion setting can cause chunks to pop in.** This affects any
mod that changes fog, not just this one. If you see chunks appearing abruptly
as you walk, turn off Fog Occlusion in Sodium's video settings.

**Performance has not been formally measured.** The mod is built to be cheap:
it reuses the position sampling Minecraft already performs rather than scanning
the world, and the per-frame work is arithmetic rather than lookups. But that is
a design claim, not a benchmark, and it has not been profiled.

**No ambient environmental sounds.** Birds, insects, cave drips and the like
were planned and cut. Wind is the only sound the mod adds.

**No grass or foliage sway.** Vanilla has no foliage animation to build on, so
this would require custom block rendering. Out of scope.

---

## Building from source

```
gradlew build
```

The jar lands in `build/libs/`. To run a development client:

```
gradlew runClient
```

Requires JDK 25. Gradle itself is fetched by the wrapper, so nothing else needs
installing.

---

## Credits

The wind ambience audio is not original work. See `CREDITS.md` for its source
and licence.

Everything else is original.

## Licence

MIT. See [LICENSE](LICENSE).

The MIT licence covers this mod's own code and assets. Bundled third-party
assets are covered by their own licences, listed in `CREDITS.md`.

## Development notes

[`docs/DEVLOG.md`](docs/DEVLOG.md) records every bug and obstacle hit while
building this, with causes and fixes. Much of it concerns changes in Minecraft
26.x that break older tutorials: the game is no longer obfuscated, Yarn mappings
are deprecated, screen handling moved off `Minecraft`, and day time was replaced
by a clock system.

[`docs/research/`](docs/research) holds what was verified about how vanilla
actually implements fog and falling leaves, read from decompiled sources rather
than taken from tutorials.
