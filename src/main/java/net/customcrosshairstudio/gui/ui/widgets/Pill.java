package net.customcrosshairstudio.gui.ui.widgets;

import net.customcrosshairstudio.gui.ui.Ui;
import net.customcrosshairstudio.gui.ui.UiWidget;
import net.minecraft.client.gui.DrawContext;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** Compact status toggle: a status light and a short word ("Live" / "Off"). */
public class Pill extends UiWidget {
    private final String onText;
    private final String offText;
    private final int colour;
    private final Supplier<Boolean> getter;
    private final Consumer<Boolean> setter;

    public Pill(int x, int y, int w, int h, String id, String onText, String offText, int colour,
                Supplier<Boolean> getter, Consumer<Boolean> setter) {
        super(x, y, w, h, id, onText);
        this.onText = onText;
        this.offText = offText;
        this.colour = colour;
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    protected void renderWidget(DrawContext c, int mx, int my, float delta) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        boolean on = getter.get();
        float hov = hoverAnim(mx, my);
        float t = anim("on", on ? 1f : 0f, 20f);
        Ui.rect(c, x, y, w, h, Ui.alpha(Ui.mix(Ui.WELL, Ui.RAISED_HI, hov), 0.92f));
        Ui.frame(c, x, y, w, h, Ui.mix(Ui.mix(Ui.LINE, Ui.LINE_HI, hov), Ui.alpha(colour, 0.8f), t));
        int light = Ui.mix(Ui.TEXT_3, colour, t);
        Ui.rect(c, x + 5, y + (h - 3) / 2, 3, 3, light);
        String s = on ? onText : offText;
        Ui.text(c, Ui.fit(s, w - 16), x + 12, y + (h - 8) / 2, Ui.mix(Ui.TEXT_2, Ui.TEXT, Math.max(t, hov)));
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        setter.accept(!getter.get());
    }
}
