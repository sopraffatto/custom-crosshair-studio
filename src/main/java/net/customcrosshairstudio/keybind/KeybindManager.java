package net.customcrosshairstudio.keybind;

import net.customcrosshairstudio.CrosshairStudioClient;
import net.customcrosshairstudio.gui.CrosshairStudioScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Manages client keybindings for opening the config screen and toggling crosshair.
 */
public class KeybindManager {
    public static KeyBinding openConfigKey;
    public static KeyBinding toggleCrosshairKey;

    public static void register() {
        openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.customcrosshairstudio.open_config",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN, // Unbound by default, configurable in Controls menu
                "category.customcrosshairstudio"
        ));

        toggleCrosshairKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.customcrosshairstudio.toggle_crosshair",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                "category.customcrosshairstudio"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openConfigKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new CrosshairStudioScreen(null));
                }
            }

            while (toggleCrosshairKey.wasPressed()) {
                var config = CrosshairStudioClient.getConfig();
                if (config != null) {
                    config.enabled = !config.enabled;
                    net.customcrosshairstudio.config.ConfigManager.save();
                }
            }
        });
    }
}
