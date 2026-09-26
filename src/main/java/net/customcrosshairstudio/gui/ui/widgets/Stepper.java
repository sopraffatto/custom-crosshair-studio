package net.customcrosshairstudio.gui.ui.widgets;

import net.customcrosshairstudio.gui.ui.Ui;
import net.customcrosshairstudio.gui.ui.UiWidget;
import net.minecraft.client.gui.DrawContext;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Discrete integer setting shown as a meter of pips (one pip per step). Click or drag across
 * the pips to set the value; right-click resets to the default. Used for every small-range
 * geometry value (thickness, length, gap, dot size, outline width).
 */
public class Stepper extends UiWidget {
    private static final int VALUE_W = 20;

    private final String label;
    private final int min;
    private final int max;
    private final int def;
    private final String unit;
    private final Supplier<Float> getter;
    private final Consumer<Float> setter;

    public Stepper(int x, int y, int w, int h, String id, String label, int min, int max, int def, String unit,
                   Supplier<Float> getter, Consumer<Float> setter) {
        super(x, y, w, h, id, label);
        this.label = label;
        this.min = min;
        this.max = max;
        this.def = def;
        this.unit = unit;
        this.getter = getter;
        this.setter = setter;
        acceptRightClick();
        silent();
        this.hint = "Click or drag to set · right-click resets";
    }

    private int pipCount() {
        return max - Math.max(1, min) + 1;
    }

    private int pipW() {
        int avail = getWidth() / 2 - VALUE_W;
        return Math.max(5, Math.min(12, (avail - (pipCount() - 1)) / pipCount()));
    }

    private int barW() {
        return pipCount() * pipW() + (pipCount() - 1);
    }

    private int barX() {
        return getX() + getWidth() - 4 - VALUE_W - barW();
    }

    private int value() {
        return Math.round(getter.get());
    }

    private void setFromMouse(double mx, boolean fromClick) {
        int first = Math.max(1, min);
        int idx = (int) Math.floor((mx - barX()) / (double) (pipW() + 1));
        int v;
        if (idx < 0) v = min;
        else v = Math.min(max, first + idx);
        if (fromClick && min == 0 && v == first && value() == first) v = 0;
        if (v != value()) setter.accept((float) v);
    }

    @Override
    protected void renderWidget(DrawContext c, int mx, int my, float delta) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        float hov = hoverAnim(mx, my);
        int v = value();
        float level = anim("level", v, 26f);

        Ui.rect(c, x, y, w, h, Ui.mix(Ui.alpha(Ui.RAISED, 0f), Ui.RAISED, hov));
        int bx = barX();
        Ui.text(c, Ui.fit(label, bx - x - 10), x + 4, y + (h - 8) / 2, Ui.mix(Ui.TEXT_2, Ui.TEXT, hov));

        int first = Math.max(1, min);
        int pw = pipW();
        int ph = Math.min(8, h - 6);
        int py = y + (h - ph) / 2;
        for (int i = 0; i < pipCount(); i++) {
            int pipValue = first + i;
            float fill = Ui.clamp01(level - pipValue + 1f);
            int px = bx + i * (pw + 1);
            Ui.rect(c, px, py, pw, ph, Ui.WELL);
            Ui.frame(c, px, py, pw, ph, Ui.mix(Ui.LINE, Ui.LINE_HI, hov));
            if (fill > 0.01f) {
                int fh = Math.max(1, Math.round((ph - 2) * fill));
                Ui.rect(c, px + 1, py + ph - 1 - fh, pw - 2, fh, Ui.ACCENT);
            }
        }
        String val = v + unit;
        boolean changed = v != def;
        Ui.textRight(c, val, x + w - 4, y + (h - 8) / 2, changed ? Ui.TEXT : Ui.TEXT_3);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (button == 1) {
            setter.accept((float) def);
            return;
        }
        setFromMouse(mouseX, true);
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double deltaX, double deltaY) {
        setFromMouse(mouseX, false);
    }
}
