package io.github.theneksusc.neoscinematicvanilla.config;

import io.github.theneksusc.neoscinematicvanilla.world.BiomeCharacter;

/**
 * How strongly each kind of place differs from the others, serialised as the
 * "biomes" object in the config file.
 *
 * <p>One value per character rather than one per biome. Sixty sliders would be
 * unusable in a settings screen and impossible to keep coherent, whereas six
 * cover every biome in the game and every modded one too, since characters are
 * classified from climate rather than named.
 *
 * <p>Each value scales how far that character departs from neutral, not the
 * effect itself. At 0 a place behaves exactly like temperate ground, at 1 it
 * gets the tuned difference, above 1 the difference is exaggerated. Expressing
 * it as distance from neutral means turning everything down converges on
 * uniform atmosphere rather than on no atmosphere.
 */
public class BiomeSettings {

	/** Master switch. When false, everywhere behaves as temperate ground. */
	public boolean enabled = true;

	/** Scales how far every character departs from neutral. */
	public float strength = 1.0F;

	public float arid = 1.0F;
	public float frozen = 1.0F;
	public float swamp = 1.0F;
	public float wooded = 1.0F;
	public float highland = 1.0F;

	/** Per character scale, or 0 when biome character is switched off. */
	public float scaleFor(BiomeCharacter character) {
		if (!this.enabled) {
			return 0.0F;
		}

		float own = switch (character) {
			case ARID -> this.arid;
			case FROZEN -> this.frozen;
			case SWAMP -> this.swamp;
			case WOODED -> this.wooded;
			case HIGHLAND -> this.highland;
			case TEMPERATE -> 1.0F;
		};

		return Math.max(0.0F, own * this.strength);
	}

	/**
	 * Applies a character's multiplier at the configured strength.
	 *
	 * <p>Interpolating from 1 rather than multiplying is what makes the scale
	 * mean "how different is this place", so a scale of 0 gives neutral ground
	 * instead of silencing the effect entirely.
	 */
	public float apply(BiomeCharacter character, float multiplier) {
		float scale = scaleFor(character);
		return 1.0F + (multiplier - 1.0F) * scale;
	}
}
