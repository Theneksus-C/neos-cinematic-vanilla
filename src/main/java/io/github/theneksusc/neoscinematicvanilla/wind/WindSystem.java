package io.github.theneksusc.neoscinematicvanilla.wind;

import io.github.theneksusc.neoscinematicvanilla.NeosCinematicVanillaClient;
import io.github.theneksusc.neoscinematicvanilla.config.CinematicConfig;
import io.github.theneksusc.neoscinematicvanilla.config.WindSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LightLayer;

/**
 * A shared model of wind, producing a direction and a strength that other
 * systems read.
 *
 * <p>Nothing here renders or plays anything. The ambient loop, mote drift, and
 * leaf drift all consume {@link #strength()} and {@link #directionRadians()},
 * which is what keeps them agreeing with each other. Leaves drifting one way
 * while motes drift another would read as noise rather than weather.
 *
 * <h2>Exposure is measured, not assumed</h2>
 *
 * <p>Wind should be stronger on a peak or in open desert than on a forest floor.
 * Three signals combine to decide that, none of which scans the world:
 *
 * <ul>
 *   <li><b>Sky exposure</b> at the player, which already drives fog and motes.
 *   <li><b>Altitude</b>, ramping up with height.
 *   <li><b>Openness</b>, the fraction of nearby positions that are air. Vanilla
 *       samples 1334 random positions near the player every tick for its own
 *       animation, and the mod already hooks that loop, so counting how many
 *       were air costs one increment per sample and no scanning at all.
 * </ul>
 *
 * <p>Openness is the signal that makes a clearing windier than dense woods
 * without knowing anything about biomes.
 */
public final class WindSystem {

	/** Ticks between direction target changes, before the configured speed scales it. */
	private static final int DIRECTION_CHANGE_TICKS = 600;

	/** How quickly direction eases toward its target, per tick. */
	private static final float DIRECTION_EASE = 0.004F;

	/** How quickly strength eases toward its target, per tick. Slow, so gusts swell. */
	private static final float STRENGTH_EASE = 0.012F;

	/** Base strength in calm weather, before exposure and gust. */
	private static final float CALM_STRENGTH = 0.30F;

	/** Added at full rain, and again at full thunder. */
	private static final float RAIN_STRENGTH = 0.35F;
	private static final float THUNDER_STRENGTH = 0.30F;

	/** Depth of the slow gust cycle, as a fraction of current strength. */
	private static final float GUST_DEPTH = 0.35F;

	/** Gust cycle lengths in ticks. Two periods that do not divide evenly avoid an obvious rhythm. */
	private static final float GUST_PERIOD_A = 197.0F;
	private static final float GUST_PERIOD_B = 313.0F;

	/** Elevation at or below which altitude contributes nothing to exposure. */
	private static final float SHELTERED_Y = 62.0F;

	/** Elevation at or above which altitude contributes fully. */
	private static final float EXPOSED_Y = 150.0F;

	/** How the three exposure signals are weighted against each other. */
	private static final float SKY_WEIGHT = 0.45F;
	private static final float OPENNESS_WEIGHT = 0.35F;
	private static final float ALTITUDE_WEIGHT = 0.20F;

	/** One in this many of vanilla's samples is actually read. */
	private static final int SAMPLE_EVERY = 8;

	/** Samples needed in a tick before the openness estimate is trusted. */
	private static final int MIN_SAMPLES = 40;

	/** How quickly the openness estimate follows the incoming samples. */
	private static final float OPENNESS_EASE = 0.05F;

	/** Interval between diagnostic lines, in ticks. */
	private static final int DEBUG_INTERVAL_TICKS = 40;

	private static float direction;
	private static float directionTarget;
	private static float strength;
	private static float strengthTarget;
	private static float exposure;
	private static float openness = 0.5F;

	/** Accumulated over one tick of vanilla's sampling, then folded into openness. */
	private static int airSamples;
	private static int totalSamples;

	/** Counts every offered sample, so only every SAMPLE_EVERY th one is read. */
	private static int sampleCursor;

	private static long ageTicks;

	private WindSystem() {
	}

	/**
	 * Considers one of the positions vanilla sampled for the openness estimate.
	 *
	 * <p>Called for all 1334 of vanilla's samples per tick, but only every
	 * {@link #SAMPLE_EVERY}th one is actually read. Reading the block state at
	 * every sample would mean roughly twenty seven thousand lookups a second for
	 * a statistic that a fraction of them measures just as well: a subsample of
	 * over a hundred positions per tick, eased over time, is already far more
	 * than this needs.
	 */
	public static void sampleOpenness(ClientLevel level, BlockPos pos) {
		if (++sampleCursor % SAMPLE_EVERY != 0) {
			return;
		}

		totalSamples++;

		if (level.getBlockState(pos).isAir()) {
			airSamples++;
		}
	}

