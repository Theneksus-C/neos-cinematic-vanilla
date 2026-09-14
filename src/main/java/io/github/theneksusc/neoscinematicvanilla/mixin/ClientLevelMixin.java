package io.github.theneksusc.neoscinematicvanilla.mixin;

import io.github.theneksusc.neoscinematicvanilla.particle.AmbientParticleController;
import io.github.theneksusc.neoscinematicvanilla.wind.WindSystem;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks vanilla's ambient position sampling so the mod can add motes of its
 * own.
 *
 * <p>{@code doAnimateTick} is called 1334 times per client tick with a random
 * position near the player, which vanilla uses for block animations, drips, and
 * the Nether's ambient particles. Reusing that sampling means the mod needs no
 * loop, no position generation, and no world scanning of its own.
 *
 * <p>Injecting at the tail means vanilla's own particles are unaffected. The
 * handler's first act is a single random comparison, which is what keeps a hook
 * on a path this hot from costing anything measurable.
 */
@Mixin(ClientLevel.class)
public class ClientLevelMixin {

	@Inject(method = "doAnimateTick", at = @At("TAIL"))
	private void neoscinematicvanilla$spawnAmbientParticles(
			int xt,
			int yt,
			int zt,
			int radius,
			RandomSource animateRandom,
			Block markerParticleTarget,
			BlockPos.MutableBlockPos pos,
			CallbackInfo ci) {

		ClientLevel level = (ClientLevel) (Object) this;

		AmbientParticleController.onSample(level, pos, animateRandom);
		WindSystem.sampleOpenness(level, pos);
	}
}
