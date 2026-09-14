# Falling leaves in Minecraft 26.2

Verified on 2026-09-15 against decompiled 26.2 sources.

## Vanilla already implements this feature

`LeavesBlock.animateTick` calls `makeFallingLeavesParticles`, which rolls
against a per species chance and spawns a leaf below the block:

```java
private void makeFallingLeavesParticles(Level level, BlockPos pos, RandomSource random,
                                        BlockState belowState, BlockPos below) {
    if (!(random.nextFloat() >= this.leafParticleChance)) {
        if (!isFaceFull(belowState.getCollisionShape(level, below), Direction.UP)) {
            this.spawnFallingLeavesParticle(level, pos, random);
        }
    }
}
```

Chances, from `Blocks`:

| Leaf type | Chance |
| --- | --- |
| Most leaves | 0.01 |
| Pale oak | 0.02 |
| Cherry | 0.1 |

`FallingLeavesParticle` is not a token effect either:

```java
lifetime = 300;                                      // 15 seconds
rotSpeed, spinAcceleration                           // tumbling
windBig, swirl, swirlPeriod                          // sideways drift and swirl
friction = 1.0F;
gravity = fallAcceleration * 1.2F * 0.0025F;
```

Colour comes from the biome:

```java
ColorParticleOption.create(ParticleTypes.TINTED_LEAVES, level.getClientLeafTintColor(pos))
```

Three subclasses select the particle: `TintedParticleLeavesBlock`,
`UntintedParticleLeavesBlock`, and `MangroveLeavesBlock`.

## What was missing

1. The rate is fixed and a player cannot change it.
2. `windBig` is randomised per particle rather than shared, so leaves in the
   same place drift in unrelated directions. There is no world wind.
3. Leaves only spawn directly below their own block.

## What this mod adds

Only item 1. A frequency multiplier applied over vanilla's per species chance,
so the relative difference between cherry and oak is preserved.

Item 2 belongs to the wind system. Building a leaf drift model here would be
replaced by it, so leaves are left alone until wind exists.

## Implementation

`LeavesBlockMixin` injects at the head of `makeFallingLeavesParticles`. At a
multiplier of exactly 1 it returns without cancelling, so vanilla runs
untouched rather than merely equivalently. Otherwise it cancels and reproduces
the logic with a scaled chance.

Vanilla's single roll caps output at one leaf per sampled position, which would
make any large multiplier meaningless. `FallingLeafController.rollSpawnCount`
splits the scaled chance into a whole part and a remainder, so a scaled chance
of 2.5 spawns two leaves always and a third half the time.
