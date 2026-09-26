package net.customcrosshairstudio.render.styles;

import net.customcrosshairstudio.config.CrosshairProfile;
import net.customcrosshairstudio.render.CrosshairStyle;
import net.customcrosshairstudio.render.RenderContext;
import net.customcrosshairstudio.render.RenderUtils;
import net.minecraft.client.gui.DrawContext;

/**
 * Inverted crosshair style using single-pass disjoint inversion.
 * Eliminates double-inversion hollows when geometry overlaps.
 */
public class InvertedBlendCrosshairStyle implements CrosshairStyle {

    @Override
    public void render(DrawContext context, RenderContext ctx, CrosshairProfile config) {
        RenderUtils.drawInvertedCrosshair(context, ctx.centerX, ctx.centerY,
                config.thickness, config.length, config.gap,
                config.dot, config.dotSize);
    }
}

