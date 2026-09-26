package net.customcrosshairstudio.render;

import net.customcrosshairstudio.config.DrawnPattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

/**
 * Crosshair renderer working in framebuffer pixels, so the crosshair keeps the same physical
 * size at every GUI scale. One "unit" (thickness, arm length, dot size) is
 * {@link #UNIT} framebuffer pixels; gap and outline width are single framebuffer pixels.
 * All shapes are built from integer rectangles around one integer centre, which keeps them
 * symmetric. The outline is a coloured border drawn outside the fill, beneath it.
 */
public final class RenderUtils {

    /** Framebuffer pixels per crosshair unit. Even, so bars and dots centre exactly on the anchor. */
    public static final int UNIT = 2;

    private RenderUtils() {}

    /** Shape parameters in framebuffer pixels, centred on integer (cx, cy). */
    public record Layout(int cx, int cy, int half, int length, int gap) {
        /** {@code centerX/Y} are in GUI coordinates; {@code scale} is the GUI scale factor. */
        public static Layout of(float centerX, float centerY, float thickness, float armLength, float gap, int scale) {
            return new Layout(Math.round(centerX * scale), Math.round(centerY * scale),
                    Math.max(1, Math.round(thickness)) * UNIT / 2,
                    Math.max(1, Math.round(armLength)) * UNIT,
                    Math.max(0, Math.round(gap)));
        }

        /**
         * The four arms as {x1, y1, x2, y2}: right, left, bottom, top. Each starts {@code gap} px from
         * the centre and reaches {@code half + length} beyond it, so with gap 0 they join into one plus.
         */
        public int[][] arms() {
            int inner = gap;
            int outer = gap + half + length;
            return new int[][]{
                    {cx + inner, cy - half, cx + outer, cy + half},
                    {cx - outer, cy - half, cx - inner, cy + half},
                    {cx - half, cy + inner, cx + half, cy + outer},
                    {cx - half, cy - outer, cx + half, cy - inner}};
        }

        /** Centre dot as {x1, y1, x2, y2}; {@code dotSize} is in units. */
        public int[] dot(float dotSize) {
            int d = Math.max(1, Math.round(dotSize)) * UNIT / 2;
            return new int[]{cx - d, cy - d, cx + d, cy + d};
        }
    }

    private static int guiScale() {
        return Math.max(1, (int) Math.round(MinecraftClient.getInstance().getWindow().getScaleFactor()));
    }

    private static void fillRect(DrawContext c, int[] r, int grow, int colour) {
        if (r[0] >= r[2] || r[1] >= r[3]) return;
        c.fill(r[0] - grow, r[1] - grow, r[2] + grow, r[3] + grow, colour);
    }

    /** Runs {@code body} with the matrix scaled so that one drawn unit is one framebuffer pixel. */
    private static void inFramebufferSpace(DrawContext context, int scale, Runnable body) {
        context.getMatrices().pushMatrix();
        context.getMatrices().scale(1.0f / scale, 1.0f / scale);
        try {
            body.run();
        } finally {
            context.getMatrices().popMatrix();
        }
    }

    public static void drawCrosshair4Arm(DrawContext context, float centerX, float centerY,
                                         float thickness, float armLength, float gap,
                                         int fillColor, boolean outline, float outlinePx, int outlineColour,
                                         boolean hasDot, float dotSize) {
        int s = guiScale();
        Layout l = Layout.of(centerX, centerY, thickness, armLength, gap, s);
        int[][] arms = l.arms();
        int[] dot = hasDot ? l.dot(dotSize) : null;
        int grow = Math.max(1, Math.round(outlinePx));

        inFramebufferSpace(context, s, () -> {
            if (outline) {
                for (int[] arm : arms) fillRect(context, arm, grow, outlineColour);
                if (dot != null) fillRect(context, dot, grow, outlineColour);
            }
            for (int[] arm : arms) fillRect(context, arm, 0, fillColor);
            if (dot != null) fillRect(context, dot, 0, fillColor);
        });
    }

    public static void drawDot(DrawContext context, float centerX, float centerY, float dotSize,
                               int dotColor, boolean outline, float outlinePx, int outlineColour) {
        int s = guiScale();
        int[] dot = Layout.of(centerX, centerY, 1, 1, 0, s).dot(dotSize);
        int grow = Math.max(1, Math.round(outlinePx));

        inFramebufferSpace(context, s, () -> {
            if (outline) fillRect(context, dot, grow, outlineColour);
            fillRect(context, dot, 0, dotColor);
        });
    }

    /**
     * Framebuffer-pixel origin (top-left) of the drawn grid. Drawn cells are whole GUI pixels (the same
     * size as the vanilla crosshair pixels at the current GUI scale); the centre cell sits on the same GUI
     * pixel as vanilla's crosshair centre.
     */
    public static int[] drawnOrigin(float centerX, float centerY, int scale) {
        return new int[]{
                ((int) Math.floor(centerX - 0.5f) - DrawnPattern.CENTER) * scale,
                ((int) Math.floor(centerY - 0.5f) - DrawnPattern.CENTER) * scale};
    }

