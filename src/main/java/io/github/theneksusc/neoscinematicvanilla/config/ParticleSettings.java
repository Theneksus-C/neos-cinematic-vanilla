package io.github.theneksusc.neoscinematicvanilla.config;

/**
 * Ambient particle settings, serialised as the "particles" object in the config
 * file.
 *
 * <p>Two kinds of value live here. Densities and multipliers default to 1 and
 * scale a tuned baseline, so a user can nudge without needing to know the
 * underlying numbers. Appearance values such as colour and lifetime are
 * absolute, because those are what a person actually wants to state directly.
 *
 * <p>Anything that affects how the effect looks belongs here rather than as a
 * constant in the code. Constants are reserved for values that would break the
 * effect if changed, not for matters of taste.
 */
public class ParticleSettings {

	/** Master switch for ambient particles. */
	public boolean enabled = true;

	/** Scales every particle source below. */
	public float density = 1.0F;

	/** Dust motes suspended in caves and other deep enclosed spaces. */
	public float caveDustDensity = 1.0F;

	/** Motes among the canopy in forest and taiga biomes. */
	public float forestMoteDensity = 1.0F;

	/** Motes among the canopy in jungles, which are denser and narrower. */
	public float jungleRayDensity = 1.0F;

	/**
	 * Mote colour as a hex string, with or without a leading hash. Defaults to
	 * a warm yellow suggesting dust catching sunlight. Falls back to that
	 * default if the value cannot be parsed.
	 */
	public String moteColor = "#FFE68C";

	/** Opacity a mote reaches once fully faded in, from 0 to 1. */
	public float moteOpacity = 0.70F;

	/** Scales mote size. Above 1 makes individual motes more obvious. */
	public float moteSize = 1.0F;

	/** How long a mote lasts before fading out, in seconds. */
	public float moteLifetimeSeconds = 18.0F;

	/** Length of the fade at each end of a mote's life, in seconds. */
	public float moteFadeSeconds = 1.5F;

	/** Scales how fast motes drift. 0 leaves them completely still. */
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

	/** Colour used when {@link #moteColor} cannot be parsed. */
	private static final int FALLBACK_COLOR = 0xFFE68C;

	/**
	 * Parses {@link #moteColor} into a packed RGB value.
	 *
	 * <p>Parsing happens per particle rather than once at load, which is
	 * affordable because it runs a few times a second at most, and which means
	 * an edited config takes effect without any cached value to invalidate.
	 */
	public int moteColorRgb() {
		String value = this.moteColor;

		if (value == null) {
			return FALLBACK_COLOR;
		}

		value = value.trim();

		if (value.startsWith("#")) {
			value = value.substring(1);
		}

		try {
			return Integer.parseInt(value, 16) & 0xFFFFFF;
		} catch (NumberFormatException e) {
			return FALLBACK_COLOR;
		}
	}
}
