package net.customcrosshairstudio.render.styles;

import net.customcrosshairstudio.config.CrosshairProfile;
import net.customcrosshairstudio.render.CrosshairStyle;
import net.customcrosshairstudio.render.RenderContext;
import net.customcrosshairstudio.render.RenderUtils;
import net.minecraft.client.gui.DrawContext;

/**
 * Classic 4-arm customizable crosshair style.
 * Uses unified Crosshair Color for both arms and dot.
 */
public class ClassicCrosshairStyle implements CrosshairStyle {

    @Override
    public void render(DrawContext context, RenderContext ctx, CrosshairProfile config) {
        RenderUtils.drawCrosshair4Arm(context, ctx.centerX, ctx.centerY,
                config.thickness, config.length, config.gap, ctx.finalColor,
                config.outline, config.outlineThickness, config.outlineColor, config.dot, config.dotSize);
    }
}
