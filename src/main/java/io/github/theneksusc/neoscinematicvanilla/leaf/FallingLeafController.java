package io.github.theneksusc.neoscinematicvanilla.leaf;

import net.minecraft.util.RandomSource;

/**
 * Decides how many leaves fall from a leaf block on a given tick.
 *
 * <p>Vanilla rolls once against a per block chance, which caps the rate at one
 * leaf per sampled position and makes any multiplier above roughly a hundredfold
 * meaningless. Splitting the scaled chance into a whole part and a remainder
 * lifts that cap while keeping the average exactly proportional to the
 * multiplier.
 *
 * <p>Vanilla's own chances differ per tree, 0.01 for most leaves against 0.1 for
 * cherry, and scaling rather than replacing them preserves those differences.
 */
public final class FallingLeafController {

	private FallingLeafController() {
	}

	/**
	 * Returns how many leaves to spawn this tick.
	 *
	 * <p>A scaled chance of 2.5 spawns two leaves always and a third half the
	 * time, averaging 2.5. A scaled chance below 1 behaves exactly as vanilla's
	 * single roll does.
	 */
	public static int rollSpawnCount(float baseChance, float frequency, RandomSource random) {
		if (frequency <= 0.0F || baseChance <= 0.0F) {
			return 0;
		}

		float scaled = baseChance * frequency;
		int guaranteed = (int) scaled;
		float remainder = scaled - guaranteed;

		return guaranteed + (random.nextFloat() < remainder ? 1 : 0);
	}
}
