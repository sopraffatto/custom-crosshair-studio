package net.customcrosshairstudio.render;

import net.customcrosshairstudio.config.CrosshairProfile;
import net.minecraft.client.gui.DrawContext;

/**
 * Strategy interface for modular crosshair rendering geometries.
 * Each implementation provides distinct geometric mathematics and zero-allocation draw calls.
 */
public interface CrosshairStyle {
    /**
     * Renders the crosshair geometry.
     *
     * @param context the vanilla DrawContext
     * @param ctx the reusable frame render context
     * @param config the active crosshair profile (normal or enemy)
     */
    void render(DrawContext context, RenderContext ctx, CrosshairProfile config);
}
