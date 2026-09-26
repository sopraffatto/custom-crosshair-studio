package net.customcrosshairstudio.gui.ui;

import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.text.Text;

/**
 * Base for every Custom Crosshair Studio control: keyed motion ({@link #anim}), an optional status-bar hint,
 * optional right-click handling ({@link #button} holds the button of the current click) and
 * an opt-out of the vanilla click sound for continuous controls.
 */
public abstract class UiWidget extends ClickableWidget {
    protected final String id;
    protected String hint;
    protected int button;
    private boolean rightClick;
    private boolean silent;

    protected UiWidget(int x, int y, int w, int h, String id, String label) {
        super(x, y, w, h, Text.literal(label == null ? "" : label));
        this.id = id;
    }

    /** Status-bar text shown while hovered. */
    public String hint() {
        return hint;
    }

    public UiWidget withHint(String hint) {
        this.hint = hint;
        return this;
    }

    protected UiWidget acceptRightClick() {
        this.rightClick = true;
        return this;
    }

    protected UiWidget silent() {
        this.silent = true;
        return this;
    }

    protected float anim(String channel, float target, float speed) {
        return Ui.motion(id + "#" + channel, target, speed);
    }

    protected boolean over(double mx, double my) {
        return mx >= getX() && my >= getY() && mx < getX() + getWidth() && my < getY() + getHeight();
    }

    protected float hoverAnim(int mx, int my) {
        return anim("hover", over(mx, my) ? 1f : 0f, 20f);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.button = button;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected boolean isValidClickButton(int button) {
        return button == 0 || (rightClick && button == 1);
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
        if (!silent) super.playDownSound(soundManager);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
}
