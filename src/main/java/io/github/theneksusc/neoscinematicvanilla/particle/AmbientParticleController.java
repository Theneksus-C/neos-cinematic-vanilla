package io.github.theneksusc.neoscinematicvanilla.particle;

import io.github.theneksusc.neoscinematicvanilla.NeosCinematicVanillaClient;
import io.github.theneksusc.neoscinematicvanilla.config.CinematicConfig;
import io.github.theneksusc.neoscinematicvanilla.config.ParticleSettings;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Spawns sparse ambient motes in the air around the player.
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
 * <h2>Motes arrive in shafts, not singly</h2>
 *
 * <p>Every source spawns a short descending line of fine motes rather than one
 * mote at a time. A lone particle reads as an artefact; a cluster following a
 * common line reads as light catching dust in the air. Each shaft picks one
 * lean, so every mote in it follows the same angle, with slight sideways
 * scatter so the line is not ruler straight.
 *
 * <h2>Why the rates look high</h2>
 *
 * <p>Steady state count is spawn rate multiplied by lifetime, so a short lived
 * particle needs a high rate to stay visible. {@code DUST} lives 8 to 40 ticks
 * scaled by its size, meaning well under a second at these sizes. Shafts
 * therefore shimmer and disperse rather than persisting, and the number alive
 * at any moment stays low despite the throughput.
 *
 * <p>{@code DUST} is also the one particle type that accepts both a packed RGB
 * colour and a scale. An earlier version used {@code SPORE_BLOSSOM_AIR}, whose
 * provider hardcodes a green tint that cannot be overridden.
 */
public final class AmbientParticleController {

	/**
	 * Chance per sampled position that the real checks run at all. Sized
	 * against vanilla's 1334 samples per tick to leave roughly eighty
	 * candidates per second before conditions are applied.
	 */
	private static final float SAMPLE_GATE = 0.003F;

	/** Chance a surviving candidate becomes a shaft, per source. */
	private static final float CAVE_SHAFT_CHANCE = 0.30F;
	private static final float FOREST_SHAFT_CHANCE = 0.16F;
	private static final float JUNGLE_SHAFT_CHANCE = 0.10F;

	/** Sky light at or below which a position counts as enclosed. */
	private static final int ENCLOSED_SKY_LIGHT = 3;

	/** Elevation at or above which enclosed spaces are buildings rather than caves. */
	private static final int CAVE_CEILING_Y = 58;

	/**
	 * Sky light at or above which a canopy position counts as open enough for
	 * motes. Keeps them in clearings and among leaves rather than inside trunks.
	 */
	private static final int CANOPY_MIN_SKY_LIGHT = 10;

	/** Neutral grey white for underground dust, with no warmth since there is no sun. */
	private static final int CAVE_COLOR = 0xD5D5CC;

	/** Warm off white above ground, suggesting motes catching sunlight rather than glowing. */
	private static final int SUNLIT_COLOR = 0xF6EBCB;

	/** Sideways scatter of each mote around its shaft line. */
	private static final double SHAFT_JITTER = 0.09;

	/** Spacing between motes along a shaft. */
	private static final double SHAFT_SPACING = 0.55;

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
	 * Dust falling through deep enclosed spaces. Gated on depth as well as sky
	 * light so that cellars and interiors at surface level stay clear. Kept
	 * close to vertical, since there is no sun underground to angle it.
	 */
	private static boolean tryCaveShaft(
			ClientLevel level, BlockPos pos, int skyLight, RandomSource random, ParticleSettings config) {

		if (skyLight > ENCLOSED_SKY_LIGHT || pos.getY() > CAVE_CEILING_Y) {
			return false;
		}

		if (random.nextFloat() >= CAVE_SHAFT_CHANCE * config.caveDustDensity * config.density) {
			return false;
		}

		spawnShaft(level, pos, random, config, CAVE_COLOR, 0.70F, 5, 9, 0.12);
		return true;
	}

	/**
	 * Canopy shafts. Jungles get denser, finer shafts than other wooded biomes,
	 * matching how much more closed a jungle canopy is.
	 */
	private static void tryCanopyShaft(
			ClientLevel level, BlockPos pos, int skyLight, RandomSource random, ParticleSettings config) {

		if (skyLight < CANOPY_MIN_SKY_LIGHT) {
			return;
		}

		var biome = level.getBiome(pos);

		if (biome.is(BiomeTags.IS_JUNGLE)) {
			if (random.nextFloat() < JUNGLE_SHAFT_CHANCE * config.jungleRayDensity * config.density) {
				spawnShaft(level, pos, random, config, SUNLIT_COLOR, 0.45F, 10, 16, 0.35);
			}

			return;
		}

		if (!biome.is(BiomeTags.IS_FOREST) && !biome.is(BiomeTags.IS_TAIGA)) {
			return;
		}

		if (random.nextFloat() < FOREST_SHAFT_CHANCE * config.forestMoteDensity * config.density) {
			spawnShaft(level, pos, random, config, SUNLIT_COLOR, 0.50F, 7, 11, 0.28);
		}
	}

	/**
	 * Places a short line of motes descending at a slight lean.
	 *
	 * <p>The motes are placed, not simulated. Each is small enough that its own
	 * lifetime is well under a second, so a shaft shimmers and disperses rather
	 * than persisting as a visible object.
	 *
	 * @param scale    mote size, which also divides mote lifetime
	 * @param maxLean  horizontal drift per block of descent
	 */
	private static void spawnShaft(
			ClientLevel level,
			BlockPos pos,
			RandomSource random,
			ParticleSettings config,
			int color,
			float scale,
			int minMotes,
			int maxMotes,
			double maxLean) {

		DustParticleOptions mote = new DustParticleOptions(color, scale);

		int count = minMotes + random.nextInt(maxMotes - minMotes + 1);

		// One lean per shaft, so every mote in it follows the same line.
		double leanX = (random.nextDouble() * 2.0 - 1.0) * maxLean;
		double leanZ = (random.nextDouble() * 2.0 - 1.0) * maxLean;

		double x = pos.getX() + random.nextDouble();
		double y = pos.getY() + random.nextDouble();
		double z = pos.getZ() + random.nextDouble();

		for (int i = 0; i < count; i++) {
			double along = i * SHAFT_SPACING;

			level.addParticle(
					mote,
					x + leanX * along + (random.nextDouble() * 2.0 - 1.0) * SHAFT_JITTER,
					y - along,
					z + leanZ * along + (random.nextDouble() * 2.0 - 1.0) * SHAFT_JITTER,
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
