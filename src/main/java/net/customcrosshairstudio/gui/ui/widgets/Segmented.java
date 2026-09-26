package net.customcrosshairstudio.gui.ui.widgets;

import net.customcrosshairstudio.gui.ui.Ui;
import net.customcrosshairstudio.gui.ui.UiWidget;
import net.minecraft.client.gui.DrawContext;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Segmented selector with a sliding indicator. Segments are either text labels or colour chips
 * (when {@code swatches} is non-null), which is how the canvas backdrop picker is drawn.
 */
public class Segmented extends UiWidget {
    private final String[] labels;
    private final int[] swatches;
    private final Supplier<Integer> selected;
    private final Consumer<Integer> onSelect;

    public Segmented(int x, int y, int w, int h, String id, String[] labels, int[] swatches,
                     Supplier<Integer> selected, Consumer<Integer> onSelect) {
        super(x, y, w, h, id, String.join(" ", labels));
        this.labels = labels;
        this.swatches = swatches;
        this.selected = selected;
        this.onSelect = onSelect;
    }

    private int count() {
        return labels.length;
    }

    private int segX(int i) {
        return getX() + 1 + i * (getWidth() - 2) / count();
    }

    @Override
    protected void renderWidget(DrawContext c, int mx, int my, float delta) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        int sel = selected.get();
        float pos = anim("pos", sel, 22f);

        Ui.rect(c, x, y, w, h, Ui.alpha(Ui.WELL, 0.92f));
        Ui.frame(c, x, y, w, h, Ui.LINE);
        int segW = (w - 2) / count();
        int ix = x + 1 + Math.round(pos * (w - 2) / count());
        Ui.rect(c, ix, y + 1, segW, h - 2, Ui.RAISED_HI);
        Ui.rect(c, ix, y + h - 2, segW, 1, Ui.ACCENT);

        for (int i = 0; i < count(); i++) {
            int sx = segX(i);
            int sw = segX(i + 1) - sx;
            boolean hovered = my >= y && my < y + h && mx >= sx && mx < sx + sw;
            float near = Ui.clamp01(1f - Math.abs(pos - i));
            if (swatches != null) {
                int s = Math.min(sw - 4, h - 6);
                int cx = sx + (sw - s) / 2, cy = y + (h - 1 - s) / 2;
                Ui.rect(c, cx, cy, s, s, swatches[i]);
                Ui.frame(c, cx, cy, s, s, Ui.mix(hovered ? Ui.TEXT_2 : Ui.LINE_HI, Ui.TEXT, near));
            } else {
                int colour = Ui.mix(hovered ? Ui.TEXT_2 : Ui.TEXT_3, Ui.TEXT, near);
                Ui.textCentered(c, Ui.fit(labels[i], sw - 2), sx + sw / 2, y + (h - 8) / 2, colour);
            }
        }
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        for (int i = 0; i < count(); i++) {
            if (mouseX >= segX(i) && mouseX < segX(i + 1)) {
                if (i != selected.get()) onSelect.accept(i);
                return;
            }
        }
    }
}
