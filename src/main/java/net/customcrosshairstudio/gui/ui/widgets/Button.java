package net.customcrosshairstudio.gui.ui.widgets;

import net.customcrosshairstudio.gui.ui.Icons;
import net.customcrosshairstudio.gui.ui.Ui;
import net.customcrosshairstudio.gui.ui.UiWidget;
import net.minecraft.client.gui.DrawContext;

import java.util.function.Supplier;

/**
 * Text and/or icon button. Kinds: PRIMARY (filled accent), GHOST (quiet until hovered), DANGER
 * (ghost that turns red and asks for a second click before acting), TOOL (square icon with a
 * selected state, used by the Draw tool rail).
 */
public class Button extends UiWidget {
    public enum Kind { PRIMARY, GHOST, DANGER, TOOL }

    private static final long CONFIRM_NANOS = 2_500_000_000L;

    private final String label;
    private final String[] icon;
    private final Kind kind;
    private final Runnable action;
    private Supplier<Boolean> selected = () -> false;
    private Supplier<Boolean> enabled = () -> true;
    private boolean mirrorIcon;
    private long armedAt;

    public Button(int x, int y, int w, int h, String id, String label, String[] icon, Kind kind, Runnable action) {
        super(x, y, w, h, id, label);
        this.label = label;
        this.icon = icon;
        this.kind = kind;
        this.action = action;
    }

    public Button selectedWhen(Supplier<Boolean> selected) {
        this.selected = selected;
        return this;
    }

    public Button mirroredIcon() {
        this.mirrorIcon = true;
        return this;
    }

    public Button enabledWhen(Supplier<Boolean> enabled) {
        this.enabled = enabled;
        return this;
    }

    private boolean armed() {
        return kind == Kind.DANGER && armedAt != 0L && System.nanoTime() - armedAt < CONFIRM_NANOS;
    }

    @Override
    protected void renderWidget(DrawContext c, int mx, int my, float delta) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        boolean usable = enabled.get();
        float hov = usable ? hoverAnim(mx, my) : anim("hover", 0f, 20f);
        float sel = anim("sel", selected.get() ? 1f : 0f, 22f);
        float arm = anim("arm", armed() ? 1f : 0f, 20f);

        int bg, fg;
        switch (kind) {
            case PRIMARY -> {
                bg = Ui.mix(Ui.ACCENT, 0xFFFFFFFF, hov * 0.25f);
                fg = Ui.ACCENT_INK;
            }
            case DANGER -> {
                bg = Ui.mix(Ui.alpha(Ui.RAISED, hov), Ui.alpha(Ui.ENEMY, 0.9f), arm);
                fg = Ui.mix(Ui.mix(Ui.TEXT_2, 0xFFFF8A95, hov), 0xFFFFFFFF, arm);
            }
            case TOOL -> {
                bg = Ui.mix(Ui.alpha(Ui.RAISED, 0.6f + 0.4f * hov), Ui.RAISED_HI, sel);
                fg = Ui.mix(Ui.mix(Ui.TEXT_2, Ui.TEXT, hov), Ui.ACCENT, sel);
            }
            default -> {
                bg = Ui.alpha(Ui.RAISED, 0.55f + 0.45f * hov);
                fg = Ui.mix(Ui.TEXT_2, Ui.TEXT, hov);
            }
        }
        if (!usable) fg = Ui.TEXT_3;
        Ui.rect(c, x, y, w, h, bg);
        if (kind != Kind.PRIMARY) Ui.frame(c, x, y, w, h, Ui.mix(Ui.LINE, Ui.LINE_HI, hov));
        if (kind == Kind.TOOL && sel > 0.01f) Ui.rect(c, x, y + h - 1, w, 1, Ui.alpha(Ui.ACCENT, sel));

        String text = armed() ? "Sure?" : label;
        boolean hasText = text != null && !text.isEmpty();
        int iconW = icon != null ? Icons.SIZE : 0;
        int gap = icon != null && hasText ? 4 : 0;
        String shown = hasText ? Ui.fit(text, w - 6 - iconW - gap) : "";
        int total = iconW + gap + Ui.width(shown);
        int cx = x + (w - total) / 2;
        if (icon != null) Icons.draw(c, icon, cx, y + (h - Icons.SIZE) / 2, fg, mirrorIcon);
        if (hasText) Ui.text(c, shown, cx + iconW + gap, y + (h - 8) / 2, fg);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (!enabled.get()) return;
        if (kind == Kind.DANGER && !armed()) {
            armedAt = System.nanoTime();
            return;
        }
        armedAt = 0L;
        action.run();
    }

    @Override
    public String hint() {
        if (armed()) return "Click again to confirm";
        return hint;
    }
}
