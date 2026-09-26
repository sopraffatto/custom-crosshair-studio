package net.customcrosshairstudio.gui;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Mod Menu integration entrypoint.
 * Allows opening Custom Crosshair Studio's settings screen directly from Mod Menu.
 */
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return CrosshairStudioScreen::new;
    }
}
