# Neo's Cinematic Vanilla

A client-side Minecraft mod that makes the vanilla world feel more physical,
atmospheric, and alive without altering gameplay, progression, building, or
Minecraft's visual identity.

## Philosophy

Vanilla Minecraft, plus atmosphere. Every effect is subtle by default. The
intended reaction is "why does this feel more alive", not "this is a different
game".

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
letters, so the display name and the internal identifiers differ by necessity.
Only the display name appears in the in-game mod list.

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

## Status

Base build working. The mod compiles, packages, and loads on Minecraft 26.2.
No atmosphere features implemented yet.

## Documentation

- [Development log and obstacles](docs/DEVLOG.md)
