package io.github.theneksusc.neoscinematicvanilla.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.theneksusc.neoscinematicvanilla.NeosCinematicVanillaClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Root of the mod's configuration, stored as a single JSON file alongside the
 * game's other configs.
 *
 * <p>Each feature owns a nested section rather than adding fields to a flat
 * namespace, so the file stays readable as features are added.
 *
 * <p>The file is re-read when its modification time changes, which allows
 * values to be tuned without restarting the game. Java changes still require a
 * restart, since only data is reloaded here.
 */
public class CinematicConfig {

	public FogSettings fog = new FogSettings();
	public ParticleSettings particles = new ParticleSettings();
	public LeafSettings leaves = new LeafSettings();
	public WindSettings wind = new WindSettings();

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String FILE_NAME = "neoscinematicvanilla.json";

	/** Minimum gap between modification time checks, so the disk is not touched every frame. */
	private static final long CHECK_INTERVAL_MILLIS = 1000L;

	private static CinematicConfig instance = new CinematicConfig();
	private static Path path;
	private static long lastModified = -1L;
	private static long lastCheckMillis;

	/**
	 * Suppresses reloading while the settings screen is open.
	 *
	 * <p>A reload replaces the section objects, and a screen being edited at
	 * that moment would have its pending changes overwritten by whatever is on
	 * disk. The person editing in game is the more authoritative source, so the
	 * file is left alone until they finish.
	 */
	private static boolean reloadSuspended;

	public static void setReloadSuspended(boolean suspended) {
		reloadSuspended = suspended;
	}

	public static CinematicConfig get() {
		return instance;
	}

	public static FogSettings fog() {
		return instance.fog;
	}

	public static ParticleSettings particles() {
		return instance.particles;
	}

	public static LeafSettings leaves() {
		return instance.leaves;
	}

	public static WindSettings wind() {
		return instance.wind;
	}

	/** Reads the config from disk, writing a default file first if none exists. */
	public static void load() {
		path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);

		try {
			if (!Files.exists(path)) {
				save();
				NeosCinematicVanillaClient.LOGGER.info("Wrote default config to {}", path);
				return;
			}

			CinematicConfig parsed = GSON.fromJson(Files.readString(path), CinematicConfig.class);

			if (parsed != null) {
				// A section missing from the file leaves its field null, so any
				// gap is filled with defaults rather than crashing on first use.
				if (parsed.fog == null) {
					parsed.fog = new FogSettings();
				}

				if (parsed.particles == null) {
					parsed.particles = new ParticleSettings();
				}

				if (parsed.leaves == null) {
					parsed.leaves = new LeafSettings();
				}

				if (parsed.wind == null) {
					parsed.wind = new WindSettings();
				}

				instance = parsed;
			}

			lastModified = Files.getLastModifiedTime(path).toMillis();
		} catch (Exception e) {
			NeosCinematicVanillaClient.LOGGER.error("Failed to read config, keeping previous values", e);
		}
	}

	/**
	 * Restores every setting to its default.
	 *
	 * <p>Implemented by replacing the section objects rather than assigning each
	 * field back, so a field added later is covered automatically and cannot be
	 * forgotten here.
	 */
	public static void resetToDefaults() {
		instance.fog = new FogSettings();
		instance.particles = new ParticleSettings();
		instance.leaves = new LeafSettings();
		instance.wind = new WindSettings();
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
			NeosCinematicVanillaClient.LOGGER.error("Failed to write config", e);
		}
	}

	/**
	 * Reloads the file if it changed on disk. Rate limited, so calling this
	 * every frame costs one clock read in the common case.
	 */
	public static void reloadIfChanged() {
		if (reloadSuspended) {
			return;
		}

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
				NeosCinematicVanillaClient.LOGGER.info("Reloaded config");
			}
		} catch (IOException e) {
			// A transient read failure is not worth reporting every second.
		}
	}
}
