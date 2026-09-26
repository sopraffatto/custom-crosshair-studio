package net.customcrosshairstudio.gui.ui;

import net.customcrosshairstudio.config.CrosshairProfile;
import net.customcrosshairstudio.render.CrosshairRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * Draws a profile through the real in-game renderer, centred on an integer GUI point and
 * optionally magnified by an integer zoom (so pixels stay square and crisp), clipped to a box.
 */
public final class Preview {
    private Preview() {}

    public static void draw(DrawContext c, CrosshairProfile profile, int cx, int cy, int zoom,
                            int clipX, int clipY, int clipW, int clipH) {
        if (clipW <= 0 || clipH <= 0) return;
        c.enableScissor(clipX, clipY, clipX + clipW, clipY + clipH);
        var m = c.getMatrices();
        m.pushMatrix();
        if (zoom > 1) {
            m.translate(cx, cy);
            m.scale(zoom, zoom);
            m.translate(-cx, -cy);
        }
        CrosshairRenderer.INSTANCE.renderPreview(c, profile, cx - 32, cy - 32, 64, 64);
        m.popMatrix();
        c.disableScissor();
    }

    /** Dark well with a faint dot grid, used behind small previews. */
    public static void well(DrawContext c, int x, int y, int w, int h) {
        Ui.rect(c, x, y, w, h, Ui.WELL);
        for (int gy = y + 3; gy < y + h - 1; gy += 4) {
            for (int gx = x + 3; gx < x + w - 1; gx += 4) Ui.rect(c, gx, gy, 1, 1, 0x14FFFFFF);
        }
    }
}
