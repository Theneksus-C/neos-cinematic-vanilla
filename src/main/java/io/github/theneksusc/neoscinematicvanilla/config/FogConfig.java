package io.github.theneksusc.neoscinematicvanilla.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.theneksusc.neoscinematicvanilla.NeosCinematicVanillaClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * User-facing fog settings, stored as JSON next to the game's other configs.
 *
 * <p>Every field is a plain public value so that the JSON maps one to one onto
 * this class with no custom serialisation. Defaults are chosen to be noticeable
 * but restrained.
 *
 * <p>The file is re-read when its modification time changes, so values can be
 * tuned without restarting the game.
 */
public class FogConfig {

	/** Master switch. When false the mod leaves vanilla fog completely alone. */
	public boolean enabled = true;

	/**
	 * Scales every effect below. 0 disables fog changes entirely while leaving
	 * the mod loaded, 1 is the tuned default, values above 1 exaggerate.
	 */
	public float intensity = 1.0F;

	/** Haze that builds up at low elevations and clears on high ground. */
	public float altitudeInfluence = 1.0F;

	/** Density added underground, driven by how much sky light reaches the camera. */
	public float caveInfluence = 1.0F;

	/** Additional density during rain and thunderstorms. */
	public float weatherInfluence = 1.0F;

	/** Scales where fog begins. Above 1 pushes the near edge further away. */
	public float startDistance = 1.0F;

	/** Scales where fog reaches full opacity. Above 1 pushes the far edge further away. */
	public float endDistance = 1.0F;

	/**
	 * Whether to also affect dimensions that deliberately define their own fog,
	 * such as the Nether. Off by default so authored looks are preserved.
	 */
	public boolean overrideCustomDimensionFog = false;

	/**
	 * How quickly fog reacts to a change in surroundings, per tick. Lower values
	 * are smoother. Prevents visible popping when moving between light levels.
	 */
	public float transitionSpeed = 0.08F;

	/**
	 * Logs the resulting fog band and the fog fraction it produces at 64, 128,
	 * and 256 blocks, roughly twice per second. Intended for tuning and for
	 * diagnosing whether an effect is present but too weak to see.
	 */
	public boolean debugLogging = false;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String FILE_NAME = "neoscinematicvanilla.json";

	private static FogConfig instance = new FogConfig();
	private static Path path;
	private static long lastModified = -1L;
	private static long lastCheckMillis;

	/** Minimum gap between modification time checks, to avoid touching the disk every frame. */
	private static final long CHECK_INTERVAL_MILLIS = 1000L;

	public static FogConfig get() {
		return instance;
	}

	/** Reads the config from disk, writing a default file first if none exists. */
	public static void load() {
		path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);

		try {
			if (!Files.exists(path)) {
				save();
				NeosCinematicVanillaClient.LOGGER.info("Wrote default fog config to {}", path);
				return;
			}

			String json = Files.readString(path);
			FogConfig parsed = GSON.fromJson(json, FogConfig.class);

			if (parsed != null) {
				instance = parsed;
			}

			lastModified = Files.getLastModifiedTime(path).toMillis();
		} catch (Exception e) {
			NeosCinematicVanillaClient.LOGGER.error("Failed to read fog config, keeping previous values", e);
		}
	}

	/** Writes current values to disk. */
	public static void save() {
		if (path == null) {
			path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
		}

		try {
			Files.createDirectories(path.getParent());
			Files.writeString(path, GSON.toJson(instance));
			lastModified = Files.getLastModifiedTime(path).toMillis();
		} catch (IOException e) {
			NeosCinematicVanillaClient.LOGGER.error("Failed to write fog config", e);
		}
	}

	/**
	 * Reloads the file if it changed on disk. Rate limited, so calling this
	 * every frame costs one clock read in the common case.
	 */
	public static void reloadIfChanged() {
		long now = System.currentTimeMillis();

		if (now - lastCheckMillis < CHECK_INTERVAL_MILLIS) {
			return;
		}

		lastCheckMillis = now;

		if (path == null) {
			return;
		}

		try {
			if (!Files.exists(path)) {
				return;
			}

			long modified = Files.getLastModifiedTime(path).toMillis();

			if (modified != lastModified) {
				load();
				NeosCinematicVanillaClient.LOGGER.info("Reloaded fog config");
			}
		} catch (IOException e) {
			// A transient read failure is not worth reporting every second.
		}
	}
}
