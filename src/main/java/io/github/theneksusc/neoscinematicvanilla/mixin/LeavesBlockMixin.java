package io.github.theneksusc.neoscinematicvanilla.mixin;

import io.github.theneksusc.neoscinematicvanilla.config.CinematicConfig;
import io.github.theneksusc.neoscinematicvanilla.config.LeafSettings;
import io.github.theneksusc.neoscinematicvanilla.leaf.FallingLeafController;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Scales how often vanilla drops leaves from a leaf block.
 *
 * <p>Minecraft 26.2 already has falling leaves on every leaf block, with
 * tumbling, swirl, and a biome foliage tint. The only thing missing is any way
 * for a player to change the rate, so this replaces vanilla's single roll with
 * a scaled one and otherwise leaves the effect alone.
 *
 * <p>At a multiplier of exactly 1 the injection returns without cancelling,
 * letting vanilla run untouched. That keeps the default path identical to no
 * mod at all rather than merely equivalent, which matters because vanilla's
 * chance differs per tree species.
 */
@Mixin(LeavesBlock.class)
public abstract class LeavesBlockMixin {

	/** Vanilla's per species chance. 0.01 for most leaves, 0.1 for cherry, 0.02 for pale oak. */
	@Shadow
	@Final
	protected float leafParticleChance;

	/** Implemented per subclass, choosing the particle and its tint. */
	@Shadow
	protected abstract void spawnFallingLeavesParticle(Level level, BlockPos pos, RandomSource random);

	@Inject(method = "makeFallingLeavesParticles", at = @At("HEAD"), cancellable = true)
	private void neoscinematicvanilla$scaleLeafFrequency(
			Level level,
			BlockPos pos,
			RandomSource random,
			BlockState belowState,
			BlockPos below,
			CallbackInfo ci) {

		LeafSettings config = CinematicConfig.leaves();

		if (!config.enabled) {
			ci.cancel();
			return;
		}

		if (config.frequency == 1.0F) {
			return;
		}

		ci.cancel();

		// Vanilla's own guard. A leaf would otherwise spawn inside the block
		// directly beneath and be invisible.
		if (Block.isFaceFull(belowState.getCollisionShape(level, below), Direction.UP)) {
			return;
		}

		int count = FallingLeafController.rollSpawnCount(this.leafParticleChance, config.frequency, random);

		for (int i = 0; i < count; i++) {
			this.spawnFallingLeavesParticle(level, pos, random);
		}
	}
}
