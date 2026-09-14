package io.github.theneksusc.neoscinematicvanilla.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import io.github.theneksusc.neoscinematicvanilla.NeosCinematicVanillaClient;
import io.github.theneksusc.neoscinematicvanilla.config.CinematicConfig;
import io.github.theneksusc.neoscinematicvanilla.config.GrainSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.Identifier;

/**
 * Applies a film grain pass over the rendered world.
 *
 * <p>Runs after vanilla's own post effect and before the interface is drawn, so
 * grain covers the world without touching the HUD, menus, or this mod's own
 * settings screen. Grain over a menu would read as a broken display rather than
 * as film.
 *
 * <p>One effect file per strength, because a post effect's uniforms are baked
 * into a GPU buffer when it is built and cannot be changed afterwards. Selecting
 * a different file is the supported way to change strength.
 *
 * <p>Vanilla keeps a single post effect slot, used by the creeper and spider
 * camera effects. This deliberately does not use that slot, so those still work.
 *
 * <h2>Known incompatibility</h2>
 *
 * <p>Shader packs replace the render pipeline wholesale, so grain will not
 * appear when one is loaded. Nothing here fails in that case, the pass simply
 * has no effect worth seeing, which is the honest outcome rather than fighting
 * another mod for control of the frame.
 */
public final class FilmGrainRenderer {

	/** One effect per level, indexed by the configured level. Index 0 is unused. */
	private static final String[] EFFECT_NAMES = {null, "film_grain_light", "film_grain_medium", "film_grain_heavy"};

	private static final Identifier[] EFFECT_IDS = new Identifier[EFFECT_NAMES.length];

	/** Set once a level has failed, so a broken shader is not retried every frame. */
	private static final boolean[] FAILED = new boolean[EFFECT_NAMES.length];

	static {
		for (int i = 1; i < EFFECT_NAMES.length; i++) {
			EFFECT_IDS[i] = Identifier.fromNamespaceAndPath(
					NeosCinematicVanillaClient.MOD_ID, EFFECT_NAMES[i]);
		}
	}

	private FilmGrainRenderer() {
	}

	/**
	 * Runs the configured grain pass, if any.
	 *
	 * <p>Called every frame, so the disabled path is a single field read and a
	 * comparison.
	 *
	 * <p>{@code PostChain.process} is deprecated in favour of adding passes to
	 * the main frame graph, but vanilla's own renderer still calls it at exactly
	 * this point for the creeper and spider effects. The non deprecated path
	 * needs a {@code FrameGraphBuilder}, which does not exist at this injection
	 * site, and matching what vanilla does here is worth more than avoiding the
	 * warning.
	 */
	@SuppressWarnings("deprecation")
	public static void apply(RenderTarget target, GraphicsResourceAllocator allocator) {
		GrainSettings config = CinematicConfig.grain();

		int level = config.level;

		if (level <= GrainSettings.LEVEL_OFF || level >= EFFECT_IDS.length || FAILED[level]) {
			return;
		}

		Minecraft client = Minecraft.getInstance();

		// Nothing to grain outside a world, and the main target holds the menu
		// at that point rather than the level.
		if (client.level == null) {
			return;
		}

		PostChain chain = client.getShaderManager().getPostChain(EFFECT_IDS[level], LevelTargetBundle.MAIN_TARGETS);

		if (chain == null) {
			// A missing or uncompilable effect would otherwise be looked up
			// every frame for the rest of the session.
			FAILED[level] = true;
			NeosCinematicVanillaClient.LOGGER.warn(
					"Film grain effect {} could not be loaded, disabling that level", EFFECT_IDS[level]);
			return;
		}

		chain.process(target, allocator);
	}

	/** Clears the failure flags, so a resource reload can recover a fixed effect. */
	public static void onResourceReload() {
		java.util.Arrays.fill(FAILED, false);
	}
}
