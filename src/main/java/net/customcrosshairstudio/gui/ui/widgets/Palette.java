package net.customcrosshairstudio.gui.ui.widgets;

import net.customcrosshairstudio.gui.ui.Ui;
import net.customcrosshairstudio.gui.ui.UiWidget;
import net.minecraft.client.gui.DrawContext;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** Row of quick colours. Picking one sets RGB and keeps the current opacity. */
public class Palette extends UiWidget {
    public static final int[] COLOURS = {
            0xFFFFFFFF, 0xFF00FF66, 0xFF2BE8FF, 0xFFFFE14D, 0xFFFF9A2E, 0xFFFF4B5C, 0xFFD36BFF, 0xFF000000};

    private final Supplier<Integer> getter;
    private final Consumer<Integer> setter;

    public Palette(int x, int y, int w, int h, String id, Supplier<Integer> getter, Consumer<Integer> setter) {
        super(x, y, w, h, id, "Palette");
        this.getter = getter;
        this.setter = setter;
        this.hint = "Quick colours · opacity is kept";
    }

    private int cellW() {
        return (getWidth() + 2) / COLOURS.length;
    }

    @Override
    protected void renderWidget(DrawContext c, int mx, int my, float delta) {
        int x = getX(), y = getY(), h = getHeight();
        int cw = cellW();
        int current = getter.get() & 0xFFFFFF;
        for (int i = 0; i < COLOURS.length; i++) {
            int cx = x + i * cw;
            int w = cw - 2;
            boolean hovered = over(mx, my) && mx >= cx && mx < cx + cw;
            boolean sel = (COLOURS[i] & 0xFFFFFF) == current;
            float s = anim("s" + i, sel ? 1f : 0f, 20f);
            float hv = anim("h" + i, hovered ? 1f : 0f, 20f);
            int inset = Math.round(2 * (1f - Math.max(s, hv)));
            Ui.rect(c, cx, y, w, h, Ui.WELL);
            Ui.rect(c, cx + 1 + inset, y + 1 + inset, w - 2 - 2 * inset, h - 2 - 2 * inset, COLOURS[i]);
            Ui.frame(c, cx, y, w, h, Ui.mix(Ui.mix(Ui.LINE, Ui.LINE_HI, hv), Ui.ACCENT, s));
        }
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        int i = (int) ((mouseX - getX()) / cellW());
        if (i < 0 || i >= COLOURS.length) return;
        setter.accept((getter.get() & 0xFF000000) | (COLOURS[i] & 0xFFFFFF));
    }
}
