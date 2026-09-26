package net.customcrosshairstudio.config;

import java.util.ArrayList;
import java.util.List;

/**
 * One complete crosshair look. The normal crosshair is the config itself (which extends this,
 * keeping the existing flat JSON keys); the enemy crosshair is a separate instance.
 */
public class CrosshairProfile {
    public CrosshairStudioConfig.StyleType style = CrosshairStudioConfig.StyleType.CLASSIC;

    // Unified colour (ARGB) for arms, dot and drawn pixels
    public int color = 0xFFFFFFFF;

    public boolean outline = false;
    // Outline width in framebuffer pixels, independent of GUI scale
    public float outlineThickness = 1.0f;
    // Outline colour (always opaque); black by default, so older configs look unchanged
    public int outlineColor = 0xFF000000;

    // Thickness/length are GUI pixels; gap is framebuffer pixels
    public float thickness = 1.0f;
    public float length = 1.0f;
    public float gap = 0.0f;

    public boolean dot = false;
    public float dotSize = 2.0f;

    // Drawn style pixel pattern (DrawnPattern.SIZE rows of '#'/'.')
    public List<String> drawn = DrawnPattern.defaultRows();
    // Drawn pixels invert the background instead of using colour/outline
    public boolean drawnInvert = false;

    public void normalizeProfile() {
        if (style == null) style = CrosshairStudioConfig.StyleType.CLASSIC;
        thickness = Math.round(clampFinite(thickness, 1.0f, 4.0f, 1.0f));
        length = Math.round(clampFinite(length, 1.0f, 6.0f, 1.0f));
        gap = Math.round(clampFinite(gap, 0.0f, 4.0f, 0.0f));
        dotSize = Math.round(clampFinite(dotSize, 1.0f, 4.0f, 2.0f));
        outlineThickness = Math.round(clampFinite(outlineThickness, 1.0f, 4.0f, 1.0f));
        outlineColor |= 0xFF000000;
        drawn = DrawnPattern.normalize(drawn);
    }

    public void resetProfile() {
        style = CrosshairStudioConfig.StyleType.CLASSIC;
        color = 0xFFFFFFFF;
        outline = false;
        outlineThickness = 1.0f;
        outlineColor = 0xFF000000;
        thickness = 1.0f;
        length = 1.0f;
        gap = 0.0f;
        dot = false;
        dotSize = 2.0f;
        drawn = DrawnPattern.defaultRows();
        drawnInvert = false;
    }

    public void copyProfileFrom(CrosshairProfile other) {
        if (other == null) return;
        style = other.style != null ? other.style : CrosshairStudioConfig.StyleType.CLASSIC;
        color = other.color;
        outline = other.outline;
        outlineThickness = other.outlineThickness;
        outlineColor = other.outlineColor;
        thickness = other.thickness;
        length = other.length;
        gap = other.gap;
        dot = other.dot;
        dotSize = other.dotSize;
        drawn = new ArrayList<>(DrawnPattern.normalize(other.drawn));
        drawnInvert = other.drawnInvert;
    }

    /** Default look for a fresh enemy profile: same shape as the default, in red. */
    public static CrosshairProfile enemyDefault() {
        CrosshairProfile p = new CrosshairProfile();
        p.color = 0xFFFF3B3B;
        return p;
    }

    static float clampFinite(float value, float min, float max, float fallback) {
        if (!Float.isFinite(value)) return fallback;
        return Math.max(min, Math.min(max, value));
    }
}