	/** Advances the model. Called once per client tick. */
	public static void tick(Minecraft client) {
		WindSettings config = CinematicConfig.wind();

		ClientLevel level = client.level;
		Player player = client.player;

		if (!config.enabled || level == null || player == null) {
			strength = 0.0F;
			resetSampleCounters();
			return;
		}

		ageTicks++;

		updateOpenness();
		updateExposure(level, player, config);
		updateDirection(config);
		updateStrength(level, config);

		if (config.debugLogging && ageTicks % DEBUG_INTERVAL_TICKS == 0) {
			NeosCinematicVanillaClient.LOGGER.info(
					"wind: strength={} direction={} exposure={} openness={}",
					String.format("%.3f", strength),
					String.format("%.0f", Math.toDegrees(direction)),
					String.format("%.2f", exposure),
					String.format("%.2f", openness));
		}
	}

	/**
	 * Folds this tick's samples into the running openness estimate.
	 *
	 * <p>Eased rather than taken outright, because a single tick's sample of a
	 * random neighbourhood is noisy, and wind that jittered with it would feel
	 * wrong.
	 */
	private static void updateOpenness() {
		if (totalSamples >= MIN_SAMPLES) {
			float measured = airSamples / (float) totalSamples;
			openness += (measured - openness) * OPENNESS_EASE;
		}

		resetSampleCounters();
	}

	private static void resetSampleCounters() {
		airSamples = 0;
		totalSamples = 0;
	}

	/** Combines sky access, openness, and altitude into a single 0 to 1 exposure. */
	private static void updateExposure(ClientLevel level, Player player, WindSettings config) {
		BlockPos pos = player.blockPosition();

		float sky = level.getBrightness(LightLayer.SKY, pos) / 15.0F;

		float y = (float) player.getY();
		float altitude = Mth.clamp((y - SHELTERED_Y) / (EXPOSED_Y - SHELTERED_Y), 0.0F, 1.0F);

		float combined = sky * SKY_WEIGHT + openness * OPENNESS_WEIGHT + altitude * ALTITUDE_WEIGHT;

		// At zero influence wind ignores surroundings entirely, which is the
		// setting for someone who just wants constant wind.
		float influence = Mth.clamp(config.exposureInfluence, 0.0F, 1.0F);
		exposure = Mth.lerp(influence, 1.0F, Mth.clamp(combined, 0.0F, 1.0F));
	}

	/**
	 * Wanders the direction toward a new random target periodically.
	 *
	 * <p>Eased through the shortest arc, so wind never snaps around or spins the
	 * long way to reach a nearby heading.
	 */
	private static void updateDirection(WindSettings config) {
		float speed = Math.max(0.0F, config.directionChangeSpeed);
		int interval = Math.max(20, (int) (DIRECTION_CHANGE_TICKS / Math.max(0.05F, speed)));

		if (ageTicks % interval == 0) {
			directionTarget = (float) (Math.random() * Math.PI * 2.0);
		}

		float delta = Mth.wrapDegrees((float) Math.toDegrees(directionTarget - direction));
		direction += (float) Math.toRadians(delta) * DIRECTION_EASE * speed;
	}

	/** Eases strength toward a weather driven target and applies the gust cycle. */
	private static void updateStrength(ClientLevel level, WindSettings config) {
		float rain = level.getRainLevel(1.0F);
		float thunder = level.getThunderLevel(1.0F);

		float weather = (rain * RAIN_STRENGTH + thunder * THUNDER_STRENGTH)
				* Math.max(0.0F, config.weatherInfluence);

		strengthTarget = CALM_STRENGTH + weather;
		strength += (strengthTarget - strength) * STRENGTH_EASE;

		// Two out of phase cycles, so the swell never settles into a loop the
		// ear can pick out.
		float gustPhase = (float) Math.sin(ageTicks / GUST_PERIOD_A * Math.PI * 2.0)
				* 0.6F
				+ (float) Math.sin(ageTicks / GUST_PERIOD_B * Math.PI * 2.0) * 0.4F;

		float gust = 1.0F + gustPhase * GUST_DEPTH * Math.max(0.0F, config.gustiness);

		strength = Mth.clamp(strength * gust * exposure * Math.max(0.0F, config.strength), 0.0F, 1.0F);
	}

	/** Current wind strength, 0 to 1. */
	public static float strength() {
		return strength;
	}

	/** Current wind heading in radians. */
	public static float directionRadians() {
		return direction;
	}

	/** How exposed the player's surroundings are, 0 sheltered to 1 open. */
	public static float exposure() {
		return exposure;
	}

	/** Eastward component of wind, scaled by strength. */
	public static double driftX() {
		return Math.cos(direction) * strength;
	}

	/** Southward component of wind, scaled by strength. */
	public static double driftZ() {
		return Math.sin(direction) * strength;
	}

	/** Clears carried state so re-entering a world does not inherit the previous one's wind. */
	public static void reset() {
		strength = 0.0F;
		strengthTarget = 0.0F;
		exposure = 0.0F;
		openness = 0.5F;
		ageTicks = 0L;
		resetSampleCounters();
	}
}
