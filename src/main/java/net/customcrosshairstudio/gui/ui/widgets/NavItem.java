package net.customcrosshairstudio.gui.ui.widgets;

import net.customcrosshairstudio.gui.ui.Icons;
import net.customcrosshairstudio.gui.ui.Ui;
import net.customcrosshairstudio.gui.ui.UiWidget;
import net.minecraft.client.gui.DrawContext;

import java.util.function.Supplier;

/**
 * Sidebar page entry. Icon + label in the wide sidebar, icon only on the rail (the label then
 * appears in the status bar). The selection highlight itself is drawn by the screen so it can
 * slide between items.
 */
public class NavItem extends UiWidget {
    private final String label;
    private final String[] icon;
    private final boolean compact;
    private final Supplier<Boolean> selected;
    private final Runnable onSelect;

    public NavItem(int x, int y, int w, int h, String id, String label, String[] icon, boolean compact,
                   Supplier<Boolean> selected, Runnable onSelect) {
        super(x, y, w, h, id, label);
        this.label = label;
        this.icon = icon;
        this.compact = compact;
        this.selected = selected;
        this.onSelect = onSelect;
        this.hint = label;
    }

    @Override
    protected void renderWidget(DrawContext c, int mx, int my, float delta) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        float hov = hoverAnim(mx, my);
        float sel = anim("sel", selected.get() ? 1f : 0f, 20f);
        if (hov > 0.01f && sel < 0.99f) Ui.rect(c, x, y, w, h, Ui.alpha(Ui.RAISED, hov * (1f - sel)));
        int fg = Ui.mix(Ui.mix(Ui.TEXT_3, Ui.TEXT_2, hov), Ui.TEXT, sel);
        int iconColour = Ui.mix(fg, Ui.ACCENT, sel);
        if (compact) {
            Icons.draw(c, icon, x + (w - Icons.SIZE) / 2, y + (h - Icons.SIZE) / 2, iconColour);
        } else {
            Icons.draw(c, icon, x + 7, y + (h - Icons.SIZE) / 2, iconColour);
            Ui.text(c, Ui.fit(label, w - 24), x + 19, y + (h - 8) / 2, fg);
        }
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        onSelect.run();
    }
}
