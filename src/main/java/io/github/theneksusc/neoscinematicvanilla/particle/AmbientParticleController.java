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
 * Spawns sparse ambient motes in the air around the player.
 *
 * <p>This does not run a loop of its own. Vanilla already samples 1334 random
 * positions around the player every client tick inside
 * {@code ClientLevel.doAnimateTick}, to drive block animations, drips, and the
 * Nether's ambient particles. This class is invoked at the tail of that method
 * and reuses the position vanilla already chose, so it adds no world scanning
 * and no extra random position generation.
 *
 * <p>Because it runs 1334 times per tick, roughly 27000 times per second, the
 * first statement has to be a cheap rejection. Everything past the gate runs
 * about five times a second, so it can afford a biome lookup.
 *
 * <p>Spawn rate is the number that matters for performance, not the cost of a
 * particle. The mote used here lives 500 to 1000 ticks, so two spawns per
 * second settles at roughly seventy particles alive. Twenty per second would
 * settle near seven hundred.
 */
public final class AmbientParticleController {

	/**
	 * Chance per sampled position that the expensive checks run at all. Sized
	 * against vanilla's 1334 samples per tick to leave roughly five candidates
	 * per second before conditions are applied.
	 */
	private static final float SAMPLE_GATE = 0.0002F;

	/** Chance a surviving candidate becomes a mote, per source. */
	private static final float CAVE_SPAWN_CHANCE = 0.40F;
	private static final float FOREST_SPAWN_CHANCE = 0.22F;

	/** Sky light at or below which a position counts as enclosed. */
	private static final int ENCLOSED_SKY_LIGHT = 3;

	/** Elevation at or above which enclosed spaces are buildings rather than caves. */
	private static final int CAVE_CEILING_Y = 58;

	/**
	 * Sky light at or above which a forest position counts as open enough for
	 * motes. Keeps them in clearings and among the canopy rather than inside
	 * trunks or under dense cover.
	 */
	private static final int FOREST_MIN_SKY_LIGHT = 10;

	/** Interval between diagnostic lines, in spawns. */
	private static final int DEBUG_REPORT_INTERVAL = 40;

	private static int spawnCount;

	private AmbientParticleController() {
	}

	/**
	 * Called once per position vanilla samples.
	 *
	 * <p>Vanilla has already looked up the block state at this position, but
	 * reading its local would tie this mixin to the exact shape of vanilla's
	 * method body. Looking it up again is the more robust choice and costs
	 * nothing, because it happens only after the gate has rejected all but a
	 * handful of calls per second.
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

		trySpawnForestMote(level, pos, skyLight, random, config);
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

		float chance = CAVE_SPAWN_CHANCE * config.caveDustDensity * config.density;

		if (random.nextFloat() >= chance) {
			return false;
		}

		spawn(level, pos, random, config);
		return true;
	}

	/**
	 * Motes drifting through a lit canopy. Requires real sky exposure so they
	 * appear in clearings and among leaves rather than inside tree trunks.
	 */
	private static void trySpawnForestMote(
			ClientLevel level, BlockPos pos, int skyLight, RandomSource random, ParticleSettings config) {

		if (skyLight < FOREST_MIN_SKY_LIGHT) {
			return;
		}

		var biome = level.getBiome(pos);

		if (!biome.is(BiomeTags.IS_FOREST) && !biome.is(BiomeTags.IS_TAIGA) && !biome.is(BiomeTags.IS_JUNGLE)) {
			return;
		}

		float chance = FOREST_SPAWN_CHANCE * config.forestMoteDensity * config.density;

		if (random.nextFloat() >= chance) {
			return;
		}

		spawn(level, pos, random, config);
	}

	/**
	 * Adds one mote at a random offset inside the sampled block.
	 *
	 * <p>{@code SPORE_BLOSSOM_AIR} is a suspended particle of size 0.01 with a
	 * gravity of 0.01 and no collision, which reads as a dust mote drifting
	 * rather than as an effect. Zero velocity is passed so it is carried only by
	 * its own slow fall, which the wind system will later be able to influence.
	 */
	private static void spawn(ClientLevel level, BlockPos pos, RandomSource random, ParticleSettings config) {
		level.addParticle(
				ParticleTypes.SPORE_BLOSSOM_AIR,
				pos.getX() + random.nextDouble(),
				pos.getY() + random.nextDouble(),
				pos.getZ() + random.nextDouble(),
				0.0,
				0.0,
				0.0);

		if (config.debugLogging && ++spawnCount % DEBUG_REPORT_INTERVAL == 0) {
			NeosCinematicVanillaClient.LOGGER.info("ambient particles spawned: {}", spawnCount);
		}
	}
}
