package io.github.theneksusc.neoscinematicvanilla.config;

/**
 * Ambient particle settings, serialised as the "particles" object in the config
 * file.
 *
 * <p>Densities are multipliers on deliberately low base rates. Base rates
 * differ per source because the particles they use have very different
 * lifetimes, and steady state count is spawn rate multiplied by lifetime.
 *
 * <p>Mote colour is deliberately absent. Colour is decided by the character of
 * the place, so that a desert reads as dusty and a snowfield as cold, and a
 * single chosen colour would flatten that straight back out. See
 * {@code BiomeCharacter} for the palette, and the "biomes" section for how
 * strongly places are allowed to differ from one another.
 */
public class ParticleSettings {

	/** Master switch for ambient particles. */
	public boolean enabled = true;

	/** Scales every particle source below. */
	public float density = 1.0F;

	/** Shafts of motes in caves and other deep enclosed spaces. */
	public float caveDustDensity = 1.0F;

	/**
	 * Motes in open air. Their rate, shape, and colour follow the character of
	 * the place, so this scales all of them at once rather than one biome.
	 */
	public float surfaceMoteDensity = 1.0F;

	/** Opacity a mote reaches once fully faded in, from 0 to 1. */
	public float moteOpacity = 0.70F;

	/** Scales mote size. Above 1 makes individual motes more obvious. */
	public float moteSize = 1.0F;

	/** How long a mote lasts before fading out, in seconds. */
	public float moteLifetimeSeconds = 18.0F;

	/** Length of the fade at each end of a mote's life, in seconds. */
	public float moteFadeSeconds = 1.5F;

	/** Scales how fast motes drift on their own, before wind is applied. */
	public float moteDriftSpeed = 1.0F;

	/** Scales how far a shaft of motes extends downward. */
	public float shaftLength = 1.0F;

	/** Scales how widely motes scatter around a shaft's axis. */
	public float shaftRadius = 1.0F;

	/** Scales how far a shaft leans from vertical. 0 makes every shaft upright. */
	public float shaftLean = 1.0F;

	/** Scales how many motes a single shaft contains. */
	public float shaftDensity = 1.0F;

	/**
	 * Logs how many particles the mod has spawned in the last interval, so
	 * rates can be checked against what they were intended to be.
	 */
	public boolean debugLogging = false;
}
