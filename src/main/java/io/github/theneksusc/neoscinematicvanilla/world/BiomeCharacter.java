package io.github.theneksusc.neoscinematicvanilla.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

/**
 * The mood of a place, used to vary every other effect by where the player is.
 *
 * <p>Uniform atmosphere stops being noticed. A desert that feels like a plains
 * that feels like a savanna is atmosphere applied to a world rather than
 * atmosphere belonging to it, so each character supplies its own multipliers and
 * mote colour and the existing systems read them.
 *
 * <h2>Classified by climate, not by a list of biomes</h2>
 *
 * <p>Characters are decided from data every biome already carries: whether it
 * gets precipitation, whether that precipitation is snow, and how warm it is.
 * A hot biome that never rains is arid whether it is vanilla's desert or one
 * from another mod, so modded worlds get sensible character without this class
 * knowing anything about them. A hardcoded list of biome names would cover
 * vanilla and nothing else.
 *
 * <p>Swamps are the exception. Vanilla has no swamp tag and no climate value
 * separates a swamp from a plains, so the two vanilla swamps are named
 * directly. Modded swamps therefore read as temperate, which is a known
 * limitation rather than an oversight.
 */
public enum BiomeCharacter {

	/** The baseline. Plains, meadows, beaches, anything unremarkable. */
	TEMPERATE(1.00F, 1.00F, 1.00F, 0xF3E7CD),

	/** Deserts, badlands, savannas. Dust in the air, warm haze, little shelter. */
	ARID(1.15F, 1.45F, 1.55F, 0xE3C793),

	/** Anywhere snow falls. Pale cold air and motes that read as blown snow. */
	FROZEN(1.30F, 1.35F, 1.30F, 0xDCE9F5),

	/** Swamps. Thick still air, the heaviest fog and the least wind. */
	SWAMP(1.65F, 0.75F, 0.45F, 0xC6D0AE),

	/** Forest, taiga, jungle. Sheltered from wind, full of motes. */
	WOODED(0.90F, 1.25F, 0.55F, 0xF6EBCB),

	/** Peaks and windswept hills. Thin clear air and the strongest wind. */
	HIGHLAND(0.60F, 0.70F, 1.60F, 0xE9F1F7);

	/** Temperature at or above which a biome with no precipitation counts as arid. */
	private static final float ARID_TEMPERATURE = 0.9F;

	private final float fogMultiplier;
	private final float particleMultiplier;
	private final float windMultiplier;
	private final int moteColor;

	BiomeCharacter(float fogMultiplier, float particleMultiplier, float windMultiplier, int moteColor) {
		this.fogMultiplier = fogMultiplier;
		this.particleMultiplier = particleMultiplier;
		this.windMultiplier = windMultiplier;
		this.moteColor = moteColor;
	}

	public float fogMultiplier() {
		return this.fogMultiplier;
	}

	public float particleMultiplier() {
		return this.particleMultiplier;
	}

	public float windMultiplier() {
		return this.windMultiplier;
	}

	/** Packed RGB for motes here. Sandy in a desert, cold white in snow. */
	public int moteColor() {
		return this.moteColor;
	}

	/** Translation key for this character's name in the settings screen. */
	public String translationKey() {
		return "neoscinematicvanilla.biome." + name().toLowerCase(java.util.Locale.ROOT);
	}

	/**
	 * Classifies a position.
	 *
	 * <p>Order matters. Swamps are named outright because nothing distinguishes
	 * them otherwise. Snow is checked before anything else climatic, so a snowy
	 * peak reads as frozen rather than as highland. Arid comes before highland so
	 * that badlands plateaus stay dusty instead of becoming bare mountain.
	 */
	public static BiomeCharacter of(ClientLevel level, Holder<Biome> holder, BlockPos pos) {
		if (holder.is(Biomes.SWAMP) || holder.is(Biomes.MANGROVE_SWAMP)) {
			return SWAMP;
		}

		Biome.Precipitation precipitation = level.getPrecipitationAt(pos);

		if (precipitation == Biome.Precipitation.SNOW) {
			return FROZEN;
		}

		if (precipitation == Biome.Precipitation.NONE
				&& holder.value().getBaseTemperature() >= ARID_TEMPERATURE) {
			return ARID;
		}

		if (holder.is(BiomeTags.IS_MOUNTAIN)) {
			return HIGHLAND;
		}

		if (holder.is(BiomeTags.IS_FOREST) || holder.is(BiomeTags.IS_TAIGA) || holder.is(BiomeTags.IS_JUNGLE)) {
			return WOODED;
		}

		return TEMPERATE;
	}
}
