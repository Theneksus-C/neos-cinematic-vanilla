package io.github.theneksusc.neoscinematicvanilla.config;

/**
 * Fog settings, serialised as the "fog" object in the config file.
 *
 * <p>Fields are plain and public so the JSON maps onto them directly with no
 * custom serialisation. Defaults are tuned to be noticeable but restrained.
 */
public class FogSettings {

	/** Master switch for fog. When false, vanilla fog is left completely alone. */
	public boolean enabled = true;

	/**
	 * Scales every effect below. 0 disables fog changes while leaving the rest
	 * of the mod loaded, 1 is the tuned default, higher exaggerates.
	 */
	public float intensity = 1.0F;

	/** Haze that builds at low elevations and clears on high ground. */
	public float altitudeInfluence = 1.0F;

	/** Density added underground, driven by how much sky light reaches the camera. */
	public float caveInfluence = 1.0F;

	/** Additional density during rain and thunderstorms. */
	public float weatherInfluence = 1.0F;

	/** Scales where fog begins. Above 1 pushes the near edge further away. */
	public float startDistance = 1.0F;

	/** Scales how strong fog becomes at render distance. */
	public float endDistance = 1.0F;

	/**
	 * Whether to also affect dimensions that define their own fog, such as the
	 * Nether. Off by default so authored looks are preserved.
	 */
	public boolean overrideCustomDimensionFog = false;

	/**
	 * How quickly fog reacts to a change in surroundings, per tick. Lower is
	 * smoother. Prevents visible popping when moving between light levels.
	 */
	public float transitionSpeed = 0.08F;

	/**
	 * Logs the resulting fog band and the fog fraction at 64, 128, and 256
	 * blocks, roughly twice per second. For tuning, and for telling apart an
	 * effect that is absent from one that is merely too weak to see.
	 */
	public boolean debugLogging = false;
}
