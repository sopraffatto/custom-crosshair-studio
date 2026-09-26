package net.customcrosshairstudio.gui.ui.widgets;

import net.customcrosshairstudio.gui.ui.Ui;
import net.customcrosshairstudio.gui.ui.UiWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Supplier;

/**
 * Continuous value field in the style of creative tools: the whole row is the slider. The filled
 * portion is tinted, label on the left, value on the right; drag anywhere to scrub, right-click resets.
 */
public class Scrubber extends UiWidget {
    private final String label;
    private final int min;
    private final int max;
    private final int def;
    private final int tint;
    private final Supplier<Float> getter;
    private final Consumer<Float> setter;
    private final IntFunction<String> format;

    public Scrubber(int x, int y, int w, int h, String id, String label, int min, int max, int def, int tint,
                    IntFunction<String> format, Supplier<Float> getter, Consumer<Float> setter) {
        super(x, y, w, h, id, label);
        this.label = label;
        this.min = min;
        this.max = max;
        this.def = def;
        this.tint = tint;
        this.format = format;
        this.getter = getter;
        this.setter = setter;
        acceptRightClick();
        silent();
        this.hint = "Drag to adjust · right-click resets";
    }

    private void setFromMouse(double mx) {
        float t = MathHelper.clamp((float) ((mx - getX() - 1) / (getWidth() - 2)), 0f, 1f);
        setter.accept((float) Math.round(min + t * (max - min)));
    }

    @Override
    protected void renderWidget(DrawContext c, int mx, int my, float delta) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        float hov = hoverAnim(mx, my);
        float drag = anim("drag", isFocused() && over(mx, my) ? 1f : 0f, 20f);
        int v = Math.round(getter.get());
        float t = anim("t", (v - min) / (float) (max - min), 30f);

        Ui.rect(c, x, y, w, h, Ui.mix(Ui.RAISED, Ui.RAISED_HI, hov));
        int fillW = Math.round((w - 2) * t);
        Ui.rect(c, x + 1, y + 1, fillW, h - 2, Ui.alpha(tint, 0.16f + 0.08f * hov));
        Ui.rect(c, x + 1, y + h - 2, fillW, 1, tint);
        if (hov > 0.02f) Ui.rect(c, x + 1 + fillW, y + 2, 1, h - 4, Ui.alpha(Ui.TEXT, hov * (0.5f + 0.5f * drag)));

        String val = format.apply(v);
        int valW = Ui.width(val);
        Ui.text(c, Ui.fit(label, w - valW - 14), x + 5, y + (h - 8) / 2, Ui.mix(Ui.TEXT_2, Ui.TEXT, hov));
        Ui.textRight(c, val, x + w - 5, y + (h - 8) / 2, v != def ? Ui.TEXT : Ui.TEXT_3);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (button == 1) {
            setter.accept((float) def);
            return;
        }
        setFromMouse(mouseX);
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double deltaX, double deltaY) {
        setFromMouse(mouseX);
    }
}
