package io.github.theneksusc.neoscinematicvanilla.fog;

import io.github.theneksusc.neoscinematicvanilla.config.FogConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.LightLayer;

/**
 * Computes an atmospheric fog band from the player's surroundings.
 *
 * <p>Vanilla leaves atmospheric fog switched off in the Overworld: the
 * registered defaults are a start of 0 and an end of 1024, which produces no
 * visible haze. This class fills that gap, driven by elevation, sky access, and
 * weather, while leaving dimensions that author their own fog untouched.
 *
 * <p>Cost per frame is one light lookup and a few dozen arithmetic operations.
 * There is no world scanning, no allocation, and no iteration over blocks.
 */
public final class FogController {

	/**
	 * The registered default for FOG_END_DISTANCE. A dimension or biome
	 * reporting anything else has deliberately authored its own fog.
	 */
	private static final float VANILLA_DEFAULT_FOG_END = 1024.0F;

	/** Elevation at or below which altitude haze reaches full strength. */
	private static final float HAZE_FLOOR_Y = 64.0F;

	/** Elevation at or above which altitude haze is fully absent. */
	private static final float HAZE_CEILING_Y = 140.0F;

	/** Elevation at or above which enclosed spaces count as buildings rather than caves. */
	private static final float CAVE_CEILING_Y = 60.0F;

	/** Elevation at or below which enclosure counts fully as being underground. */
	private static final float CAVE_FLOOR_Y = 20.0F;

	/** Peak density contributed by low elevation alone. */
	private static final float MAX_ALTITUDE_DENSITY = 0.18F;

	/** Peak density contributed underground. */
	private static final float MAX_CAVE_DENSITY = 1.0F;

	/** Peak density contributed by steady rain. */
	private static final float MAX_RAIN_DENSITY = 0.30F;

	/** Additional density contributed by thunder, on top of rain. */
	private static final float MAX_THUNDER_DENSITY = 0.15F;

	/**
	 * Near and far edges of the fog band, as multiples of render distance.
	 * The clear pair reproduces vanilla's absence of fog. The dense pair lands
	 * near the Nether's authored 10 to 96 band at default render distance.
	 */
	private static final float CLEAR_NEAR_FACTOR = 1.00F;
	private static final float CLEAR_FAR_FACTOR = 4.00F;
	private static final float DENSE_NEAR_FACTOR = 0.04F;
	private static final float DENSE_FAR_FACTOR = 0.40F;

	/** Keeps the near edge away from the far edge, which would otherwise read as a wall. */
	private static final float MIN_BAND_WIDTH = 8.0F;

	/** Smoothed density, carried between frames so changes in surroundings ease in. */
	private static float smoothedDensity;

	private FogController() {
	}

	/**
	 * Adjusts the fog band vanilla produced. Called at the tail of
	 * {@code AtmosphericFogEnvironment.setupFog}, where the fog object already
	 * holds vanilla's values including any rain offset it applied.
	 */
	public static void apply(
			FogData fog,
			Camera camera,
			ClientLevel level,
			float renderDistance,
			DeltaTracker deltaTracker) {

		FogConfig.reloadIfChanged();
		FogConfig config = FogConfig.get();

		if (!config.enabled || config.intensity <= 0.0F) {
			return;
		}

		float partialTicks = deltaTracker.getGameTimeDeltaPartialTick(false);

		if (!config.overrideCustomDimensionFog && hasAuthoredFog(camera, partialTicks)) {
			return;
		}

		smoothTowards(targetDensity(camera, level, partialTicks, config), deltaTracker, config);

		if (smoothedDensity <= 0.001F) {
			return;
		}

		float nearFactor = Mth.lerp(smoothedDensity, CLEAR_NEAR_FACTOR, DENSE_NEAR_FACTOR);
		float farFactor = Mth.lerp(smoothedDensity, CLEAR_FAR_FACTOR, DENSE_FAR_FACTOR);

		float end = renderDistance * farFactor * config.endDistance;

		// Vanilla's rain handling may already have pulled the far edge closer
		// than this. Applying a weaker band on top would undo that, so the mod
		// only acts where it would genuinely thicken the fog.
		if (end >= fog.environmentalEnd) {
			return;
		}

		float start = renderDistance * nearFactor * config.startDistance;

		fog.environmentalStart = Math.min(start, end - MIN_BAND_WIDTH);
		fog.environmentalEnd = end;
	}

	/**
	 * Reads the unmodified fog attribute rather than the value on the fog
	 * object, because vanilla's rain offset has already altered the latter and
	 * would otherwise be mistaken for an authored value.
	 */
	private static boolean hasAuthoredFog(Camera camera, float partialTicks) {
		float baseEnd = camera.attributeProbe().getValue(EnvironmentAttributes.FOG_END_DISTANCE, partialTicks);
		return baseEnd != VANILLA_DEFAULT_FOG_END;
	}

	/** Combines the environmental signals into a single density between 0 and 1. */
	private static float targetDensity(Camera camera, ClientLevel level, float partialTicks, FogConfig config) {
		float y = (float) camera.position().y;
		BlockPos pos = camera.blockPosition();

		// Sky light measures exposure to the sky and does not drop at night, so
		// it separates underground from outdoors regardless of time of day.
		int skyLight = level.getLightEngine().getLayerListener(LightLayer.SKY).getLightValue(pos);
		float enclosure = 1.0F - (skyLight / 15.0F);

		// Depth gates the cave term so that enclosed spaces at surface level,
		// which are almost always buildings, do not fill with fog.
		float depth = Mth.clamp((CAVE_CEILING_Y - y) / (CAVE_CEILING_Y - CAVE_FLOOR_Y), 0.0F, 1.0F);
		float cave = enclosure * enclosure * depth * MAX_CAVE_DENSITY * config.caveInfluence;

		float altitudeFactor = Mth.clamp((HAZE_CEILING_Y - y) / (HAZE_CEILING_Y - HAZE_FLOOR_Y), 0.0F, 1.0F);
		float altitude = altitudeFactor * MAX_ALTITUDE_DENSITY * config.altitudeInfluence;

		float rain = level.getRainLevel(partialTicks);
		float thunder = level.getThunderLevel(partialTicks);
		float weather = (rain * MAX_RAIN_DENSITY + thunder * MAX_THUNDER_DENSITY) * config.weatherInfluence;

		// Weather is not visible underground, so the two paths compete for the
		// result rather than stacking into something denser than either.
		float combined = Math.max(cave, altitude + weather);

		return Mth.clamp(combined * config.intensity, 0.0F, 1.0F);
	}

	/** Eases density towards its target so that crossing a cave mouth does not snap. */
	private static void smoothTowards(float target, DeltaTracker deltaTracker, FogConfig config) {
		float rate = Mth.clamp(config.transitionSpeed * deltaTracker.getGameTimeDeltaTicks(), 0.0F, 1.0F);
		smoothedDensity += (target - smoothedDensity) * rate;
	}

	/** Clears carried state so that re-entering a world does not inherit the previous one's fog. */
	public static void reset() {
		smoothedDensity = 0.0F;
	}
}
