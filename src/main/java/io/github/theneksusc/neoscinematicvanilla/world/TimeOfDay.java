package io.github.theneksusc.neoscinematicvanilla.world;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;

/**
 * Where the world is in its day, and how much mist that implies.
 *
 * <p>Minecraft 26.2 replaced the old day time accessor with a clock system, so
 * the time comes from {@code getDefaultClockTime} against the dimension's own
 * clock rather than from a level field. Dimensions whose clock does not advance
 * report a constant, which is correct: the Nether should not have a dawn.
 *
 * <p>The day is 24000 ticks, which {@code Timelines} still sets as the overworld
 * clock's period.
 */
public final class TimeOfDay {

	/** Length of a full day in ticks. */
	private static final int DAY_LENGTH = 24000;

	/** Sunrise. Time runs 0 at dawn, 6000 at noon, 12000 at dusk, 18000 at midnight. */
	private static final int DAWN = 0;
	private static final int DUSK = 12000;
	private static final int MIDNIGHT = 18000;

	/**
	 * How far either side of dawn the mist reaches, in ticks. Roughly two
	 * minutes of real time on each side, so it is a moment in the day rather
	 * than a state the world sits in.
	 */
	private static final int DAWN_SPREAD = 2600;

	/** Dusk is broader and weaker, since evening haze settles rather than burns off. */
	private static final int DUSK_SPREAD = 3400;

	/** Night covers the hours either side of midnight. */
	private static final int NIGHT_SPREAD = 7000;

	private TimeOfDay() {
	}

	/** Ticks since dawn, 0 to 24000. */
	public static int timeOfDay(ClientLevel level) {
		return (int) Math.floorMod(level.getDefaultClockTime(), DAY_LENGTH);
	}

	/**
	 * Strength of the dawn mist, 0 to 1.
	 *
	 * <p>Peaks at sunrise and falls away on both sides, so mist has gathered
	 * before first light rather than appearing with it, and burns off through
	 * the morning.
	 */
	public static float dawnMist(ClientLevel level) {
		return peak(timeOfDay(level), DAWN, DAWN_SPREAD);
	}

	/** Strength of the evening haze, 0 to 1. */
	public static float duskHaze(ClientLevel level) {
		return peak(timeOfDay(level), DUSK, DUSK_SPREAD);
	}

	/** Strength of the night haze, 0 to 1. */
	public static float nightHaze(ClientLevel level) {
		return peak(timeOfDay(level), MIDNIGHT, NIGHT_SPREAD);
	}

	/**
	 * A smooth hump centred on a moment in the day.
	 *
	 * <p>Distance is measured the short way around the clock, so a peak centred
	 * on dawn rises before midnight rolls over to 0 instead of snapping on at
	 * the wrap point. The curve is smoothstep rather than linear, so the mist
	 * gathers and lifts rather than ramping at a constant rate.
	 */
	private static float peak(int time, int centre, int spread) {
		int distance = wrappedDistance(time, centre);

		if (distance >= spread) {
			return 0.0F;
		}

		float nearness = 1.0F - (distance / (float) spread);

		// Smoothstep. The ends flatten out, so there is no visible moment where
		// the mist starts or stops changing.
		return nearness * nearness * (3.0F - 2.0F * nearness);
	}

	/** Shortest distance between two times on a circular clock. */
	private static int wrappedDistance(int a, int b) {
		int direct = Math.abs(a - b);
		return Math.min(direct, DAY_LENGTH - direct);
	}

	/** Name of the current phase, for diagnostics. */
	public static String phaseName(ClientLevel level) {
		int time = timeOfDay(level);

		if (wrappedDistance(time, DAWN) < DAWN_SPREAD) {
			return "dawn";
		}

		if (wrappedDistance(time, DUSK) < DUSK_SPREAD) {
			return "dusk";
		}

		if (wrappedDistance(time, MIDNIGHT) < NIGHT_SPREAD) {
			return "night";
		}

		return "day";
	}

	/** Clamps a strength into range, for callers combining several. */
	public static float clamp(float value) {
		return Mth.clamp(value, 0.0F, 1.0F);
	}
}
