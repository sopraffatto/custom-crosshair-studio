package net.customcrosshairstudio.gui.ui.widgets;

import net.customcrosshairstudio.config.DrawnPattern;
import net.customcrosshairstudio.gui.ui.Ui;
import net.customcrosshairstudio.gui.ui.UiWidget;
import net.minecraft.client.gui.DrawContext;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * The Draw workspace canvas. Edits the same {@link DrawnPattern} rows the renderer reads.
 *
 * <ul>
 *   <li>Pen: a stroke paints, or erases when it starts on a lit pixel. Right button always erases.</li>
 *   <li>Eraser: a stroke erases.</li>
 *   <li>Symmetry mirrors every edit across the anchor (X, Y or both).</li>
 * </ul>
 * Lit cells use the profile colour, pop in when they change, and the cursor shows where the
 * next stroke (and its mirrors) will land.
 */
public class PixelCanvas extends UiWidget {
    public static final int TOOL_PEN = 0;
    public static final int TOOL_ERASER = 1;
    public static final int MIRROR_OFF = 0;
    public static final int MIRROR_X = 1;
    public static final int MIRROR_Y = 2;
    public static final int MIRROR_XY = 3;

    private static final int N = DrawnPattern.SIZE;
    private static final int C = DrawnPattern.CENTER;

    private final int cell;
    private final Supplier<List<String>> getter;
    private final Consumer<List<String>> setter;
    private final IntSupplier tool;
    private final IntSupplier mirror;
    private final IntSupplier colour;
    private final Supplier<Boolean> invert;
    private final Runnable beforeStroke;

    private final float[] pop = new float[N * N];
    private List<String> seen;
    private boolean paintValue;
    private int lastX = Integer.MIN_VALUE;
    private int lastY = Integer.MIN_VALUE;
    private int hoverX = -1;
    private int hoverY = -1;

    public PixelCanvas(int x, int y, int cell, String id, Supplier<List<String>> getter, Consumer<List<String>> setter,
                       IntSupplier tool, IntSupplier mirror, IntSupplier colour, Supplier<Boolean> invert,
                       Runnable beforeStroke) {
        super(x, y, cell * N, cell * N, id, "Pixel canvas");
        this.cell = cell;
        this.getter = getter;
        this.setter = setter;
        this.tool = tool;
        this.mirror = mirror;
        this.colour = colour;
        this.invert = invert;
        this.beforeStroke = beforeStroke;
        Arrays.fill(pop, 1f);
        acceptRightClick();
        silent();
    }

    /** Hovered cell relative to the anchor, or null when the cursor is off the grid. */
    public int[] hoveredOffset() {
        if (hoverX < 0) return null;
        return new int[]{hoverX - C, hoverY - C};
    }

    @Override
    public String hint() {
        return tool.getAsInt() == TOOL_ERASER ? "Drag to erase · Ctrl+Z undo"
                : "Left paints · right erases · Ctrl+Z undo";
    }

    private int[][] targets(int x, int y) {
        int m = mirror.getAsInt();
        int mx = 2 * C - x, my = 2 * C - y;
        return switch (m) {
            case MIRROR_X -> new int[][]{{x, y}, {mx, y}};
            case MIRROR_Y -> new int[][]{{x, y}, {x, my}};
            case MIRROR_XY -> new int[][]{{x, y}, {mx, y}, {x, my}, {mx, my}};
            default -> new int[][]{{x, y}};
        };
    }

    private void apply(int x, int y) {
        if (x < 0 || y < 0 || x >= N || y >= N) return;
        List<String> rows = getter.get();
        List<String> next = rows;
        for (int[] t : targets(x, y)) {
            if (DrawnPattern.isSet(next, t[0], t[1]) != paintValue) next = DrawnPattern.with(next, t[0], t[1], paintValue);
        }
        if (next != rows) setter.accept(next);
    }

    private int cellAt(double v, int origin) {
        return (int) Math.floor((v - origin) / cell);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        int cx = cellAt(mouseX, getX());
        int cy = cellAt(mouseY, getY());
        if (button == 1 || tool.getAsInt() == TOOL_ERASER) paintValue = false;
        else paintValue = !DrawnPattern.isSet(getter.get(), cx, cy);
        beforeStroke.run();
        lastX = cx;
        lastY = cy;
        apply(cx, cy);
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double deltaX, double deltaY) {
        dragTo(mouseX, mouseY);
    }

