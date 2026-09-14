package io.github.theneksusc.neoscinematicvanilla.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.CrossFrameResourcePool;
import io.github.theneksusc.neoscinematicvanilla.render.FilmGrainRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Runs the film grain pass over the finished world image.
 *
 * <p>Injected immediately before {@code fogRenderer.endFrame()}, which sits
 * after vanilla's own post effect has been applied and before the interface is
 * drawn. That ordering matters in both directions: running earlier would let
 * vanilla's effect overwrite the grain, and running later would lay grain over
 * the HUD and menus, which reads as a broken display rather than as film.
 *
 * <p>The allocator is shadowed because a post chain needs somewhere to put its
 * intermediate targets, and reusing the one the renderer already owns avoids
 * allocating a second pool for a single pass.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

	@Shadow
	@Final
	private CrossFrameResourcePool resourcePool;

	@Shadow
	public abstract RenderTarget mainRenderTarget();

	@Inject(
			method = "render",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/fog/FogRenderer;endFrame()V"))
	private void neoscinematicvanilla$applyFilmGrain(
			DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo ci) {

		FilmGrainRenderer.apply(mainRenderTarget(), this.resourcePool);
	}
}
