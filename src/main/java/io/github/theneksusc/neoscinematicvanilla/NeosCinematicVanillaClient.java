package io.github.theneksusc.neoscinematicvanilla;

import io.github.theneksusc.neoscinematicvanilla.config.CinematicConfig;
import io.github.theneksusc.neoscinematicvanilla.gui.ModKeys;
import io.github.theneksusc.neoscinematicvanilla.particle.DustMoteParticle;
import io.github.theneksusc.neoscinematicvanilla.particle.ModParticles;
import io.github.theneksusc.neoscinematicvanilla.sound.ModSounds;
import io.github.theneksusc.neoscinematicvanilla.wind.WindDriver;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client entry point for Neo's Cinematic Vanilla.
 *
 * <p>Fabric calls {@link #onInitializeClient()} once during client startup,
 * after the game is in a mod-load-ready state but before the main menu appears.
 * Atmosphere systems are registered here as they are built.
 *
 * <p>The mod is declared client-only in {@code fabric.mod.json}, so Fabric will
 * refuse to load it on a dedicated server rather than failing at runtime.
 */
public class NeosCinematicVanillaClient implements ClientModInitializer {

	/** Namespace used for every resource this mod registers. Matches the id in fabric.mod.json. */
	public static final String MOD_ID = "neoscinematicvanilla";

	/** Naming the logger after the mod id makes the source of each log line obvious. */
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		CinematicConfig.load();

		// The type must exist before anything can spawn it, and the provider
		// must be registered before the particle engine loads its sprites.
		ModParticles.register();
		ParticleProviderRegistry.getInstance().register(ModParticles.DUST_MOTE, DustMoteParticle.Provider::new);

		ModSounds.register();
		ModKeys.register();
		WindDriver.register();

		LOGGER.info("Neo's Cinematic Vanilla initialised");
	}
}
