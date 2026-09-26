package net.customcrosshairstudio;

import net.customcrosshairstudio.config.ConfigManager;
import net.customcrosshairstudio.config.CrosshairStudioConfig;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main client entrypoint for Custom Crosshair Studio.
 */
public class CrosshairStudioClient implements ClientModInitializer {
    public static final String MOD_ID = "customcrosshairstudio";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("[Custom Crosshair Studio] Initializing...");
        ConfigManager.load();
        net.customcrosshairstudio.keybind.KeybindManager.register();
        LOGGER.info("[Custom Crosshair Studio] Initialized.");
    }

    public static CrosshairStudioConfig getConfig() {
        return ConfigManager.getConfig();
    }
}
