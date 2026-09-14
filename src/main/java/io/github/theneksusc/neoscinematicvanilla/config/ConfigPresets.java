package io.github.theneksusc.neoscinematicvanilla.config;

import java.util.Locale;

/**
 * Named looks for the whole mod.
 *
 * <p>A preset resets every visual setting to its default and then applies its
 * own deltas, so picking one produces the same result no matter what was set
 * before. Expressing each preset as deltas from the defaults means only the
 * values it actually changes appear below, and retuning a default moves every
 * preset with it.
 *
 * <p>Three things are deliberately preserved across a preset:
 *
 * <ul>
 *   <li><b>Mote colour.</b> A strength level has no business overwriting a
 *       colour someone chose.
 *   <li><b>Debug logging.</b> Switching logging on or off is never what someone
 *       pressing a preset was asking for.
 *   <li><b>Override Nether fog.</b> That replaces a look Mojang authored
 *       deliberately, so it stays an explicit opt in rather than something a
 *       preset can enable silently.
 * </ul>
 */
public enum ConfigPresets {

	/**
	 * Everything off, leaving vanilla untouched without uninstalling. Values
	 * are left at their defaults so re-enabling a system by hand gives
	 * something sensible rather than whatever was set last.
	 */
	OFF {
		@Override
		void configure(FogSettings fog, ParticleSettings particles, LeafSettings leaves) {
			fog.enabled = false;
			particles.enabled = false;
			leaves.enabled = false;
		}
	},

	/** Present but easy to miss. For players who want a hint rather than a look. */
	SUBTLE {
		@Override
		void configure(FogSettings fog, ParticleSettings particles, LeafSettings leaves) {
			fog.intensity = 0.5F;
			fog.endDistance = 0.8F;
			fog.hazeNoneAboveY = 120.0F;
			fog.caveNoneAboveY = 55.0F;

			particles.density = 0.55F;
			particles.moteOpacity = 0.5F;
			particles.moteSize = 0.85F;
			particles.moteLifetimeSeconds = 14.0F;
			particles.shaftDensity = 0.7F;
			particles.shaftLength = 0.85F;
			particles.shaftRadius = 0.9F;

			leaves.frequency = 0.55F;
		}
	},

	/** The tuned defaults, which is why this preset changes nothing. */
	BALANCED {
		@Override
		void configure(FogSettings fog, ParticleSettings particles, LeafSettings leaves) {
		}
	},

	/** Heavier atmosphere, closer to a deliberately moody look. */
	CINEMATIC {
		@Override
		void configure(FogSettings fog, ParticleSettings particles, LeafSettings leaves) {
			fog.intensity = 1.6F;
			fog.startDistance = 0.85F;
			fog.endDistance = 1.3F;
			fog.transitionSpeed = 0.05F;
			fog.hazeFullBelowY = 72.0F;
			fog.hazeNoneAboveY = 170.0F;
			fog.caveFullBelowY = 30.0F;
			fog.caveNoneAboveY = 66.0F;

			particles.density = 1.7F;
			particles.moteOpacity = 0.85F;
			particles.moteSize = 1.2F;
			particles.moteLifetimeSeconds = 24.0F;
			particles.moteFadeSeconds = 2.5F;
			particles.moteDriftSpeed = 1.15F;
			particles.shaftDensity = 1.4F;
			particles.shaftLength = 1.25F;
			particles.shaftRadius = 1.15F;
			particles.shaftLean = 1.2F;

			leaves.frequency = 1.7F;
		}
	};

	/** Applies this preset's deltas on top of freshly defaulted settings. */
	abstract void configure(FogSettings fog, ParticleSettings particles, LeafSettings leaves);

	/** Translation key for this preset's button label. */
	public String translationKey() {
		return "neoscinematicvanilla.preset." + name().toLowerCase(Locale.ROOT);
	}

	/**
	 * Applies this preset to the live settings.
	 *
	 * <p>Resetting first is what makes a preset predictable: the result depends
	 * only on which preset was chosen, never on what happened to be set before.
	 * The few settings a preset must not touch are carried across the reset by
	 * hand.
	 */
	public void apply() {
		String moteColor = CinematicConfig.particles().moteColor;
		boolean fogDebug = CinematicConfig.fog().debugLogging;
		boolean particleDebug = CinematicConfig.particles().debugLogging;
		boolean overrideDimensionFog = CinematicConfig.fog().overrideCustomDimensionFog;

		CinematicConfig.resetToDefaults();

		FogSettings fog = CinematicConfig.fog();
		ParticleSettings particles = CinematicConfig.particles();
		LeafSettings leaves = CinematicConfig.leaves();

		particles.moteColor = moteColor;
		fog.debugLogging = fogDebug;
		particles.debugLogging = particleDebug;
		fog.overrideCustomDimensionFog = overrideDimensionFog;

		configure(fog, particles, leaves);
	}
}