    /** Continues the current stroke to the given mouse position (also used for right-button drags). */
    public void dragTo(double mouseX, double mouseY) {
        int cx = cellAt(mouseX, getX());
        int cy = cellAt(mouseY, getY());
        if (lastX == Integer.MIN_VALUE || (cx == lastX && cy == lastY)) return;
        int steps = Math.max(Math.abs(cx - lastX), Math.abs(cy - lastY));
        for (int i = 1; i <= steps; i++) {
            apply(lastX + Math.round((float) (cx - lastX) * i / steps), lastY + Math.round((float) (cy - lastY) * i / steps));
        }
        lastX = cx;
        lastY = cy;
    }

    private void cellFrame(DrawContext c, int cx, int cy, int col) {
        int px = getX() + cx * cell, py = getY() + cy * cell;
        Ui.frame(c, px - 1, py - 1, cell + 1, cell + 1, col);
    }

    @Override
    protected void renderWidget(DrawContext c, int mx, int my, float delta) {
        int x0 = getX(), y0 = getY(), size = getWidth();
        List<String> rows = getter.get();
        boolean inGrid = over(mx, my);
        hoverX = inGrid ? cellAt(mx, x0) : -1;
        hoverY = inGrid ? cellAt(my, y0) : -1;

        float step = Ui.step(22f);
        for (int i = 0; i < N * N; i++) {
            boolean now = DrawnPattern.isSet(rows, i % N, i / N);
            boolean was = seen != null && DrawnPattern.isSet(seen, i % N, i / N);
            if (seen != null && now != was) pop[i] = 0f;
            pop[i] = pop[i] + (1f - pop[i]) * step;
        }
        seen = rows;

        // rulers: a tick per cell, anchor tick in accent
        for (int i = 0; i < N; i++) {
            int col = i == C ? Ui.ACCENT : ((i - C) % 5 == 0 ? Ui.LINE_HI : Ui.LINE);
            int len = i == C ? 4 : 2;
            Ui.rect(c, x0 + i * cell + cell / 2, y0 - 2 - len, 1, len, col);
            Ui.rect(c, x0 - 2 - len, y0 + i * cell + cell / 2, len, 1, col);
        }

        Ui.rect(c, x0 - 1, y0 - 1, size + 1, size + 1, Ui.LINE);
        int lit = invert.get() ? 0xFFE9ECF1 : (colour.getAsInt() | 0xFF000000);
        boolean lowContrast = !invert.get() && Ui.luminance(lit) < 0.22f;
        for (int cy = 0; cy < N; cy++) {
            for (int cx = 0; cx < N; cx++) {
                int px = x0 + cx * cell, py = y0 + cy * cell;
                boolean on = DrawnPattern.isSet(rows, cx, cy);
                boolean axis = cx == C || cy == C;
                int off = ((cx + cy) & 1) == 0 ? 0xFF121418 : 0xFF0F1114;
                if (axis) off = Ui.mix(off, 0xFF1A1E25, 0.8f);
                Ui.rect(c, px, py, cell - 1, cell - 1, off);
                if (on) {
                    float p = pop[cy * N + cx];
                    int inset = Math.round((1f - p) * (cell - 1) * 0.35f);
                    Ui.rect(c, px + inset, py + inset, cell - 1 - 2 * inset, cell - 1 - 2 * inset, lit);
                    if (lowContrast) Ui.frame(c, px, py, cell - 1, cell - 1, Ui.LINE_HI);
                } else if (pop[cy * N + cx] < 0.98f) {
                    float p = pop[cy * N + cx];
                    Ui.rect(c, px, py, cell - 1, cell - 1, Ui.alpha(lit, (1f - p) * 0.6f));
                }
            }
        }
        // anchor marker (the pixel the game centres on)
        int ax = x0 + C * cell, ay = y0 + C * cell;
        if (!DrawnPattern.isSet(rows, C, C)) {
            int inner = cell - 1;
            int dot = inner % 2 == 0 ? 2 : 1;
            int off = (inner - dot) / 2;
            Ui.rect(c, ax + off, ay + off, dot, dot, Ui.ACCENT);
        }

        if (hoverX >= 0) {
            boolean erasing = tool.getAsInt() == TOOL_ERASER;
            int ghost = erasing ? Ui.ENEMY : Ui.ACCENT;
            int[][] t = targets(hoverX, hoverY);
            for (int i = 0; i < t.length; i++) {
                if (t[i][0] < 0 || t[i][1] < 0 || t[i][0] >= N || t[i][1] >= N) continue;
                cellFrame(c, t[i][0], t[i][1], i == 0 ? ghost : Ui.alpha(ghost, 0.45f));
            }
            if (!erasing && !DrawnPattern.isSet(rows, hoverX, hoverY)) {
                Ui.rect(c, x0 + hoverX * cell, y0 + hoverY * cell, cell - 1, cell - 1, Ui.alpha(lit, 0.3f));
            }
        }
    }
}
