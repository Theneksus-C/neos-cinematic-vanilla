# Neo's Cinematic Vanilla

A client-side Minecraft mod that adds atmospheric effects to the vanilla world:
dynamic fog, ambient particles, falling leaves, environmental sound, improved
weather visuals, and wind.

Gameplay, progression, world generation, and the appearance of blocks and items
are unchanged.

## Design constraints

- Effects are subtle by default.
- Everything is client-side. The mod does not need to be installed on a server.
- Features are individually configurable.
- Systems share one cached environment sample rather than each scanning the
  world independently, to limit per-frame cost.

## Planned features

1. Dynamic fog reacting to biome, weather, altitude, and time of day
2. Sparse ambient environmental particles
3. Occasional falling leaves in forests
4. Ambient environmental sounds per environment type
5. Directional and distance-based positional ambience
6. Improved rain and snow visuals
7. A subtle environmental wind system

## Identity

| Field | Value |
| --- | --- |
| Display name | Neo's Cinematic Vanilla |
| Mod ID | `neoscinematicvanilla` |
| Java package | `io.github.theneksusc.neoscinematicvanilla` |
| Repository | `neos-cinematic-vanilla` |

Mod IDs and Java packages cannot contain apostrophes, spaces, or capital
letters, so the display name and the internal identifiers differ.

## Stack

| Component | Version |
| --- | --- |
| Minecraft | 26.2 |
| Mod loader | Fabric |
| Fabric Loader | 0.19.5 |
| Fabric API | 0.160.0+26.2 |
| Fabric Loom | 1.17.20 |
| Java | 25 |
| Gradle | 9.5.1 |

Minecraft 26.1 was the first unobfuscated release, so this project uses
Mojang's official names directly. No Yarn mappings are involved.

## Building

```
gradlew build
```

Output is written to `build/libs/`.

## Running a development client

```
gradlew runClient
```

Launches Minecraft with the mod loaded. The game directory is `run/`.

## Status

Dynamic fog implemented and tuned. Altitude, cave depth, and weather drive an
atmospheric fog band, configurable at runtime. Remaining features not started.

## Documentation

- [Development log and obstacles](docs/DEVLOG.md)
