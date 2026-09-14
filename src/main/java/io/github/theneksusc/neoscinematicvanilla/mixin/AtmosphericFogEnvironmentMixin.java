package io.github.theneksusc.neoscinematicvanilla.mixin;

import io.github.theneksusc.neoscinematicvanilla.NeosCinematicVanillaClient;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.AtmosphericFogEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Observation hook into vanilla's atmospheric fog.
 *
 * <p>{@code AtmosphericFogEnvironment} handles fog in normal air only. Vanilla
 * selects it through {@code isApplicable} before this code runs, so water,
 * lava, powder snow, blindness, and darkness fog are unaffected without any
 * explicit checks here.
 *
 * <p>This version only reports what vanilla computed. It deliberately changes
 * nothing, so that the injection point can be confirmed correct before any
 * behaviour depends on it.
 */
@Mixin(AtmosphericFogEnvironment.class)
public class AtmosphericFogEnvironmentMixin {

	/** Fog is set up every frame. Logging each one would flood the log, so samples are throttled. */
	private static final int LOG_INTERVAL_FRAMES = 120;

	private static int neoscinematicvanilla$frameCounter;

	/**
	 * Runs immediately before {@code setupFog} returns, at which point the
	 * {@code fog} object holds the values vanilla decided on.
	 *
	 * <p>Parameters must match the target method exactly, followed by the
	 * {@link CallbackInfo} that Mixin appends.
	 */
	@Inject(method = "setupFog", at = @At("TAIL"))
	private void neoscinematicvanilla$observeFog(
			FogData fog,
			Camera camera,
			ClientLevel level,
			float renderDistance,
			DeltaTracker deltaTracker,
			CallbackInfo ci) {

		if (neoscinematicvanilla$frameCounter++ % LOG_INTERVAL_FRAMES != 0) {
			return;
		}

		NeosCinematicVanillaClient.LOGGER.info(
				"fog sample: envStart={} envEnd={} skyEnd={} cloudEnd={} renderDistance={} y={}",
				fog.environmentalStart,
				fog.environmentalEnd,
				fog.skyEnd,
				fog.cloudEnd,
				renderDistance,
				camera.position().y);
	}
}
