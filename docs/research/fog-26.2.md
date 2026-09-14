# Fog in Minecraft 26.2

Verified on 2026-09-14 by decompiling 26.2 with Vineflower and reading the
classes directly. Nothing here is taken from tutorials or from mods targeting
earlier versions.

## Fog is a strategy system

```
net.minecraft.client.renderer.fog.FogRenderer
net.minecraft.client.renderer.fog.FogData
net.minecraft.client.renderer.fog.environment.FogEnvironment      abstract base
  AtmosphericFogEnvironment      normal air
  WaterFogEnvironment
  LavaFogEnvironment
  PowderedSnowFogEnvironment
  BlindnessFogEnvironment
  DarknessFogEnvironment
  MobEffectFogEnvironment
```

Vanilla selects one environment through `isApplicable(FogType, Entity)` and
calls its `setupFog`. Hooking only `AtmosphericFogEnvironment` therefore leaves
water, lava, powder snow, blindness, and darkness untouched with no explicit
guarding.

Verified signatures:

```java
// FogRenderer
FogData setupFog(Camera, int renderDistance, DeltaTracker, float skyDarkness, ClientLevel)
void updateBuffer(FogData)

// FogEnvironment
abstract void setupFog(FogData, Camera, ClientLevel, float renderDistance, DeltaTracker)
int getBaseColor(ClientLevel, Camera, int renderDistance, float partialTicks)
abstract boolean isApplicable(FogType, Entity)
```

`FogData` is a mutable class with public fields:

```java
float environmentalStart, environmentalEnd;    // atmospheric haze band
float renderDistanceStart, renderDistanceEnd;  // distance falloff
float skyEnd, cloudEnd;                        // sky and cloud fade points
Vector4f color;
```

## Fog values come from a data-driven attribute system

`AtmosphericFogEnvironment.setupFog` does not compute fog from hardcoded
constants. It reads them from environment attributes:

```java
fog.environmentalStart = camera.attributeProbe().getValue(EnvironmentAttributes.FOG_START_DISTANCE, partialTicks);
fog.environmentalEnd   = camera.attributeProbe().getValue(EnvironmentAttributes.FOG_END_DISTANCE, partialTicks);
fog.skyEnd             = Math.min(renderDistance, ...SKY_FOG_END_DISTANCE...);
fog.cloudEnd           = Math.min(options.cloudRange() * 16, ...CLOUD_FOG_END_DISTANCE...);
```

The system lives in `net.minecraft.world.attribute`:

- `Registries.ENVIRONMENT_ATTRIBUTE` is a real registry.
- `EnvironmentAttributeMap` has `CODEC`, `NETWORK_CODEC`, and
  `CODEC_ONLY_POSITIONAL`, so attribute maps are serialised from data and
  synced to clients.
- `Biome.getAttributes()` returns an `EnvironmentAttributeMap`, so per-biome
  fog is already a vanilla data feature.
- `EnvironmentAttributeLayer` has `Constant`, `TimeBased`, and `Positional`
  implementations, so attributes can already vary with time of day and with
  position.
- `EnvironmentAttributeMap.applyModifier(attribute, value)` exists, so layers
  compose rather than overwrite.
- `EnvironmentAttributeProbe.tick(Level, Vec3)` samples and blends as the
  player moves; `getValue(attribute, partialTicks)` reads the interpolated
  result.

Attributes relevant to this mod's roadmap:

| Attribute | Feature it touches |
| --- | --- |
| `FOG_COLOR`, `FOG_START_DISTANCE`, `FOG_END_DISTANCE` | dynamic fog |
| `SKY_FOG_END_DISTANCE`, `CLOUD_FOG_END_DISTANCE` | dynamic fog |
| `AMBIENT_PARTICLES` | ambient particles |
| `AMBIENT_SOUNDS`, `BACKGROUND_MUSIC`, `MUSIC_VOLUME` | ambient sound |
| `SKY_COLOR`, `SUNRISE_SUNSET_COLOR`, `CLOUD_COLOR` | atmosphere |

## Consequences for this mod

Vanilla 26.2 already varies fog by biome, by time of day, and by rain, and it
already interpolates smoothly as the player moves between biomes. Building a
parallel system would duplicate it and look worse.

