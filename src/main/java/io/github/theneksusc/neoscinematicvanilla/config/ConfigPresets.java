package io.github.theneksusc.neoscinematicvanilla.config;

/**
 * Named starting points for the settings.
 *
 * <p>A preset overwrites the values it governs and leaves everything else
 * alone, so debug toggles and elevation thresholds a user has set are not
 * clobbered by picking a strength. Presets are a starting point to tune from,
 * not a mode the mod stays in.
 *
 * <p>Each preset is expressed as multipliers on the tuned baseline rather than
 * as absolute numbers, so retuning the baseline moves every preset with it.
 */
public enum ConfigPresets {

	/** Everything off, leaving vanilla untouched without uninstalling. */
	OFF(0.0F, 0.0F),

	/** Present but easy to miss. For players who want a hint rather than a look. */
	SUBTLE(0.5F, 0.55F),

	/** The tuned defaults. */
	BALANCED(1.0F, 1.0F),

	/** Heavier atmosphere, closer to a deliberately moody look. */
	CINEMATIC(1.6F, 1.7F);

	private final float fogIntensity;
	private final float particleDensity;

	ConfigPresets(float fogIntensity, float particleDensity) {
		this.fogIntensity = fogIntensity;
		this.particleDensity = particleDensity;
	}

	/** Translation key for this preset's button label. */
	public String translationKey() {
		return "neoscinematicvanilla.preset." + name().toLowerCase(java.util.Locale.ROOT);
	}

	/**
	 * Applies this preset to the live settings.
	 *
	 * <p>The enabled flags follow the strength, so choosing OFF genuinely stops
	 * the work rather than running every system at zero effect.
	 */
	public void apply() {
		FogSettings fog = CinematicConfig.fog();
		ParticleSettings particles = CinematicConfig.particles();
		LeafSettings leaves = CinematicConfig.leaves();

		fog.enabled = this.fogIntensity > 0.0F;
		fog.intensity = this.fogIntensity;

		particles.enabled = this.particleDensity > 0.0F;
		particles.density = this.particleDensity;

		// Leaves track particle density, since both are airborne clutter and a
		// player who wants less of one almost always wants less of the other.
		leaves.enabled = this.particleDensity > 0.0F;
		leaves.frequency = this.particleDensity;

		// Per source weights return to even, so a preset is a predictable
		// starting point rather than a blend with whatever was set before.
		fog.altitudeInfluence = 1.0F;
		fog.caveInfluence = 1.0F;
		fog.weatherInfluence = 1.0F;

		particles.caveDustDensity = 1.0F;
		particles.forestMoteDensity = 1.0F;
		particles.jungleRayDensity = 1.0F;
	}
}
