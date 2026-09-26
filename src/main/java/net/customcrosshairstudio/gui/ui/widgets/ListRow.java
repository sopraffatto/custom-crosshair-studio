package net.customcrosshairstudio.gui.ui.widgets;

import net.customcrosshairstudio.gui.ui.Icons;
import net.customcrosshairstudio.gui.ui.Ui;
import net.customcrosshairstudio.gui.ui.UiWidget;
import net.minecraft.client.gui.DrawContext;

/**
 * Clickable list row: leading icon, title, trailing detail text, and an optional delete cross
 * that only appears on hover. Used for presets, key bindings and disclosure headers.
 */
public class ListRow extends UiWidget {
    private static final int DELETE_W = 12;

    private final String[] icon;
    private final String title;
    private final String detail;
    private final Runnable onClick;
    private final Runnable onDelete;
    private boolean keycap;

    public ListRow(int x, int y, int w, int h, String id, String[] icon, String title, String detail,
                   Runnable onClick, Runnable onDelete) {
        super(x, y, w, h, id, title);
        this.icon = icon;
        this.title = title;
        this.detail = detail;
        this.onClick = onClick;
        this.onDelete = onDelete;
    }

    /** Draw the detail as a key cap (for key bindings). */
    public ListRow asKeycap() {
        this.keycap = true;
        return this;
    }

    private boolean overDelete(double mx) {
        return onDelete != null && mx >= getX() + getWidth() - DELETE_W;
    }

    @Override
    protected void renderWidget(DrawContext c, int mx, int my, float delta) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        float hov = hoverAnim(mx, my);
        float del = anim("del", over(mx, my) && overDelete(mx) ? 1f : 0f, 20f);
        Ui.rect(c, x, y, w, h, Ui.alpha(Ui.RAISED, 0.35f + 0.65f * hov));

        int tx = x + 5;
        if (icon != null) {
            Icons.draw(c, icon, x + 5, y + (h - Icons.SIZE) / 2, Ui.mix(Ui.TEXT_3, Ui.ACCENT, hov));
            tx += Icons.SIZE + 5;
        }
        int right = x + w - 5 - (onDelete != null ? DELETE_W : 0);
        int detailW = 0;
        if (detail != null && !detail.isEmpty()) {
            String d = Ui.fit(detail, Math.max(20, (right - tx) / 2));
            detailW = Ui.width(d);
            if (keycap) {
                int kw = detailW + 8;
                int kx = right - kw;
                Ui.rect(c, kx, y + 2, kw, h - 4, Ui.WELL);
                Ui.frame(c, kx, y + 2, kw, h - 4, Ui.mix(Ui.LINE_HI, Ui.TEXT_3, hov));
                Ui.rect(c, kx + 1, y + h - 3, kw - 2, 1, Ui.LINE_HI);
                Ui.text(c, d, kx + 4, y + (h - 8) / 2, Ui.TEXT);
                detailW = kw;
            } else {
                Ui.textRight(c, d, right, y + (h - 8) / 2, Ui.TEXT_3);
            }
        }
        Ui.text(c, Ui.fit(title, right - tx - detailW - 6), tx, y + (h - 8) / 2, Ui.mix(Ui.TEXT_2, Ui.TEXT, hov));

        if (onDelete != null && hov > 0.02f) {
            int col = Ui.alpha(Ui.mix(Ui.TEXT_3, Ui.ENEMY, del), hov);
            Icons.draw(c, Icons.CLOSE, x + w - DELETE_W + 2, y + (h - Icons.SIZE) / 2, col);
        }
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (overDelete(mouseX)) onDelete.run();
        else if (onClick != null) onClick.run();
    }
}
