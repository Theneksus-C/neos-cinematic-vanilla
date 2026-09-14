package io.github.theneksusc.neoscinematicvanilla.fog;

import io.github.theneksusc.neoscinematicvanilla.NeosCinematicVanillaClient;
import io.github.theneksusc.neoscinematicvanilla.config.CinematicConfig;
import io.github.theneksusc.neoscinematicvanilla.config.FogSettings;
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
 * <p>Vanilla leaves atmospheric fog close to switched off in the Overworld: the
 * registered defaults are a start of 0 and an end of 1024, which produces a
 * faint distance gradient rather than nothing at all. This class thickens that
 * band in response to elevation, sky access, and weather, while leaving
 * dimensions that author their own fog untouched.
 *
 * <p>The band is interpolated away from whatever vanilla produced, never from a
 * fixed baseline. At zero density the result is exactly what vanilla would have
 * rendered, which keeps the effect continuous as density crosses zero and keeps
 * it correct at every render distance. An earlier version interpolated from an
 * assumed clear band, which produced a visible brightness step wherever density
 * reached zero, most obviously at the altitude ceiling.
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

	/**
	 * Peak density contributed by low elevation alone. Tuned down from 0.18
	 * after play testing, which read as slightly too heavy at ground level.
	 */
	private static final float MAX_ALTITUDE_DENSITY = 0.14F;

	/** Peak density contributed underground. */
	private static final float MAX_CAVE_DENSITY = 1.0F;

	/** Peak density contributed by steady rain. */
	private static final float MAX_RAIN_DENSITY = 0.30F;

	/** Additional density contributed by thunder, on top of rain. */
	private static final float MAX_THUNDER_DENSITY = 0.15F;

	/** Near edge of the band at full density, as a multiple of render distance. */
	private static final float DENSE_NEAR_FACTOR = 0.04F;

	/**
	 * Fog strength at full density, expressed as the fraction of full fog
	 * reached at render distance. Vanilla's Overworld default works out to
	 * roughly 0.25 at 16 chunks. Values above 1 saturate before the far plane,
	 * which is what a cave needs.
	 *
	 * <p>Density is mapped onto this rather than onto the far edge directly.
	 * The far edge starts at 1024 while useful viewing distances are a few
	 * hundred blocks, so interpolating it moves the visible result far too
	 * little at low densities.
	 */
	private static final float DENSE_FOG_FRACTION = 2.5F;

	/** Keeps the near edge away from the far edge, which would otherwise read as a wall. */
	private static final float MIN_BAND_WIDTH = 8.0F;

	/** Below this the band is indistinguishable from vanilla, so the work is skipped. */
	private static final float NEGLIGIBLE_DENSITY = 0.001F;

	/** Smoothed density, carried between frames so changes in surroundings ease in. */
	private static float smoothedDensity;

	/** Throttles diagnostic output to roughly one line every two seconds at 60 fps. */
	private static final int DEBUG_LOG_INTERVAL_FRAMES = 120;

	private static int debugFrameCounter;

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

		CinematicConfig.reloadIfChanged();
		FogSettings config = CinematicConfig.fog();

		if (!config.enabled || config.intensity <= 0.0F) {
			return;
		}

		float partialTicks = deltaTracker.getGameTimeDeltaPartialTick(false);

		if (!config.overrideCustomDimensionFog && hasAuthoredFog(camera, partialTicks)) {
			return;
		}

		smoothTowards(targetDensity(camera, level, partialTicks, config), deltaTracker, config);

		if (smoothedDensity <= NEGLIGIBLE_DENSITY) {
			return;
		}

		float vanillaStart = fog.environmentalStart;
		float vanillaEnd = fog.environmentalEnd;
		float vanillaSpan = vanillaEnd - vanillaStart;

		if (vanillaSpan <= 0.0F) {
			return;
		}

		// How much of full fog vanilla reaches at render distance. Anchoring the
		// interpolation here rather than on the far edge keeps zero density
		// identical to vanilla while making low densities actually visible.
		float vanillaFraction = (renderDistance - vanillaStart) / vanillaSpan;
		float targetFraction = Mth.lerp(smoothedDensity, vanillaFraction, DENSE_FOG_FRACTION * config.endDistance);

		if (targetFraction <= 0.0F) {
			return;
		}

		float start = Mth.lerp(smoothedDensity, vanillaStart, renderDistance * DENSE_NEAR_FACTOR * config.startDistance);
		float end = start + (renderDistance - start) / targetFraction;

		// Guards a configuration that would otherwise thin fog below vanilla,
		// which is never the intent of this mod.
		end = Math.min(end, vanillaEnd);
		end = Math.max(end, start + MIN_BAND_WIDTH);

		fog.environmentalStart = start;
		fog.environmentalEnd = end;

		if (config.debugLogging) {
			logSample(start, end, renderDistance, camera);
		}
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
	private static float targetDensity(Camera camera, ClientLevel level, float partialTicks, FogSettings config) {
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
	private static void smoothTowards(float target, DeltaTracker deltaTracker, FogSettings config) {
		float rate = Mth.clamp(config.transitionSpeed * deltaTracker.getGameTimeDeltaTicks(), 0.0F, 1.0F);
		smoothedDensity += (target - smoothedDensity) * rate;
	}

	/** Clears carried state so that re-entering a world does not inherit the previous one's fog. */
	public static void reset() {
		smoothedDensity = 0.0F;
	}

	/**
	 * Reports the resulting band and the fog fraction it produces at a few
	 * distances, so tuning can be judged against numbers rather than
	 * impressions. Enabled by the debugLogging config flag and throttled to
	 * roughly one line every two seconds.
	 */
	private static void logSample(float start, float end, float renderDistance, Camera camera) {
		if (debugFrameCounter++ % DEBUG_LOG_INTERVAL_FRAMES != 0) {
			return;
		}

		NeosCinematicVanillaClient.LOGGER.info(
				"fog: y={} density={} band={}..{} renderDistance={} fogAt64={} fogAt128={} fogAt256={}",
				String.format("%.1f", camera.position().y),
				String.format("%.3f", smoothedDensity),
				String.format("%.1f", start),
				String.format("%.1f", end),
				String.format("%.0f", renderDistance),
				String.format("%.2f", fogFractionAt(64.0F, start, end)),
				String.format("%.2f", fogFractionAt(128.0F, start, end)),
				String.format("%.2f", fogFractionAt(256.0F, start, end)));
	}

	/** Mirrors linear_fog_value from vanilla's fog.glsl, for diagnostic output only. */
	private static float fogFractionAt(float distance, float start, float end) {
		if (distance <= start) {
			return 0.0F;
		}

		if (distance >= end) {
			return 1.0F;
		}

		return (distance - start) / (end - start);
	}
}