    /** Renders each enabled cell as one GUI pixel (horizontal runs merged); outline sits outside, beneath. */
    public static void drawDrawn(DrawContext context, float centerX, float centerY, List<String> rows,
                                 int fillColor, boolean outline, float outlinePx, int outlineColour) {
        if (rows == null || DrawnPattern.isEmpty(rows)) return;
        int s = guiScale();
        int[] o = drawnOrigin(centerX, centerY, s);
        int grow = Math.max(1, Math.round(outlinePx));

        inFramebufferSpace(context, s, () -> {
            for (int pass = outline ? 0 : 1; pass < 2; pass++) {
                int colour = pass == 0 ? outlineColour : fillColor;
                int g = pass == 0 ? grow : 0;
                for (int y = 0; y < DrawnPattern.SIZE; y++) {
                    int x = 0;
                    while (x < DrawnPattern.SIZE) {
                        if (!DrawnPattern.isSet(rows, x, y)) { x++; continue; }
                        int start = x;
                        while (x < DrawnPattern.SIZE && DrawnPattern.isSet(rows, x, y)) x++;
                        fillRect(context, new int[]{o[0] + start * s, o[1] + y * s,
                                o[0] + x * s, o[1] + (y + 1) * s}, g, colour);
                    }
                }
            }
        });
    }

    /** Drawn pattern rendered as single-pass background inversion (cell runs never overlap). */
    public static void drawDrawnInverted(DrawContext context, float centerX, float centerY, List<String> rows) {
        if (rows == null || DrawnPattern.isEmpty(rows)) return;
        int s = guiScale();
        int[] o = drawnOrigin(centerX, centerY, s);

        inFramebufferSpace(context, s, () -> {
            for (int y = 0; y < DrawnPattern.SIZE; y++) {
                int x = 0;
                while (x < DrawnPattern.SIZE) {
                    if (!DrawnPattern.isSet(rows, x, y)) { x++; continue; }
                    int start = x;
                    while (x < DrawnPattern.SIZE && DrawnPattern.isSet(rows, x, y)) x++;
                    fillInvert(context, o[0] + start * s, o[1] + y * s, o[0] + x * s, o[1] + (y + 1) * s);
                }
            }
        });
    }

    /**
     * Splits the union of overlapping rectangles into disjoint tiles (coordinate compression), so
     * an inverting fill never hits the same pixel twice.
     */
    public static List<int[]> disjointUnion(List<int[]> rects) {
        TreeSet<Integer> xs = new TreeSet<>();
        TreeSet<Integer> ys = new TreeSet<>();
        for (int[] r : rects) {
            if (r[0] >= r[2] || r[1] >= r[3]) continue;
            xs.add(r[0]); xs.add(r[2]); ys.add(r[1]); ys.add(r[3]);
        }
        Integer[] xa = xs.toArray(new Integer[0]);
        Integer[] ya = ys.toArray(new Integer[0]);
        List<int[]> tiles = new ArrayList<>();
        for (int j = 0; j + 1 < ya.length; j++) {
            int runStart = -1;
            for (int i = 0; i <= xa.length - 1; i++) {
                boolean covered = i + 1 < xa.length && covers(rects, xa[i], ya[j]);
                if (covered && runStart < 0) runStart = xa[i];
                if (!covered && runStart >= 0) {
                    tiles.add(new int[]{runStart, ya[j], xa[i], ya[j + 1]});
                    runStart = -1;
                }
            }
        }
        return tiles;
    }

    private static boolean covers(List<int[]> rects, int x, int y) {
        for (int[] r : rects) {
            if (x >= r[0] && x < r[2] && y >= r[1] && y < r[3]) return true;
        }
        return false;
    }

    /** Inverts a rectangle using the GUI_INVERT pipeline in a single pass. */
    public static void fillInvert(DrawContext context, int x1, int y1, int x2, int y2) {
        if (x1 > x2) { int t = x1; x1 = x2; x2 = t; }
        if (y1 > y2) { int t = y1; y1 = y2; y2 = t; }
        if (x1 == x2 || y1 == y2) return;
        context.fill(RenderPipelines.GUI_INVERT, x1, y1, x2, y2, 0xFFFFFFFF);
    }

    public static void drawInvertedCrosshair(DrawContext context, float centerX, float centerY,
                                             float thickness, float armLength, float gap,
                                             boolean hasDot, float dotSize) {
        int s = guiScale();
        Layout l = Layout.of(centerX, centerY, thickness, armLength, gap, s);
        List<int[]> shapes = new ArrayList<>(Arrays.asList(l.arms()));
        if (hasDot) shapes.add(l.dot(dotSize));
        List<int[]> tiles = disjointUnion(shapes);

        inFramebufferSpace(context, s, () -> {
            for (int[] t : tiles) fillInvert(context, t[0], t[1], t[2], t[3]);
        });
    }
}
