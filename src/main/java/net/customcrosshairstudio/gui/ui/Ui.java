package net.customcrosshairstudio.gui.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.Map;

/**
 * Custom Crosshair Studio design system: colour tokens, a frame clock, keyed motion values that survive
 * widget rebuilds, and the small set of drawing primitives every widget uses.
 * Visual language: flat, square, hairline-separated surfaces; monochrome (black, greys, white
 * as the accent); red only for Enemy.
 */
public final class Ui {
    public static final int SCRIM = 0xC805060A;
    public static final int APP = 0xFF0B0C0F;
    public static final int SURFACE = 0xFF101216;
    public static final int RAISED = 0xFF16191E;
    public static final int RAISED_HI = 0xFF1D2127;
    public static final int WELL = 0xFF08090B;
    public static final int LINE = 0xFF20242B;
    public static final int LINE_HI = 0xFF323843;
    public static final int TEXT = 0xFFECEEF2;
    public static final int TEXT_2 = 0xFFA2A8B4;
    public static final int TEXT_3 = 0xFF626977;
    public static final int ACCENT = 0xFFFFFFFF;
    public static final int ACCENT_INK = 0xFF0B0C0F;
    public static final int ENEMY = 0xFFFF4B5C;

    private static final Map<String, Float> MOTION = new HashMap<>();
    private static long lastNanos;
    private static float dt = 0.016f;

    private Ui() {}

    // ------------------------------------------------------------------ time & motion

    /** Advance the frame clock; call once at the start of every screen frame. */
    public static void tick() {
        long now = System.nanoTime();
        if (lastNanos != 0L) dt = Math.min(0.05f, (now - lastNanos) / 1_000_000_000f);
        lastNanos = now;
    }

    public static void resetMotion() {
        MOTION.clear();
        lastNanos = 0L;
    }

    /** Frame-rate independent exponential approach. */
    public static float approach(float cur, float target, float speed) {
        float next = cur + (target - cur) * (1.0f - (float) Math.exp(-speed * dt));
        return Math.abs(next - target) < 0.002f ? target : next;
    }

    /** Fraction of the remaining distance an exponential animation of {@code speed} covers this frame. */
    public static float step(float speed) {
        return 1.0f - (float) Math.exp(-speed * dt);
    }

    /** Keyed animated value; the first request snaps to {@code target}, later ones ease towards it. */
    public static float motion(String key, float target, float speed) {
        Float cur = MOTION.get(key);
        float v = cur == null ? target : approach(cur, target, speed);
        MOTION.put(key, v);
        return v;
    }

    public static float easeOut(float t) {
        t = clamp01(t);
        float u = 1f - t;
        return 1f - u * u * u;
    }

    public static float clamp01(float t) {
        return Math.max(0f, Math.min(1f, t));
    }

    // ------------------------------------------------------------------ colour

    public static int alpha(int argb, float a) {
        int na = Math.round(((argb >>> 24) & 0xFF) * clamp01(a));
        return (na << 24) | (argb & 0x00FFFFFF);
    }

    public static int mix(int a, int b, float t) {
        t = clamp01(t);
        int aa = (a >>> 24) & 0xFF, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >>> 24) & 0xFF, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return (Math.round(aa + (ba - aa) * t) << 24) | (Math.round(ar + (br - ar) * t) << 16)
                | (Math.round(ag + (bg - ag) * t) << 8) | Math.round(ab + (bb - ab) * t);
    }

    /** Relative luminance 0..1 of an ARGB colour (alpha ignored). */
    public static float luminance(int argb) {
        return (0.2126f * ((argb >> 16) & 0xFF) + 0.7152f * ((argb >> 8) & 0xFF) + 0.0722f * (argb & 0xFF)) / 255f;
    }

    // ------------------------------------------------------------------ shapes

    public static void rect(DrawContext c, int x, int y, int w, int h, int colour) {
        if (w <= 0 || h <= 0 || (colour >>> 24) == 0) return;
        c.fill(x, y, x + w, y + h, colour);
    }

    public static void frame(DrawContext c, int x, int y, int w, int h, int colour) {
        if (w <= 1 || h <= 1) return;
        rect(c, x, y, w, 1, colour);
        rect(c, x, y + h - 1, w, 1, colour);
        rect(c, x, y + 1, 1, h - 2, colour);
        rect(c, x + w - 1, y + 1, 1, h - 2, colour);
    }

    /** Four L-shaped corner marks, like a camera viewfinder / print crop marks. */
    public static void cropMarks(DrawContext c, int x, int y, int w, int h, int len, int colour) {
        rect(c, x, y, len, 1, colour);
        rect(c, x, y, 1, len, colour);
        rect(c, x + w - len, y, len, 1, colour);
        rect(c, x + w - 1, y, 1, len, colour);
        rect(c, x, y + h - 1, len, 1, colour);
        rect(c, x, y + h - len, 1, len, colour);
        rect(c, x + w - len, y + h - 1, len, 1, colour);
        rect(c, x + w - 1, y + h - len, 1, len, colour);
    }

    // ------------------------------------------------------------------ type

    public static TextRenderer font() {
        return MinecraftClient.getInstance().textRenderer;
    }

    public static int width(String s) {
        return font().getWidth(s);
    }

    public static void text(DrawContext c, String s, int x, int y, int colour) {
        c.drawText(font(), s, x, y, colour, false);
    }

    public static void textRight(DrawContext c, String s, int right, int y, int colour) {
        text(c, s, right - width(s), y, colour);
    }

    public static void textCentered(DrawContext c, String s, int cx, int y, int colour) {
        text(c, s, cx - width(s) / 2, y, colour);
    }

    public static void bold(DrawContext c, String s, int x, int y, int colour) {
        c.drawText(font(), Text.literal(s).formatted(Formatting.BOLD), x, y, colour, false);
    }

    public static int boldWidth(String s) {
        return font().getWidth(Text.literal(s).formatted(Formatting.BOLD));
    }

    /** Trims {@code s} with ".." so that it fits {@code maxW} pixels. */
    public static String fit(String s, int maxW) {
        if (maxW <= 0) return "";
        TextRenderer tr = font();
        if (tr.getWidth(s) <= maxW) return s;
        int dots = tr.getWidth("..");
        if (maxW <= dots) return "";
        return tr.trimToWidth(s, maxW - dots) + "..";
    }
}
