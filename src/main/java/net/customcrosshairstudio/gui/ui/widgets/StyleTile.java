package net.customcrosshairstudio.gui.ui.widgets;

import net.customcrosshairstudio.config.CrosshairStudioConfig.StyleType;
import net.customcrosshairstudio.config.CrosshairProfile;
import net.customcrosshairstudio.config.DrawnPattern;
import net.customcrosshairstudio.gui.ui.Icons;
import net.customcrosshairstudio.gui.ui.Preview;
import net.customcrosshairstudio.gui.ui.Ui;
import net.customcrosshairstudio.gui.ui.UiWidget;
import net.minecraft.client.gui.DrawContext;

import java.util.function.Supplier;

/**
 * Style gallery tile: a live, magnified thumbnail of the edited profile rendered in this style
 * (so the user sees their own crosshair in each style), with the style name underneath.
 */
public class StyleTile extends UiWidget {
    private final StyleType type;
    private final Supplier<CrosshairProfile> base;
    private final Supplier<Boolean> selected;
    private final Runnable onSelect;
    private final CrosshairProfile scratch = new CrosshairProfile();

    public StyleTile(int x, int y, int w, int h, String id, StyleType type, Supplier<CrosshairProfile> base,
                     Supplier<Boolean> selected, Runnable onSelect) {
        super(x, y, w, h, id, type.getDisplayName());
        this.type = type;
        this.base = base;
        this.selected = selected;
        this.onSelect = onSelect;
        this.hint = switch (type) {
            case CLASSIC -> "Four arms with optional gap and centre dot";
            case DOT -> "A single centre dot";
            case INVERTED -> "Classic shape that inverts the background";
            case DRAWN -> "Your own pixel design · opens the canvas";
        };
    }

    @Override
    protected void renderWidget(DrawContext c, int mx, int my, float delta) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        float hov = hoverAnim(mx, my);
        float sel = anim("sel", selected.get() ? 1f : 0f, 18f);

        Ui.rect(c, x, y, w, h, Ui.mix(Ui.RAISED, Ui.RAISED_HI, Math.max(hov, sel * 0.6f)));
        Ui.frame(c, x, y, w, h, Ui.mix(Ui.mix(Ui.LINE, Ui.LINE_HI, hov), Ui.ACCENT, sel));

        int labelH = 12;
        int tx = x + 2, ty = y + 2, tw = w - 4, th = h - labelH - 3;
        Preview.well(c, tx, ty, tw, th);
        scratch.copyProfileFrom(base.get());
        scratch.style = type;
        if (type == StyleType.DRAWN && DrawnPattern.isEmpty(scratch.drawn)) scratch.drawn = DrawnPattern.defaultRows();
        Preview.draw(c, scratch, tx + tw / 2, ty + th / 2, 3, tx, ty, tw, th);

        int fg = Ui.mix(Ui.mix(Ui.TEXT_2, Ui.TEXT, hov), Ui.TEXT, sel);
        Icons.draw(c, Icons.forStyle(type), x + 4, y + h - labelH + 1, Ui.mix(Ui.TEXT_3, Ui.ACCENT, sel));
        Ui.text(c, Ui.fit(type.getDisplayName(), w - 16), x + 14, y + h - labelH + 1, fg);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        onSelect.run();
    }
}
