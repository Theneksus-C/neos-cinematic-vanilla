package io.github.theneksusc.neoscinematicvanilla.particle;

import io.github.theneksusc.neoscinematicvanilla.NeosCinematicVanillaClient;
import io.github.theneksusc.neoscinematicvanilla.config.BiomeSettings;
import io.github.theneksusc.neoscinematicvanilla.config.CinematicConfig;
import io.github.theneksusc.neoscinematicvanilla.config.ParticleSettings;
import io.github.theneksusc.neoscinematicvanilla.world.BiomeCharacter;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Spawns drifting motes suspended in the air around the player.
 *
 * <p>This runs no loop of its own. Vanilla already samples 1334 random
 * positions around the player every client tick inside
 * {@code ClientLevel.doAnimateTick}, to drive block animations, drips, and the
 * Nether's ambient particles. This class is invoked at the tail of that method
 * and reuses the position vanilla already chose, so it adds no world scanning
 * and no extra random position generation.
 *
 * <p>Because it runs roughly 27000 times per second, the first statement is a
 * single random comparison that rejects almost every call. Everything past that
 * gate runs a few dozen times a second and can afford a biome lookup.
 *
 * <h2>Where motes appear, and what colour</h2>
 *
 * <p>Surface motes used to appear only under a canopy. They now appear wherever
 * the place has a character worth showing: dust in arid country, blown snow in
 * frozen country, shafts under a canopy. Each character sets its own rate,
 * shape, and colour, which is what stops one biome feeling like the next.
 *
 * <p>Classification uses the sampled position rather than the player's, so a
 * mote spawning across a border takes its own biome's colour. That costs one
 * biome lookup per surviving candidate, a few dozen a second, not per sample.
 *
 * <p>Caves are deliberately exempt. Underground the biome overhead says nothing
 * about the air, so cave dust keeps one neutral grey rather than turning sandy
 * because a desert happens to be above.
 *
 * <h2>Motes fill a volume, not a line</h2>
 *
 * <p>Minecraft cannot draw a beam of light, so a shaft has to be implied by
 * what floats in it. Each spawn scatters motes through a slanted cylinder
 * rather than along a line: a position along the axis, then a random offset
 * within a radius of it. A line reads as a row of dots, whereas a filled
 * volume reads as air catching light.
 */
public final class AmbientParticleController {

	/**
	 * Chance per sampled position that the real checks run at all. Sized
	 * against vanilla's 1334 samples per tick to leave roughly eighty
	 * candidates per second before conditions are applied.
	 */
	private static final float SAMPLE_GATE = 0.003F;

	/** Chance a surviving candidate becomes a shaft, per source. */
	private static final float CAVE_SHAFT_CHANCE = 0.011F;

	/** Sky light at or below which a position counts as enclosed. */
	private static final int ENCLOSED_SKY_LIGHT = 3;

	/** Elevation at or above which enclosed spaces are buildings rather than caves. */
	private static final int CAVE_CEILING_Y = 58;

	/**
	 * Sky light at or above which a surface position counts as open enough for
	 * motes. Keeps them out of trunks and from under dense cover.
	 */
	private static final int SURFACE_MIN_SKY_LIGHT = 10;

	/** Neutral grey for underground dust, which no biome above should tint. */
	private static final int CAVE_COLOR = 0xD5D5CC;

	/** Interval between diagnostic lines. */
	private static final long DEBUG_INTERVAL_MILLIS = 5000L;

	private static int spawnsSinceReport;
	private static long lastReportMillis;

	private AmbientParticleController() {
	}

	/**
	 * Called once per position vanilla samples.
	 *
	 * <p>Vanilla has already looked up the block state here, but reading its
	 * local would tie this mixin to the exact shape of vanilla's method body.
	 * Looking it up again is more robust and costs nothing, because it happens
	 * only after the gate has rejected all but a few dozen calls per second.
	 */
	public static void onSample(ClientLevel level, BlockPos pos, RandomSource random) {
		// Cheapest possible rejection, and it rejects almost every call.
		if (random.nextFloat() >= SAMPLE_GATE) {
			return;
		}

		ParticleSettings config = CinematicConfig.particles();

		if (!config.enabled || config.density <= 0.0F) {
			return;
		}

		BlockState state = level.getBlockState(pos);

		// Motes belong in open air. Anything else would bury them inside blocks.
		if (!state.isAir()) {
			return;
		}

		int skyLight = level.getBrightness(LightLayer.SKY, pos);

		if (tryCaveShaft(level, pos, skyLight, random, config)) {
			return;
		}

		trySurfaceShaft(level, pos, skyLight, random, config);
	}

	/**
	 * Dust suspended in deep enclosed spaces. Gated on depth as well as sky
	 * light so that cellars and interiors at surface level stay clear. Wider and
	 * closer to vertical than a sunlit shaft, since nothing underground angles
	 * the light.
	 */
	private static boolean tryCaveShaft(
			ClientLevel level, BlockPos pos, int skyLight, RandomSource random, ParticleSettings config) {

		if (skyLight > ENCLOSED_SKY_LIGHT || pos.getY() > CAVE_CEILING_Y) {
			return false;
		}

		if (random.nextFloat() >= CAVE_SHAFT_CHANCE * config.caveDustDensity * config.density) {
			return false;
		}

		spawnVolume(level, pos, random, config, CAVE_COLOR, 14, 22, 5.0, 1.3, 0.10);
		return true;
	}

