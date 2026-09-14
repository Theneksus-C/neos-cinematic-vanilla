package io.github.theneksusc.neoscinematicvanilla.config;

/**
 * Ambient particle settings, serialised as the "particles" object in the config
 * file.
 *
 * <p>Densities are multipliers on a deliberately low base rate. The base rate
 * is chosen so that a value of 1 produces a couple of spawns per second, which
 * settles at roughly seventy particles alive given their long lifetime.
 */
public class ParticleSettings {

	/** Master switch for ambient particles. */
	public boolean enabled = true;

	/** Scales every particle source below. */
	public float density = 1.0F;

	/** Dust motes drifting in caves and other deep enclosed spaces. */
	public float caveDustDensity = 1.0F;

	/** Sparse motes among the canopy in forest, taiga, and jungle biomes. */
	public float forestMoteDensity = 1.0F;

	/**
	 * Logs how many particles the mod has spawned, roughly once every ten
	 * seconds. For confirming rates are what they were intended to be.
	 */
	public boolean debugLogging = false;
}
