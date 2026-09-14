package io.github.theneksusc.neoscinematicvanilla.config;

/**
 * Film grain settings, serialised as the "grain" object in the config file.
 *
 * <p>Strength is a discrete level rather than a slider because of how
 * Minecraft's post processing works. A post effect's uniforms are baked into a
 * GPU buffer when the effect is built from its JSON definition, and
 * {@code PostChain} exposes no way to change them afterwards. Continuous
 * intensity would mean either rebuilding the effect on every change or reaching
 * into the shader system's internals, so the mod ships one effect file per
 * level instead.
 *
 * <p>Grain is also the one effect here that sits on top of the finished image
 * rather than changing something in the world, so it defaults to off. Everything
 * else in this mod is on by default.
 */
public class GrainSettings {

	/** No grain. Nothing is applied and the render pipeline is untouched. */
	public static final int LEVEL_OFF = 0;

	/** Highest level, used to bound the setting. */
	public static final int LEVEL_MAX = 3;

	/**
	 * Grain strength: 0 off, 1 light, 2 medium, 3 heavy.
	 *
	 * <p>Off by default. Grain reads as a deliberate stylistic choice rather
	 * than as atmosphere, so it should be something a player turns on.
	 */
	public int level = LEVEL_OFF;
}
