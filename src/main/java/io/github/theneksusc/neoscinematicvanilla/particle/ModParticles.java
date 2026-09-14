package io.github.theneksusc.neoscinematicvanilla.particle;

import io.github.theneksusc.neoscinematicvanilla.NeosCinematicVanillaClient;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

/**
 * Particle types this mod adds.
 *
 * <p>Vanilla offers no mote that is simultaneously tintable, long lived, and
 * free of built in motion. {@code DUST} takes a colour but divides its lifetime
 * by its size, {@code WHITE_ASH} hardcodes a pale grey and applies friction to
 * a velocity of its own, and {@code SPORE_BLOSSOM_AIR} hardcodes green.
 * Registering a type gives full control over colour, lifetime, drift, and
 * opacity.
 *
 * <p>Modded entries are appended after vanilla ones, so the numeric ids vanilla
 * uses in particle packets are unchanged and a vanilla server remains
 * compatible.
 */
public final class ModParticles {

	/**
	 * A slow drifting mote that fades in and out. See {@link DustMoteParticle}.
	 *
	 * <p>Carries a colour per spawn rather than reading one global setting,
	 * because colour comes from the character of the place: sandy in a desert,
	 * cold white in snow, warm under a canopy. Vanilla's own
	 * {@code ColorParticleOption} already holds exactly that, so its codecs are
	 * reused rather than a bespoke option type being written.
	 */
	public static final ParticleType<ColorParticleOption> DUST_MOTE =
			FabricParticleTypes.complex(ColorParticleOption::codec, ColorParticleOption::streamCodec);

	private ModParticles() {
	}

	/**
	 * Registers the particle types. Must run before anything spawns one.
	 *
	 * <p>Sprites come from {@code assets/neoscinematicvanilla/particles/dust_mote.json},
	 * which points at a vanilla texture, so no image ships with the mod.
	 */
	public static void register() {
		Registry.register(
				BuiltInRegistries.PARTICLE_TYPE,
				Identifier.fromNamespaceAndPath(NeosCinematicVanillaClient.MOD_ID, "dust_mote"),
				DUST_MOTE);
	}
}