The mod's remaining value is therefore narrower and clearer:

1. Altitude response, which vanilla does not do.
2. Underground and cave density, beyond vanilla's handling.
3. Stronger, tunable weather response.
4. User configuration, which vanilla exposes only through data packs.

Rain fog is already implemented in `AtmosphericFogEnvironment` via a smoothed
`rainFogMultiplier` that offsets start by -160 and end by -256 at full rain,
scaled by sky light and whether the biome has precipitation.

## Fabric API provides no fog hook

Every Fabric API jar was searched. No fog classes exist. A Mixin is required.

## Chosen approach

Inject at the tail of `AtmosphericFogEnvironment.setupFog` and scale the values
vanilla produced, rather than replacing them. This runs once per frame, is
limited to normal air, and keeps every vanilla behaviour as the baseline.

Fog colour is handled separately through `getBaseColor`.

Rejected alternatives:

- Mixin `EnvironmentAttributeProbe.getValue`. One hook would cover fog,
  particles, and sounds, but it is a hot path called many times per frame for
  every attribute, and a generic hook there is a performance risk.
- Mixin `FogRenderer.setupFog`. Would require re-checking submersion state that
  vanilla has already determined.
- Ship a data pack. Data packs are server-side, so this would not work on
  servers the player does not control.

## Sodium compatibility

Sodium's Fog Occlusion setting culls chunks it believes are hidden by fog.
Custom fog invalidates that assumption and causes chunks to pop in. Documented
on Sodium's issue tracker against another custom fog mod.

Design rule: widening fog and shifting colour are safe. Making fog
significantly denser than vanilla close to the player is not.

## Correction: the Overworld does not use atmospheric fog

An earlier reading of this system overstated what vanilla does. Verified
defaults, from `EnvironmentAttributes`:

```java
FOG_START_DISTANCE   defaultValue(0.0F)      spatiallyInterpolated, syncable
FOG_END_DISTANCE     defaultValue(1024.0F)   spatiallyInterpolated, syncable
SKY_FOG_END_DISTANCE defaultValue(512.0F)
CLOUD_FOG_END_DISTANCE defaultValue(2048.0F)
```

Every vanilla assignment of the atmospheric fog distances:

- `DimensionTypes` sets the Nether to `FOG_START_DISTANCE = 10.0F` and
  `FOG_END_DISTANCE = 96.0F`.
- `OverworldBiomes` modifies only `WATER_FOG_END_DISTANCE`, on two biomes.

No Overworld biome sets atmospheric fog. A 0 to 1024 band is effectively no
fog. Runtime confirmation: 107 samples across 56 blocks of elevation change in
a surface overworld world reported `envStart=0.0 envEnd=1024.0` without
variation.

Distance fog visible in the Overworld comes instead from `FogRenderer.setupFog`,
which overwrites the render distance band after the environment runs:

```java
float renderDistanceFogSpan = Mth.clamp(renderDistanceInBlocks / 10.0F, 4.0F, 64.0F);
fog.renderDistanceStart = renderDistanceInBlocks - renderDistanceFogSpan;
fog.renderDistanceEnd = renderDistanceInBlocks;
```

At 16 chunks that is a 25.6 block fade ending at 256 blocks, the edge-of-view
fade, unrelated to atmosphere.

### Consequences

1. **Multiplying vanilla's values does not work.** Scaling a 0 to 1024 band
   produces no visible change. The mod has to introduce a band in the
   Overworld, not scale one.
2. **Vanilla's band must not be stomped where it is deliberately set.** The
   Nether's 10 to 96 is an intentional look. Treat `environmentalEnd` below the
   1024 default as a signal that something set it on purpose, and leave it
   alone unless the user opts in.
3. **Render distance fog stays untouched.** It is set after this mixin runs and
   is the exact band Sodium's fog occlusion reasons about.

### Revised rule

```
if vanilla's environmental band is active (end < default):
    leave it alone by default
else:
    compute a band from altitude, cave depth, and weather, then apply it
```

Defaults must be subtle but non-zero, since a multiplier-only design would
leave the mod doing nothing in the Overworld.
