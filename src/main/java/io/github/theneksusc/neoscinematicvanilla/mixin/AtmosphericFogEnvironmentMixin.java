package io.github.theneksusc.neoscinematicvanilla.mixin;

import io.github.theneksusc.neoscinematicvanilla.fog.FogController;
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
 * Hooks vanilla's atmospheric fog so that elevation, sky access, and weather
 * can influence it.
 *
 * <p>{@code AtmosphericFogEnvironment} handles fog in normal air only. Vanilla
 * selects it through {@code isApplicable} before this code runs, so water,
 * lava, powder snow, blindness, and darkness fog stay untouched without any
 * explicit checks here.
 *
 * <p>All decisions live in {@link FogController}. This class exists only to
 * hand vanilla's result to it, which keeps the injected code small and keeps
 * the logic readable outside of a Minecraft runtime.
 */
@Mixin(AtmosphericFogEnvironment.class)
public class AtmosphericFogEnvironmentMixin {

	/**
	 * Runs immediately before {@code setupFog} returns, at which point the fog
	 * object holds the values vanilla decided on.
	 *
	 * <p>Parameters must match the target method exactly, followed by the
	 * {@link CallbackInfo} that Mixin appends.
	 */
	@Inject(method = "setupFog", at = @At("TAIL"))
	private void neoscinematicvanilla$applyAtmosphere(
			FogData fog,
			Camera camera,
			ClientLevel level,
			float renderDistance,
			DeltaTracker deltaTracker,
			CallbackInfo ci) {

		FogController.apply(fog, camera, level, renderDistance, deltaTracker);
	}
}
