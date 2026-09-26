package net.customcrosshairstudio.render.styles;

import net.customcrosshairstudio.config.CrosshairProfile;
import net.customcrosshairstudio.render.CrosshairStyle;
import net.customcrosshairstudio.render.RenderContext;
import net.customcrosshairstudio.render.RenderUtils;
import net.minecraft.client.gui.DrawContext;

/** User-drawn pixel pattern rendered cell by cell around the crosshair anchor, optionally inverting the background. */
public class DrawnCrosshairStyle implements CrosshairStyle {

    @Override
    public void render(DrawContext context, RenderContext ctx, CrosshairProfile config) {
        if (config.drawnInvert) {
            RenderUtils.drawDrawnInverted(context, ctx.centerX, ctx.centerY, config.drawn);
            return;
        }
        RenderUtils.drawDrawn(context, ctx.centerX, ctx.centerY, config.drawn,
                ctx.finalColor, config.outline, config.outlineThickness, config.outlineColor);
    }
}
