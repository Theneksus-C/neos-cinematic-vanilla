package io.github.theneksusc.neoscinematicvanilla.mixin;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes a particle's horizontal velocity.
 *
 * <p>The fields are protected on {@link Particle}, but {@code @Shadow} only
 * resolves members declared in the target class itself, not inherited ones. A
 * mixin on a subclass therefore cannot shadow them, and an accessor on the
 * class that actually declares them is the supported way to reach them.
 *
 * <p>Vertical velocity is deliberately absent. Wind here is horizontal, and
 * exposing more than is needed invites someone later to fight gravity with it.
 */
@Mixin(Particle.class)
public interface ParticleVelocityAccessor {

	@Accessor("xd")
	double neoscinematicvanilla$getXd();

	@Accessor("xd")
	void neoscinematicvanilla$setXd(double value);

	@Accessor("zd")
	double neoscinematicvanilla$getZd();

	@Accessor("zd")
	void neoscinematicvanilla$setZd(double value);
}
