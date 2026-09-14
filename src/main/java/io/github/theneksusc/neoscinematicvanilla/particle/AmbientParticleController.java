package io.github.theneksusc.neoscinematicvanilla.particle;

import io.github.theneksusc.neoscinematicvanilla.NeosCinematicVanillaClient;
import io.github.theneksusc.neoscinematicvanilla.config.CinematicConfig;
import io.github.theneksusc.neoscinematicvanilla.config.ParticleSettings;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.BiomeTags;
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
 * <h2>Motes fill a volume, not a line</h2>
 *
 * <p>Minecraft cannot draw a beam of light, so a shaft has to be implied by
 * what floats in it. Each spawn scatters motes through a slanted cylinder
 * rather than along a line: a position along the axis, then a random offset
 * within a radius of it. A line reads as a row of dots, whereas a filled
 * volume reads as air catching light.
 *
 * <h2>Why this particle</h2>
 *
 * <p>Steady state count is spawn rate multiplied by lifetime, and lifetime is
 * the scarce resource here. {@code DUST} accepts a colour and a size but
 * divides its lifetime by that size, leaving small motes alive for well under a
 * second. {@code SPORE_BLOSSOM_AIR} lives 500 to 1000 ticks but its provider
 * hardcodes a green tint. {@code WHITE_ASH} lives 20 to 100 ticks with no
 * colour override and zero gravity, which is the longest any neutral mote
 * lasts, so it is used everywhere despite offering no size control.
 *
 * <p>Its provider also ignores the velocity passed to it and generates a slow
 * drift of its own, which is why zero velocity is passed here.
 */
public final class AmbientParticleController {

	/**
	 * Chance per sampled position that the real checks run at all. Sized
	 * against vanilla's 1334 samples per tick to leave roughly eighty
	 * candidates per second before conditions are applied.
	 */
	private static final float SAMPLE_GATE = 0.003F;

	/**
	 * Chance a surviving candidate becomes a shaft, per source. Low because
	 * each shaft is now many motes rather than one.
	 */
	private static final float CAVE_SHAFT_CHANCE = 0.09F;
	private static final float FOREST_SHAFT_CHANCE = 0.10F;
	private static final float JUNGLE_SHAFT_CHANCE = 0.14F;

	/** Sky light at or below which a position counts as enclosed. */
	private static final int ENCLOSED_SKY_LIGHT = 3;

	/** Elevation at or above which enclosed spaces are buildings rather than caves. */
	private static final int CAVE_CEILING_Y = 58;

	/**
	 * Sky light at or above which a canopy position counts as open enough for
	 * motes. Keeps them in clearings and among leaves rather than inside trunks.
	 */
	private static final int CANOPY_MIN_SKY_LIGHT = 10;

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

		tryCanopyShaft(level, pos, skyLight, random, config);
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

		spawnVolume(level, pos, random, config, 14, 22, 5.0, 1.3, 0.10);
		return true;
	}

	/**
	 * Canopy shafts. Jungles get denser and narrower columns than other wooded
	 * biomes, matching how much more closed a jungle canopy is.
	 */
	private static void tryCanopyShaft(
			ClientLevel level, BlockPos pos, int skyLight, RandomSource random, ParticleSettings config) {

		if (skyLight < CANOPY_MIN_SKY_LIGHT) {
			return;
		}

		var biome = level.getBiome(pos);

		if (biome.is(BiomeTags.IS_JUNGLE)) {
			if (random.nextFloat() < JUNGLE_SHAFT_CHANCE * config.jungleRayDensity * config.density) {
				spawnVolume(level, pos, random, config, 18, 28, 7.0, 0.85, 0.32);
			}

			return;
		}

		if (!biome.is(BiomeTags.IS_FOREST) && !biome.is(BiomeTags.IS_TAIGA)) {
			return;
		}

		if (random.nextFloat() < FOREST_SHAFT_CHANCE * config.forestMoteDensity * config.density) {
			spawnVolume(level, pos, random, config, 12, 20, 6.0, 1.0, 0.26);
		}
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
			int minMotes,
			int maxMotes,
			double length,
			double radius,
			double maxLean) {

		int count = minMotes + random.nextInt(maxMotes - minMotes + 1);

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
					ParticleTypes.WHITE_ASH,
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
