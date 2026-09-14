package io.github.theneksusc.neoscinematicvanilla.mixin;

import io.github.theneksusc.neoscinematicvanilla.config.CinematicConfig;
import io.github.theneksusc.neoscinematicvanilla.config.WindSettings;
import io.github.theneksusc.neoscinematicvanilla.wind.WindSystem;
import net.minecraft.client.particle.FallingLeavesParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Carries vanilla's falling leaves on the shared wind.
 *
 * <p>Vanilla already tumbles and swirls a leaf, but its sideways drift comes
 * from values randomised per particle, so leaves falling side by side wander in
 * unrelated directions. Pulling them toward a common wind is what makes a gust
 * read as weather rather than as noise, and it is the reason the wind model is
 * shared rather than owned by any one effect.
 *
 * <h2>Why a pull rather than an addition</h2>
 *
 * <p>Vanilla accumulates into the velocity and never damps it:
 *
 * <pre>
 * this.xd += xa * 0.0025F;
 * this.move(this.xd, this.yd, this.zd);
 * </pre>
 *
 * <p>Adding a wind term every tick would therefore accelerate a leaf for its
 * entire three hundred tick life. Easing the velocity toward a target speed
 * instead is bounded, and it matches how a real leaf reaches a terminal drift
 * rather than accelerating forever.
 *
 * <p>Vanilla's swirl is applied after this in the same tick, so the tumble
 * survives and simply happens along the wind rather than around a fixed point.
 */
@Mixin(FallingLeavesParticle.class)
public class FallingLeavesParticleMixin {

	/** Drift a leaf reaches at full wind, in blocks per tick. */
	private static final double LEAF_WIND_SPEED = 0.055;

	/**
	 * How quickly a leaf takes up the wind's speed, per tick. Low, so a leaf
	 * eases into a gust over a couple of seconds rather than snapping sideways.
	 */
	private static final double LEAF_WIND_RESPONSE = 0.03;

	@Inject(method = "tick", at = @At("HEAD"))
	private void neoscinematicvanilla$carryOnWind(CallbackInfo ci) {
		WindSettings wind = CinematicConfig.wind();

		if (!wind.enabled || wind.leafInfluence <= 0.0F) {
			return;
		}

		// Velocity lives on Particle, which @Shadow cannot reach from a mixin on
		// a subclass, so it is read and written through an accessor mixin
		// applied to the class that declares it.
		ParticleVelocityAccessor self = (ParticleVelocityAccessor) this;

		double scale = LEAF_WIND_SPEED * wind.leafInfluence;
		double targetX = WindSystem.driftX() * scale;
		double targetZ = WindSystem.driftZ() * scale;

		double xd = self.neoscinematicvanilla$getXd();
		double zd = self.neoscinematicvanilla$getZd();

		self.neoscinematicvanilla$setXd(xd + (targetX - xd) * LEAF_WIND_RESPONSE);
		self.neoscinematicvanilla$setZd(zd + (targetZ - zd) * LEAF_WIND_RESPONSE);
	}
}
