package io.github.theneksusc.neoscinematicvanilla.wind;

import io.github.theneksusc.neoscinematicvanilla.config.CinematicConfig;
import io.github.theneksusc.neoscinematicvanilla.config.WindSettings;
import io.github.theneksusc.neoscinematicvanilla.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * The looping wind ambience, whose volume follows {@link WindSystem#strength()}.
 *
 * <p>Started once and left running for the life of the world. Rather than being
 * started and stopped as conditions change, it simply falls to silence when the
 * wind drops, which avoids the click and the restart delay that stopping and
 * restarting a streamed sound would cause.
 *
 * <p>Marked relative, so it follows the player rather than sitting at a world
 * position. Wind is everywhere at once, not somewhere in particular.
 */
public class WindSoundInstance extends AbstractTickableSoundInstance {

	/**
	 * How quickly volume follows wind strength, per tick. Slower than the wind
	 * model itself, because the ear notices a volume step far more readily than
	 * it notices particles changing course.
	 */
	private static final float VOLUME_EASE = 0.02F;

	public WindSoundInstance() {
		super(ModSounds.AMBIENT_WIND, SoundSource.AMBIENT, RandomSource.create());

		this.looping = true;
		this.delay = 0;
		this.volume = 0.0F;

		// Follows the player rather than a fixed point in the world.
		this.relative = true;

		// Starts silent and rises, so joining a world never begins with a blast.
		this.pitch = 1.0F;
	}

	@Override
	public void tick() {
		Minecraft client = Minecraft.getInstance();

		if (client.level == null) {
			this.volume = 0.0F;
			return;
		}

		WindSettings config = CinematicConfig.wind();

		this.volume += (targetVolume(config) - this.volume) * VOLUME_EASE;
	}

	/**
	 * Maps wind strength onto a volume.
	 *
	 * <p>Below the configured threshold the loop is fully silent rather than
	 * merely quiet, which stops a constant faint hiss indoors and in sheltered
	 * places. Above it, volume is remapped from the threshold rather than from
	 * zero, so crossing the line fades in smoothly instead of jumping.
	 */
	private static float targetVolume(WindSettings config) {
		if (!config.enabled || !config.soundEnabled) {
			return 0.0F;
		}

		float strength = WindSystem.strength();
		float threshold = Mth.clamp(config.soundThreshold, 0.0F, 0.95F);

		if (strength <= threshold) {
			return 0.0F;
		}

		float above = (strength - threshold) / (1.0F - threshold);

		return Mth.clamp(above * config.soundVolume, 0.0F, 1.0F);
	}

	/**
	 * Allows the engine to start this sound while it is silent.
	 *
	 * <p>Without this the sound never plays at all. {@code SoundEngine.play}
	 * rejects any instance whose volume is zero at the moment it is submitted,
	 * and this one deliberately starts silent so that entering a world never
	 * begins with a blast of wind. Vanilla uses the same override for sounds
	 * that fade in, such as the bee and minecart loops.
	 */
	@Override
	public boolean canStartSilent() {
		return true;
	}

	/** Never stops on its own, so the stream is not restarted as conditions change. */
	@Override
	public boolean isStopped() {
		return false;
	}
}
