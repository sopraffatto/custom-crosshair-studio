package net.customcrosshairstudio.render.styles;

import net.customcrosshairstudio.config.CrosshairProfile;
import net.customcrosshairstudio.render.CrosshairStyle;
import net.customcrosshairstudio.render.RenderContext;
import net.customcrosshairstudio.render.RenderUtils;
import net.minecraft.client.gui.DrawContext;

/**
 * Dedicated precision dot crosshair style.
 * Uses unified Crosshair Color and pure black outline.
 */
public class DotCrosshairStyle implements CrosshairStyle {

    @Override
    public void render(DrawContext context, RenderContext ctx, CrosshairProfile config) {
        RenderUtils.drawDot(context, ctx.centerX, ctx.centerY, config.dotSize,
                ctx.finalColor, config.outline, config.outlineThickness, config.outlineColor);
    }
}
