package io.github.theneksusc.neoscinematicvanilla.config;

/**
 * Wind settings, serialised as the "wind" object in the config file.
 *
 * <p>Wind is a shared model rather than an effect of its own. It produces a
 * direction and a strength, and other systems read them: the ambient loop's
 * volume, how motes drift, and how leaves are carried.
 *
 * <p>Grass and foliage sway is deliberately absent. Vanilla has no foliage wave
 * shader, so animating plants would mean custom block model rendering with
 * vertex animation, which is a different class of work from anything else here
 * and the most likely thing to break with Sodium.
 */
public class WindSettings {

	/** Master switch for wind. When false, nothing reads a wind value. */
	public boolean enabled = true;

	/** Scales wind strength everywhere. */
	public float strength = 1.0F;

	/**
	 * How strongly exposure varies wind. At 0 wind is the same everywhere; at 1
	 * a sheltered forest floor is nearly still while a peak is fully exposed.
	 */
	public float exposureInfluence = 1.0F;

	/** How much rain and thunder raise wind strength. */
	public float weatherInfluence = 1.0F;

	/** Depth of the slow rise and fall in strength. 0 gives perfectly steady wind. */
	public float gustiness = 1.0F;

	/** How fast the wind direction wanders. */
	public float directionChangeSpeed = 1.0F;

	/** Master switch for the ambient wind loop. */
	public boolean soundEnabled = true;

	/** Loudest the wind loop gets, at full strength, from 0 to 1. */
	public float soundVolume = 0.35F;

	/**
	 * Wind strength below which the loop is silent. Prevents a constant faint
	 * hiss indoors and in sheltered places.
	 */
	public float soundThreshold = 0.08F;

	/** How much wind pushes ambient motes sideways. */
	public float moteInfluence = 1.0F;

	/** How much wind carries falling leaves sideways. */
	public float leafInfluence = 1.0F;

	/** Logs wind strength, direction, and exposure roughly twice a second. */
	public boolean debugLogging = false;
}
