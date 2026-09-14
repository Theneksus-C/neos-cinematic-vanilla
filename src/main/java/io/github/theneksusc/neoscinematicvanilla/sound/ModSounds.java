package io.github.theneksusc.neoscinematicvanilla.sound;

import io.github.theneksusc.neoscinematicvanilla.NeosCinematicVanillaClient;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/**
 * Sound events this mod adds.
 *
 * <p>Vanilla has no ambient wind sound. Its only wind audio is the Breeze's
 * wind charge burst, which is a short attack sound rather than something that
 * can loop, so the loop ships with the mod.
 *
 * <p>The audio is declared with {@code "stream": true} in {@code sounds.json}.
 * The file runs nearly five minutes in stereo, which would decompress to
 * roughly fifty megabytes of PCM if buffered into memory. Streaming reads it
 * from disk as it plays instead.
 */
public final class ModSounds {

	/** Looping wind ambience. Volume is driven by wind strength, not played as a one shot. */
	public static final Identifier AMBIENT_WIND_ID =
			Identifier.fromNamespaceAndPath(NeosCinematicVanillaClient.MOD_ID, "ambient.wind");

	public static final SoundEvent AMBIENT_WIND = SoundEvent.createVariableRangeEvent(AMBIENT_WIND_ID);

	private ModSounds() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.SOUND_EVENT, AMBIENT_WIND_ID, AMBIENT_WIND);
	}
}
