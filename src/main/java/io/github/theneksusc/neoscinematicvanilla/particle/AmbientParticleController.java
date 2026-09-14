package io.github.theneksusc.neoscinematicvanilla.particle;

import io.github.theneksusc.neoscinematicvanilla.NeosCinematicVanillaClient;
import io.github.theneksusc.neoscinematicvanilla.config.CinematicConfig;
import io.github.theneksusc.neoscinematicvanilla.config.ParticleSettings;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
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
 * <h2>Why spawn rates differ per source</h2>
 *
 * <p>Steady state particle count is spawn rate multiplied by lifetime, so the
 * particle chosen dictates the rate. {@code WHITE_ASH} lives 20 to 100 ticks,
 * so a few dozen spawns per second settle at under a hundred alive.
 * {@code DUST} lives 8 to 40 ticks scaled by its size, so a shaft of a dozen
 * fine motes disperses in well under a second and costs very little despite
 * spawning many at once.
 *
 * <p>An earlier version used {@code SPORE_BLOSSOM_AIR}, which lives 500 to 1000
 * ticks. That needed a far lower rate, and its provider hardcodes a green tint
 * that cannot be overridden because it is a simple particle type.
 */
public final class AmbientParticleController {

	/**
	 * Chance per sampled position that the real checks run at all. Sized
	 * against vanilla's 1334 samples per tick to leave roughly eighty
	 * candidates per second before conditions are applied.
	 */
	private static final float SAMPLE_GATE = 0.003F;

	/** Chance a surviving candidate becomes a mote, per source. */
	private static final float CAVE_SPAWN_CHANCE = 0.65F;
	private static final float FOREST_SPAWN_CHANCE = 0.20F;
	private static final float JUNGLE_RAY_CHANCE = 0.035F;

	/** Sky light at or below which a position counts as enclosed. */
	private static final int ENCLOSED_SKY_LIGHT = 3;

	/** Elevation at or above which enclosed spaces are buildings rather than caves. */
	private static final int CAVE_CEILING_Y = 58;

	/**
	 * Sky light at or above which a canopy position counts as open enough for
	 * motes. Keeps them in clearings and among leaves rather than inside trunks.
	 */
	private static final int CANOPY_MIN_SKY_LIGHT = 10;

	/** Warm off white, suggesting motes catching sunlight rather than glowing. */
	private static final int SHAFT_COLOR = 0xF6EBCB;

	/** Size multiplier for shaft motes. Also scales their lifetime, keeping them brief. */
	private static final float SHAFT_SCALE = 0.5F;

	/** Number of motes in one shaft, and how far apart they sit along it. */
	private static final int SHAFT_MIN_MOTES = 8;
	private static final int SHAFT_MAX_MOTES = 14;
	private static final double SHAFT_SPACING = 0.55;

	/** Horizontal lean of a shaft, so it reads as angled light rather than a column. */
	private static final double SHAFT_MAX_LEAN = 0.35;

	/** Sideways scatter of each mote around the shaft line. */
	private static final double SHAFT_JITTER = 0.09;

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

		if (trySpawnCaveDust(level, pos, skyLight, random, config)) {
			return;
		}

		trySpawnCanopyEffect(level, pos, skyLight, random, config);
	}

	/**
	 * Dust hanging in deep enclosed spaces. Gated on depth as well as sky light
	 * so that cellars and interiors at surface level stay clear.
	 */
	private static boolean trySpawnCaveDust(
			ClientLevel level, BlockPos pos, int skyLight, RandomSource random, ParticleSettings config) {

		if (skyLight > ENCLOSED_SKY_LIGHT || pos.getY() > CAVE_CEILING_Y) {
			return false;
		}

		if (random.nextFloat() >= CAVE_SPAWN_CHANCE * config.caveDustDensity * config.density) {
			return false;
		}

		level.addParticle(
				ParticleTypes.WHITE_ASH,
				pos.getX() + random.nextDouble(),
				pos.getY() + random.nextDouble(),
				pos.getZ() + random.nextDouble(),
				0.0,
				0.0,
				0.0);

		countSpawn(config, 1);
		return true;
	}

	/**
	 * Canopy effects. Jungles get shafts of fine motes; other wooded biomes get
	 * occasional single motes.
	 */
	private static void trySpawnCanopyEffect(
			ClientLevel level, BlockPos pos, int skyLight, RandomSource random, ParticleSettings config) {

		if (skyLight < CANOPY_MIN_SKY_LIGHT) {
			return;
		}

		var biome = level.getBiome(pos);

		if (biome.is(BiomeTags.IS_JUNGLE)) {
			if (random.nextFloat() < JUNGLE_RAY_CHANCE * config.jungleRayDensity * config.density) {
				spawnLightShaft(level, pos, random, config);
			}

			return;
		}

		if (!biome.is(BiomeTags.IS_FOREST) && !biome.is(BiomeTags.IS_TAIGA)) {
			return;
		}

		if (random.nextFloat() >= FOREST_SPAWN_CHANCE * config.forestMoteDensity * config.density) {
			return;
		}

		level.addParticle(
				ParticleTypes.WHITE_ASH,
				pos.getX() + random.nextDouble(),
				pos.getY() + random.nextDouble(),
				pos.getZ() + random.nextDouble(),
				0.0,
				0.0,
				0.0);

		countSpawn(config, 1);
	}

	/**
	 * Spawns a short line of fine motes descending at a slight lean, which
	 * reads as light angling through the canopy.
	 *
	 * <p>The motes are placed rather than simulated. Each is small enough that
	 * its own lifetime is under a second, so a shaft shimmers and disperses
	 * instead of persisting as a visible object.
	 */
	private static void spawnLightShaft(ClientLevel level, BlockPos pos, RandomSource random, ParticleSettings config) {
		DustParticleOptions mote = new DustParticleOptions(SHAFT_COLOR, SHAFT_SCALE);

		int count = SHAFT_MIN_MOTES + random.nextInt(SHAFT_MAX_MOTES - SHAFT_MIN_MOTES + 1);

		// One lean per shaft, so every mote in it follows the same line.
		double leanX = (random.nextDouble() * 2.0 - 1.0) * SHAFT_MAX_LEAN;
		double leanZ = (random.nextDouble() * 2.0 - 1.0) * SHAFT_MAX_LEAN;

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
