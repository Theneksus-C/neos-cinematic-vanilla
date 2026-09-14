package io.github.theneksusc.neoscinematicvanilla.config;

/**
 * Falling leaf settings, serialised as the "leaves" object in the config file.
 *
 * <p>Minecraft 26.2 already drops leaves from every leaf block, with tumbling,
 * swirl, and a tint taken from the biome's foliage colour. What it does not
 * offer is any way for a player to change how often that happens, which is all
 * this section governs.
 *
 * <p>Leaf response to wind is deliberately absent here. Vanilla gives each leaf
 * an independently randomised sideways drift rather than a shared direction,
 * and correcting that belongs with the wind system rather than duplicating a
 * drift model that wind would replace.
 */
public class LeafSettings {

	/** Master switch. When false, no leaves fall at all. */
	public boolean enabled = true;

	/**
	 * Scales vanilla's per block chance. 1 leaves vanilla untouched, 0 stops
	 * leaves without disabling the section, and values above 1 increase the
	 * rate. Vanilla's own chance is 0.01 for most leaves, 0.1 for cherry, and
	 * 0.02 for pale oak, so those relative differences are preserved.
	 */
	public float frequency = 1.0F;
}
