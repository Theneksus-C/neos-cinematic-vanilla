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
