package io.github.theneksusc.neoscinematicvanilla.world;

import io.github.theneksusc.neoscinematicvanilla.config.BiomeSettings;
import io.github.theneksusc.neoscinematicvanilla.config.CinematicConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * Caches what kind of place the player is standing in.
 *
 * <p>Fog runs every frame and wind every tick, and both want to know the
 * character of the surroundings. Classifying it at each of those call sites
 * would mean a biome lookup per frame for a value that changes only when the
 * player walks somewhere else. This resolves it a few times a second and hands
 * out the cached answer, which is the same principle the ambient systems follow
 * by reusing vanilla's sampling rather than scanning the world themselves.
 *
 * <h2>Crossing a border is blended, not switched</h2>
 *
 * <p>A hard switch at a biome edge would be visible, since fog density and wind
 * strength would step as the player walked across a line. The character itself
 * changes at once, but a blend value eases from 0 to 1 afterwards, and callers
 * interpolate from the previous character's multipliers to the new one's. The
 * result is that walking from forest into desert thickens the air gradually
 * rather than at a boundary.
 */
public final class EnvironmentTracker {

	/** Ticks between classifications. Five a second is far more than walking needs. */
	private static final int UPDATE_INTERVAL_TICKS = 4;

	/** How quickly a new character takes over, per tick. Roughly three seconds end to end. */
	private static final float BLEND_RATE = 0.017F;

	private static BiomeCharacter current = BiomeCharacter.TEMPERATE;
	private static BiomeCharacter previous = BiomeCharacter.TEMPERATE;

	/** Progress from the previous character to the current one, 0 to 1. */
	private static float blend = 1.0F;

	private static int tickCounter;

	private EnvironmentTracker() {
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(EnvironmentTracker::onEndTick);
	}

	private static void onEndTick(Minecraft client) {
		ClientLevel level = client.level;
		Player player = client.player;

		if (level == null || player == null) {
			reset();
			return;
		}

		blend = Math.min(1.0F, blend + BLEND_RATE);

		if (++tickCounter < UPDATE_INTERVAL_TICKS) {
			return;
		}

		tickCounter = 0;

		BiomeCharacter found = BiomeCharacter.of(level, level.getBiome(player.blockPosition()), player.blockPosition());

		if (found != current) {
			// Blending starts from whatever was on screen a moment ago, not from
			// the character before that, so crossing two borders quickly does not
			// jump back to a value that was never reached.
			previous = blend >= 1.0F ? current : previous;
			current = found;
			blend = 0.0F;
		}
	}

	/** The character of the player's surroundings. */
	public static BiomeCharacter current() {
		return current;
	}

	/** Fog multiplier for here, scaled by the user's settings and eased across a border. */
	public static float fogMultiplier() {
		BiomeSettings config = CinematicConfig.biomes();

		return Mth.lerp(blend,
				config.apply(previous, previous.fogMultiplier()),
				config.apply(current, current.fogMultiplier()));
	}

	/** Wind multiplier for here, scaled by the user's settings and eased across a border. */
	public static float windMultiplier() {
		BiomeSettings config = CinematicConfig.biomes();

		return Mth.lerp(blend,
				config.apply(previous, previous.windMultiplier()),
				config.apply(current, current.windMultiplier()));
	}

	/** Clears carried state so re-entering a world does not inherit the previous one's character. */
	public static void reset() {
		current = BiomeCharacter.TEMPERATE;
		previous = BiomeCharacter.TEMPERATE;
		blend = 1.0F;
		tickCounter = 0;
	}
}