	/**
	 * Motes in open air, shaped and coloured by the character of the place.
	 *
	 * <p>Temperate ground gets almost nothing on purpose. If everywhere had
	 * motes there would be nothing for a desert or a snowfield to feel different
	 * from.
	 */
	private static void trySurfaceShaft(
			ClientLevel level, BlockPos pos, int skyLight, RandomSource random, ParticleSettings config) {

		if (skyLight < SURFACE_MIN_SKY_LIGHT) {
			return;
		}

		BiomeCharacter character = BiomeCharacter.of(level, level.getBiome(pos), pos);
		BiomeSettings biomes = CinematicConfig.biomes();

		float chance = baseChanceFor(character)
				* biomes.apply(character, character.particleMultiplier())
				* config.surfaceMoteDensity
				* config.density;

		if (random.nextFloat() >= chance) {
			return;
		}

		switch (character) {
			// Dust hangs in a wide, nearly shapeless body of air rather than in
			// a shaft, and leans hard because arid country is exposed.
			case ARID -> spawnVolume(level, pos, random, config, character.moteColor(), 16, 26, 6.5, 2.2, 0.45);

			// Blown snow is scattered and near horizontal rather than columnar.
			case FROZEN -> spawnVolume(level, pos, random, config, character.moteColor(), 14, 24, 7.0, 2.0, 0.55);

			// The canopy shaft, narrow and defined, as before.
			case WOODED -> spawnVolume(level, pos, random, config, character.moteColor(), 16, 26, 7.0, 0.9, 0.30);

			// Thick still air. Barely moving, low and close.
			case SWAMP -> spawnVolume(level, pos, random, config, character.moteColor(), 10, 16, 4.0, 1.8, 0.08);

			default -> spawnVolume(level, pos, random, config, character.moteColor(), 8, 14, 5.5, 1.4, 0.25);
		}
	}

	/** How often each character produces motes at all, before any configuration. */
	private static float baseChanceFor(BiomeCharacter character) {
		return switch (character) {
			case WOODED -> 0.018F;
			case ARID -> 0.016F;
			case FROZEN -> 0.015F;
			case SWAMP -> 0.010F;
			// Deliberately sparse, so that somewhere with character reads as
			// different rather than as merely more of the same.
			case HIGHLAND -> 0.003F;
			case TEMPERATE -> 0.004F;
		};
	}

	/**
	 * Scatters motes through a slanted cylinder, filling a volume rather than
	 * tracing a line.
	 *
	 * <p>Each mote takes an independent position along the axis rather than an
	 * evenly spaced one, so the result looks like suspended dust instead of a
	 * repeating pattern. The shaft picks one lean, so all of its motes share the
	 * same angle.
	 *
	 * @param length  how far the shaft descends, in blocks
	 * @param radius  how far motes scatter from the axis
	 * @param maxLean horizontal drift per block of descent
	 */
	private static void spawnVolume(
			ClientLevel level,
			BlockPos pos,
			RandomSource random,
			ParticleSettings config,
			int color,
			int minMotes,
			int maxMotes,
			double length,
			double radius,
			double maxLean) {

		ColorParticleOption mote = ColorParticleOption.create(ModParticles.DUST_MOTE, color);

		// Shape and size come from the baseline for this source, scaled by the
		// user's multipliers, so taste is expressed without needing to know the
		// tuned numbers underneath.
		int baseCount = minMotes + random.nextInt(maxMotes - minMotes + 1);
		int count = Math.max(1, Math.round(baseCount * Math.max(0.0F, config.shaftDensity)));

		length *= Math.max(0.0, config.shaftLength);
		radius *= Math.max(0.0, config.shaftRadius);
		maxLean *= Math.max(0.0, config.shaftLean);

		// One lean per shaft, so every mote in it follows the same angle.
		double leanX = (random.nextDouble() * 2.0 - 1.0) * maxLean;
		double leanZ = (random.nextDouble() * 2.0 - 1.0) * maxLean;

		double originX = pos.getX() + random.nextDouble();
		double originY = pos.getY() + random.nextDouble();
		double originZ = pos.getZ() + random.nextDouble();

		for (int i = 0; i < count; i++) {
			double along = random.nextDouble() * length;

			// Offsets are drawn independently rather than as a true disc, which
			// is cheaper and indistinguishable once motes are this scattered.
			double offsetX = (random.nextDouble() * 2.0 - 1.0) * radius;
			double offsetZ = (random.nextDouble() * 2.0 - 1.0) * radius;

			level.addParticle(
					mote,
					originX + leanX * along + offsetX,
					originY - along,
					originZ + leanZ * along + offsetZ,
					0.0,
					0.0,
					0.0);
		}

		countSpawn(config, count);
	}

	/** Accumulates a spawn count and reports a rate periodically when asked to. */
	private static void countSpawn(ParticleSettings config, int amount) {
		if (!config.debugLogging) {
			return;
		}

		spawnsSinceReport += amount;
		long now = System.currentTimeMillis();

		if (lastReportMillis == 0L) {
			lastReportMillis = now;
			return;
		}

		long elapsed = now - lastReportMillis;

		if (elapsed >= DEBUG_INTERVAL_MILLIS) {
			NeosCinematicVanillaClient.LOGGER.info(
					"ambient particles: {} spawned in {} ms, about {} per second",
					spawnsSinceReport,
					elapsed,
					String.format("%.1f", spawnsSinceReport * 1000.0 / elapsed));

			spawnsSinceReport = 0;
			lastReportMillis = now;
		}
	}
}
