package io.github.theneksusc.neoscinematicvanilla.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * Key bindings this mod adds.
 *
 * <p>A key binding is how the settings screen is reached without depending on
 * any other mod. It appears in the vanilla controls list, so a user can rebind
 * or clear it like any other key.
 *
 * <p>The default is unbound rather than a chosen key. Claiming a key by default
 * risks colliding with something the player already uses, and a settings screen
 * is not opened often enough to justify that.
 */
public final class ModKeys {

	/** Shown as the group heading in the vanilla controls screen. */
	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.MISC;

	private static KeyMapping openConfig;

	private ModKeys() {
	}

	public static void register() {
		openConfig = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.neoscinematicvanilla.open_config",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_UNKNOWN,
				CATEGORY));

		ClientTickEvents.END_CLIENT_TICK.register(ModKeys::onEndTick);
	}

	/**
	 * Opens the settings screen when the key is pressed.
	 *
	 * <p>{@code consumeClick} reports each press exactly once and clears it, so
	 * holding the key does not reopen the screen every tick.
	 */
	private static void onEndTick(Minecraft client) {
		if (openConfig == null) {
			return;
		}

		while (openConfig.consumeClick()) {
			// Screen state moved onto Gui in 26.2. Opening one while another is
			// already open would stack them.
			if (client.gui.screen() == null) {
				client.setScreenAndShow(new ConfigScreen(null));
			}
		}
	}
}
