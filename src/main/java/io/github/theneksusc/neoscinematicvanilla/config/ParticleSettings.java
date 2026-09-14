package io.github.theneksusc.neoscinematicvanilla.config;

/**
 * Ambient particle settings, serialised as the "particles" object in the config
 * file.
 *
 * <p>Densities are multipliers on deliberately low base rates. Base rates
 * differ per source because the particles they use have very different
 * lifetimes, and steady state count is spawn rate multiplied by lifetime.
 */
public class ParticleSettings {

	/** Master switch for ambient particles. */
	public boolean enabled = true;

	/** Scales every particle source below. */
	public float density = 1.0F;

	/** Dust motes drifting in caves and other deep enclosed spaces. */
	public float caveDustDensity = 1.0F;

	/** Sparse motes among the canopy in forest and taiga biomes. */
	public float forestMoteDensity = 1.0F;

	/** Shafts of fine motes in jungles, suggesting light through the canopy. */
	public float jungleRayDensity = 1.0F;

	/**
	 * Logs how many particles the mod has spawned in the last interval, so
	 * rates can be checked against what they were intended to be.
	 */
	public boolean debugLogging = false;
}
