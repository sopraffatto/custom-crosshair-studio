package net.customcrosshairstudio.gui.ui.widgets;

import net.customcrosshairstudio.gui.ui.Ui;
import net.customcrosshairstudio.gui.ui.UiWidget;
import net.minecraft.client.gui.DrawContext;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** Full-row boolean setting: label on the left, square sliding switch on the right. */
public class Switch extends UiWidget {
    private final String label;
    private final Supplier<Boolean> getter;
    private final Consumer<Boolean> setter;
    private final int onColour;

    public Switch(int x, int y, int w, int h, String id, String label, int onColour,
                  Supplier<Boolean> getter, Consumer<Boolean> setter) {
        super(x, y, w, h, id, label);
        this.label = label;
        this.getter = getter;
        this.setter = setter;
        this.onColour = onColour;
    }

    @Override
    protected void renderWidget(DrawContext c, int mx, int my, float delta) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        boolean on = getter.get();
        float hov = hoverAnim(mx, my);
        float t = anim("on", on ? 1f : 0f, 22f);

        Ui.rect(c, x, y, w, h, Ui.mix(Ui.alpha(Ui.RAISED, 0f), Ui.RAISED, hov));
        int tw = 18, th = 8;
        int tx = x + w - tw - 4, ty = y + (h - th) / 2;
        Ui.rect(c, tx, ty, tw, th, Ui.mix(Ui.WELL, onColour, t));
        Ui.frame(c, tx, ty, tw, th, Ui.mix(Ui.LINE_HI, onColour, t));
        int knob = 6;
        int kx = tx + 1 + Math.round((tw - 2 - knob) * t);
        Ui.rect(c, kx, ty + 1, knob, knob, Ui.mix(Ui.TEXT_3, Ui.ACCENT_INK, t));

        Ui.text(c, Ui.fit(label, tx - x - 10), x + 4, y + (h - 8) / 2, Ui.mix(Ui.TEXT_2, Ui.TEXT, Math.max(hov, t)));
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        setter.accept(!getter.get());
    }
}
