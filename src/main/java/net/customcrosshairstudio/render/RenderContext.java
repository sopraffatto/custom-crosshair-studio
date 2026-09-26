package net.customcrosshairstudio.render;

/**
 * Reusable per-frame render context (screen size, unrounded centre, colour).
 */
public class RenderContext {
    public int screenWidth;
    public int screenHeight;
    public float centerX;
    public float centerY;
    public float tickProgress;
    public int finalColor;
    public boolean isPreview = false;

    public void update(int screenWidth, int screenHeight, float tickProgress, int baseColor, boolean isPreview) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.centerX = screenWidth / 2.0f;
        this.centerY = screenHeight / 2.0f;
        this.tickProgress = tickProgress;
        this.finalColor = baseColor;
        this.isPreview = isPreview;
    }
}
