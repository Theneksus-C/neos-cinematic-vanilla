package io.github.theneksusc.neoscinematicvanilla.wind;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

/**
 * Drives the wind model and keeps its ambient loop alive.
 *
 * <p>Separated from {@link WindSystem} so the model stays a plain calculation
 * with no knowledge of ticking or of the sound engine, which keeps it readable
 * and reasonable about on its own.
 */
public final class WindDriver {

	private static WindSoundInstance soundInstance;

	/** Tracks the level the loop was started for, so a world change restarts it. */
	private static ClientLevel activeLevel;

	private WindDriver() {
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(WindDriver::onEndTick);
	}

	private static void onEndTick(Minecraft client) {
		ClientLevel level = client.level;

		if (level == null) {
			// Leaving a world invalidates the instance. A fresh one is started
			// on the next world, rather than carrying strength across.
			if (activeLevel != null) {
				activeLevel = null;
				soundInstance = null;
				WindSystem.reset();
			}

			return;
		}

		if (level != activeLevel) {
			activeLevel = level;
			soundInstance = null;
			WindSystem.reset();
		}

		WindSystem.tick(client);
		ensureSoundPlaying(client);
	}

	/**
	 * Starts the loop once per world and leaves it running.
	 *
	 * <p>The instance never stops itself, falling to silence instead when wind
	 * drops, so this only has to start it. Restarting a streamed sound would
	 * cost a decode delay and a click every time conditions crossed a threshold.
	 */
	private static void ensureSoundPlaying(Minecraft client) {
		if (soundInstance != null) {
			return;
		}

		soundInstance = new WindSoundInstance();
		client.getSoundManager().play(soundInstance);
	}
}
