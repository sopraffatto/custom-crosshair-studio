package net.customcrosshairstudio.gui.ui.widgets;

import net.customcrosshairstudio.config.CrosshairProfile;
import net.customcrosshairstudio.gui.ui.Preview;
import net.customcrosshairstudio.gui.ui.Ui;
import net.customcrosshairstudio.gui.ui.UiWidget;
import net.minecraft.client.gui.DrawContext;

import java.util.function.Supplier;

/**
 * A crosshair profile in the sidebar with a live thumbnail of that profile. Selecting it makes
 * it the profile being edited. {@code active} (Enemy only) shows whether the profile is live in game.
 */
public class ProfileSlot extends UiWidget {
    private final String name;
    private final int identity;
    private final boolean compact;
    private final Supplier<CrosshairProfile> profile;
    private final Supplier<Boolean> selected;
    private final Supplier<Boolean> active;
    private final Runnable onSelect;

    public ProfileSlot(int x, int y, int w, int h, String id, String name, int identity, boolean compact,
                       Supplier<CrosshairProfile> profile, Supplier<Boolean> selected, Supplier<Boolean> active,
                       Runnable onSelect) {
        super(x, y, w, h, id, name);
        this.name = name;
        this.identity = identity;
        this.compact = compact;
        this.profile = profile;
        this.selected = selected;
        this.active = active;
        this.onSelect = onSelect;
    }

    private String status() {
        CrosshairProfile p = profile.get();
        if (active != null && !active.get()) return "Off";
        return p.style.getDisplayName();
    }

    @Override
    public String hint() {
        return name + " profile · " + status();
    }

    @Override
    protected void renderWidget(DrawContext c, int mx, int my, float delta) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        float hov = hoverAnim(mx, my);
        float sel = anim("sel", selected.get() ? 1f : 0f, 18f);
        boolean live = active == null || active.get();
        float on = anim("live", live ? 1f : 0f, 18f);

        Ui.rect(c, x, y, w, h, Ui.mix(Ui.alpha(Ui.RAISED, hov * 0.7f), Ui.RAISED, sel));
        Ui.frame(c, x, y, w, h, Ui.mix(Ui.alpha(Ui.LINE, hov), Ui.alpha(identity, 0.85f), sel));

        int box = compact ? w - 4 : h - 6;
        int bx = x + (compact ? 2 : 3), by = y + (compact ? 2 : 3);
        Preview.well(c, bx, by, box, box);
        Preview.draw(c, profile.get(), bx + box / 2, by + box / 2, 1, bx, by, box, box);
        if (!live) Ui.rect(c, bx, by, box, box, 0x99080A0D);

        if (active != null) {
            int dot = Ui.mix(Ui.TEXT_3, identity, on);
            Ui.rect(c, x + w - 5, y + 2, 3, 3, dot);
        }
        if (compact) return;

        int tx = bx + box + 6;
        int tw = x + w - 8 - tx;
        Ui.text(c, Ui.fit(name, tw), tx, y + 5, Ui.mix(Ui.TEXT_2, Ui.TEXT, Math.max(sel, hov)));
        Ui.text(c, Ui.fit(status(), tw), tx, y + 16, live ? Ui.mix(Ui.TEXT_3, identity, sel * 0.8f) : Ui.TEXT_3);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        onSelect.run();
    }
}
