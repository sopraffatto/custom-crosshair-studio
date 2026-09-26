package net.customcrosshairstudio.gui;

import net.customcrosshairstudio.CrosshairStudioClient;
import net.customcrosshairstudio.config.ConfigManager;
import net.customcrosshairstudio.config.CrosshairStudioConfig;
import net.customcrosshairstudio.config.CrosshairStudioConfig.StyleType;
import net.customcrosshairstudio.config.CrosshairProfile;
import net.customcrosshairstudio.config.DrawnPattern;
import net.customcrosshairstudio.gui.ui.AppLayout;
import net.customcrosshairstudio.gui.ui.AppLayout.Rect;
import net.customcrosshairstudio.gui.ui.Icons;
import net.customcrosshairstudio.gui.ui.Preview;
import net.customcrosshairstudio.gui.ui.Ui;
import net.customcrosshairstudio.gui.ui.UiWidget;
import net.customcrosshairstudio.gui.ui.widgets.Button;
import net.customcrosshairstudio.gui.ui.widgets.ListRow;
import net.customcrosshairstudio.gui.ui.widgets.NavItem;
import net.customcrosshairstudio.gui.ui.widgets.Palette;
import net.customcrosshairstudio.gui.ui.widgets.Pill;
import net.customcrosshairstudio.gui.ui.widgets.PixelCanvas;
import net.customcrosshairstudio.gui.ui.widgets.ProfileSlot;
import net.customcrosshairstudio.gui.ui.widgets.Scrubber;
import net.customcrosshairstudio.gui.ui.widgets.Segmented;
import net.customcrosshairstudio.gui.ui.widgets.Stepper;
import net.customcrosshairstudio.gui.ui.widgets.StyleTile;
import net.customcrosshairstudio.gui.ui.widgets.Switch;
import net.customcrosshairstudio.keybind.KeybindManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.KeybindsScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * Custom Crosshair Studio settings app.
 *
 * <p>Model: pick a <b>profile</b> (Primary or Enemy) in the sidebar, pick a <b>page</b> (Style,
 * Shape/Draw, Color, Presets, Behavior), and edit it in the inspector while the <b>canvas</b> shows
 * the result through the real renderer. When the style is Drawn, the Shape page turns the canvas
 * into a pixel editor with its own tool rail. Geometry comes from {@link AppLayout}.
 */
public class CrosshairStudioScreen extends Screen {
    private enum Page {
        STYLE("Style"), SHAPE("Shape"), COLOR("Color"), PRESETS("Presets"), BEHAVIOR("Behavior");

        final String title;

        Page(String title) {
            this.title = title;
        }
    }

    private interface Build {
        void build(Rect r, List<UiWidget> out);
    }

    private interface Paint {
        void paint(DrawContext c, Rect r);
    }

    private record Item(int h, Build build, Paint paint) {}

    private record Placed(Rect r, Paint paint) {}

    private static final int ITEM_GAP = 2;
    private static final int[] BACKDROPS = {0xFF0A0B0F, 0xFF8FB4EE, 0xFF7D7F84, 0xFFE9E6DF};
    private static final String[] BACKDROP_NAMES = {"Void", "Sky", "Stone", "Paper"};
    private static final int[] ZOOMS = {1, 2, 4};
    private static final int UNDO_LIMIT = 64;
    private static final float TRANSITION_SECONDS = 0.2f;

    // Remembered while the game runs
    private static Page page = Page.STYLE;
    private static int backdrop = 0;
    private static int zoomIndex = 1;
    private static int tool = PixelCanvas.TOOL_PEN;
    private static int mirror = PixelCanvas.MIRROR_OFF;
    private static boolean customRgb = false;
    private static boolean customOutlineRgb = false;

    private final Screen parent;
    private final CrosshairStudioConfig config;
    private final String version;

    private boolean editingEnemy = false;
    private AppLayout layout;
    private final List<UiWidget> inspectorWidgets = new ArrayList<>();
    private final List<Item> items = new ArrayList<>();
    private final List<Placed> placed = new ArrayList<>();
    private int scrollIndex = 0;
    private int maxScroll = 0;
    private int contentHeight = 0;
    private int scrollTop = 0;
    private boolean hiddenBelow = false;

    private PixelCanvas pixelCanvas;
    private PixelCanvas rightDrag;
    private final Deque<List<String>> undo = new ArrayDeque<>();
    private final Deque<List<String>> redo = new ArrayDeque<>();

    private long transitionAt = 0L;
    private long pulseAt = 0L;
    private int lastSignature = 0;

    public CrosshairStudioScreen(Screen parent) {
        super(Text.literal("Custom Crosshair Studio"));
        this.parent = parent;
        this.config = CrosshairStudioClient.getConfig();
        this.version = FabricLoader.getInstance().getModContainer(CrosshairStudioClient.MOD_ID)
                .map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("");
        Ui.resetMotion();
    }

    // ================================================================== state helpers

    private CrosshairProfile profile() {
        return editingEnemy ? config.enemy : config;
    }

    private String profileName() {
        return editingEnemy ? "Enemy" : "Primary";
    }

    private boolean drawMode() {
        return page == Page.SHAPE && profile().style == StyleType.DRAWN;
    }

    private String pageTitle(Page pg) {
        return pg == Page.SHAPE && profile().style == StyleType.DRAWN ? "Draw" : pg.title;
    }

    private static boolean usesColour(CrosshairProfile p) {
        return p.style != StyleType.INVERTED && !(p.style == StyleType.DRAWN && p.drawnInvert);
    }

    /** User action that changes what is shown: run it, rebuild and play the content transition. */
    private void change(Runnable action) {
        action.run();
        transitionAt = System.nanoTime();
        init();
    }

    private void goTo(Page next) {
        if (page == next) return;
        change(() -> {
            page = next;
            scrollIndex = 0;
        });
    }

    private void selectProfile(boolean enemy) {
        if (editingEnemy == enemy) return;
        change(() -> {
            editingEnemy = enemy;
            scrollIndex = 0;
            undo.clear();
            redo.clear();
        });
    }

    private void selectStyle(StyleType st) {
        CrosshairProfile p = profile();
        if (p.style == st) return;
        change(() -> {
            p.style = st;
            if (st == StyleType.DRAWN) page = Page.SHAPE;
            scrollIndex = 0;
        });
    }

    // ================================================================== drawn history

    private void snapshot() {
        undo.push(new ArrayList<>(DrawnPattern.normalize(profile().drawn)));
        while (undo.size() > UNDO_LIMIT) undo.removeLast();
        redo.clear();
    }

    private void undo() {
        if (undo.isEmpty()) return;
        redo.push(new ArrayList<>(DrawnPattern.normalize(profile().drawn)));
        profile().drawn = undo.pop();
    }

    private void redo() {
        if (redo.isEmpty()) return;
        undo.push(new ArrayList<>(DrawnPattern.normalize(profile().drawn)));
        profile().drawn = redo.pop();
    }

    private void setPattern(List<String> rows) {
        snapshot();
        profile().drawn = rows;
    }

    private static int litCount(List<String> rows) {
        int n = 0;
        for (int y = 0; y < DrawnPattern.SIZE; y++) {
            for (int x = 0; x < DrawnPattern.SIZE; x++) if (DrawnPattern.isSet(rows, x, y)) n++;
        }
        return n;
    }

    // ================================================================== build

    @Override
    protected void init() {
        clearChildren();
        inspectorWidgets.clear();
        items.clear();
        placed.clear();
        pixelCanvas = null;
        rightDrag = null;
        layout = AppLayout.compute(width, height, drawMode());
        final AppLayout l = layout;

        // title bar
        addDrawableChild(new Button(l.close.x(), l.close.y(), l.close.w(), l.close.h(), "close", "", Icons.CLOSE,
                Button.Kind.GHOST, this::close).withHint("Close · settings are saved"));
        addDrawableChild(new Pill(l.masterPill.x(), l.masterPill.y(), l.masterPill.w(), l.masterPill.h(), "master",
                "Enabled", "Disabled", Ui.ACCENT, () -> config.enabled, v -> config.enabled = v)
                .withHint("Show or hide the custom crosshair in game"));

        buildSidebar(l);
        if (drawMode()) buildDrawCanvas(l);
        else buildPreviewCanvas(l);
        if (editingEnemy) {
            Rect r = l.enemyPill();
            addDrawableChild(new Pill(r.x(), r.y(), r.w(), r.h(), "enemy-live", "Live", "Off", Ui.ENEMY,
                    () -> config.enemyEnabled, v -> config.enemyEnabled = v)
                    .withHint("Enemy replaces Primary while you aim at players and hostile mobs"));
        }

        // status bar
        addDrawableChild(new Button(l.reset.x(), l.reset.y(), l.reset.w(), l.reset.h(), "reset", "Reset profile", null,
                Button.Kind.DANGER, () -> change(this::resetProfile))
                .withHint("Restore " + profileName() + " to its defaults"));
        addDrawableChild(new Button(l.done.x(), l.done.y(), l.done.w(), l.done.h(), "done", "Done", null,
                Button.Kind.PRIMARY, this::close).withHint("Save and close · Esc"));

        switch (page) {
            case STYLE -> pageStyle();
            case SHAPE -> {
                if (profile().style == StyleType.DRAWN) pageDraw();
                else pageShape();
            }
            case COLOR -> pageColor();
            case PRESETS -> pagePresets();
            case BEHAVIOR -> pageBehavior();
        }
        placeInspector();
    }

    private void resetProfile() {
        if (editingEnemy) config.enemy.copyProfileFrom(CrosshairProfile.enemyDefault());
        else config.resetProfile();
        undo.clear();
        redo.clear();
    }

    private void buildSidebar(AppLayout l) {
        boolean compact = !l.wideSidebar;
        Rect a = l.profileSlot(0);
        addDrawableChild(new ProfileSlot(a.x(), a.y(), a.w(), a.h(), "slot:primary", "Primary", Ui.ACCENT, compact,
                () -> config, () -> !editingEnemy, null, () -> selectProfile(false)));
        Rect b = l.profileSlot(1);
        addDrawableChild(new ProfileSlot(b.x(), b.y(), b.w(), b.h(), "slot:enemy", "Enemy", Ui.ENEMY, compact,
                () -> config.enemy, () -> editingEnemy, () -> config.enemyEnabled, () -> selectProfile(true)));

        Page[] pages = Page.values();
        for (int i = 0; i < pages.length; i++) {
            final Page pg = pages[i];
            Rect r = l.navItem(i);
            String[] icon = switch (pg) {
                case STYLE -> Icons.STYLE;
                case SHAPE -> profile().style == StyleType.DRAWN ? Icons.PEN : Icons.SHAPE;
                case COLOR -> Icons.COLOR;
                case PRESETS -> Icons.PRESETS;
                case BEHAVIOR -> Icons.BEHAVIOR;
            };
            addDrawableChild(new NavItem(r.x(), r.y(), r.w(), r.h(), "nav:" + pg.name(), pageTitle(pg), icon, compact,
                    () -> page == pg, () -> goTo(pg)));
        }
    }

    private void buildPreviewCanvas(AppLayout l) {
        Rect bp = l.backdropPicker();
        addDrawableChild(new Segmented(bp.x(), bp.y(), bp.w(), bp.h(), "backdrop", BACKDROP_NAMES, BACKDROPS,
                () -> backdrop, v -> backdrop = v).withHint("Preview backdrop"));
        Rect zp = l.zoomPicker();
        addDrawableChild(new Segmented(zp.x(), zp.y(), zp.w(), zp.h(), "zoom", new String[]{"1×", "2×", "4×"}, null,
                () -> zoomIndex, v -> zoomIndex = v).withHint("Zoom · 1× is the exact in-game size · scroll on the canvas"));
    }

    private void buildDrawCanvas(AppLayout l) {
        final CrosshairProfile p = profile();
        Rect g = l.grid(DrawnPattern.SIZE);
        pixelCanvas = new PixelCanvas(g.x(), g.y(), l.gridCell(DrawnPattern.SIZE), "canvas",
                () -> p.drawn, v -> p.drawn = v, () -> tool, () -> mirror, () -> p.color, () -> p.drawnInvert,
                this::snapshot);
        addDrawableChild(pixelCanvas);

        Rect r0 = l.railButton(0), r1 = l.railButton(1), r2 = l.railButton(2), r3 = l.railButton(3), r4 = l.railButton(4);
        addDrawableChild(new Button(r0.x(), r0.y(), r0.w(), r0.h(), "tool:pen", "", Icons.PEN, Button.Kind.TOOL,
                () -> tool = PixelCanvas.TOOL_PEN).selectedWhen(() -> tool == PixelCanvas.TOOL_PEN).withHint("Pen · P"));
        addDrawableChild(new Button(r1.x(), r1.y(), r1.w(), r1.h(), "tool:eraser", "", Icons.ERASER, Button.Kind.TOOL,
                () -> tool = PixelCanvas.TOOL_ERASER).selectedWhen(() -> tool == PixelCanvas.TOOL_ERASER).withHint("Eraser · E"));
        addDrawableChild(new Button(r2.x(), r2.y(), r2.w(), r2.h(), "tool:undo", "", Icons.UNDO, Button.Kind.TOOL,
                this::undo).enabledWhen(() -> !undo.isEmpty()).withHint("Undo · Ctrl+Z"));
        addDrawableChild(new Button(r3.x(), r3.y(), r3.w(), r3.h(), "tool:redo", "", Icons.UNDO, Button.Kind.TOOL,
                this::redo).mirroredIcon().enabledWhen(() -> !redo.isEmpty()).withHint("Redo · Ctrl+Y"));
        addDrawableChild(new Button(r4.x(), r4.y(), r4.w(), r4.h(), "tool:clear", "", Icons.TRASH, Button.Kind.DANGER,
                () -> setPattern(DrawnPattern.empty())).withHint("Clear canvas"));
    }

    // ================================================================== inspector pages

    private void section(String title) {
        items.add(new Item(14, null, (c, r) -> {
            Ui.text(c, title, r.x(), r.y() + 4, Ui.TEXT_3);
            int lx = r.x() + Ui.width(title) + 5;
            Ui.rect(c, lx, r.y() + 8, r.right() - lx, 1, Ui.LINE);
        }));
    }

    private void row(int h, Build build) {
        items.add(new Item(h, build, null));
    }

    private void note(String text) {
        List<OrderedText> lines = Ui.font().wrapLines(Text.literal(text), Math.max(20, layout.inspectorView.w() - 2));
        int shown = Math.min(lines.size(), 3);
        items.add(new Item(shown * 10, null, (c, r) -> {
            for (int i = 0; i < shown; i++) c.drawText(Ui.font(), lines.get(i), r.x() + 1, r.y() + i * 10, Ui.TEXT_3, false);
        }));
    }

    private void emptyState(String[] icon, String title, String body) {
        items.add(new Item(22, null, (c, r) -> {
            Ui.rect(c, r.x(), r.y(), 18, 18, Ui.RAISED);
            Icons.draw(c, icon, r.x() + 6, r.y() + 6, Ui.TEXT_3);
            Ui.bold(c, Ui.fit(title, r.w() - 26), r.x() + 24, r.y() + 1, Ui.TEXT_2);
            Ui.text(c, Ui.fit(body, r.w() - 26), r.x() + 24, r.y() + 11, Ui.TEXT_3);
        }));
    }

    private void pageStyle() {
        final CrosshairProfile p = profile();
        Rect v = layout.inspectorView;
        int cols = v.w() >= 236 ? 4 : 2;
        int tileW = (v.w() - (cols - 1) * 3) / cols;
        int tileH = Math.max(34, Math.min(48, tileW * 3 / 4));
        StyleType[] types = StyleType.values();
        section("Style");
        for (int start = 0; start < types.length; start += cols) {
            final int from = start;
            row(tileH, (r, out) -> {
                for (int k = 0; k < cols && from + k < types.length; k++) {
                    StyleType st = types[from + k];
                    out.add(new StyleTile(r.x() + k * (tileW + 3), r.y(), tileW, r.h(), "tile:" + st.name(), st,
                            this::profile, () -> profile().style == st, () -> selectStyle(st)));
                }
            });
        }
        if (p.style == StyleType.DRAWN) {
            row(16, (r, out) -> out.add(new Button(r.x(), r.y(), r.w(), r.h(), "open-canvas", "Open canvas", Icons.PEN,
                    Button.Kind.GHOST, () -> goTo(Page.SHAPE))));
        } else {
            note(switch (p.style) {
                case CLASSIC -> "Four arms. Tune thickness, length and gap in Shape.";
                case DOT -> "A single dot. Set its size in Shape.";
                case INVERTED -> "Inverts whatever is behind it, so it reads on any background.";
                case DRAWN -> "";
            });
        }
    }

    private Stepper stepper(Rect r, String id, String label, int min, int max, int def, String unit,
                            java.util.function.Supplier<Float> get, java.util.function.Consumer<Float> set) {
        return new Stepper(r.x(), r.y(), r.w(), r.h(), id, label, min, max, def, unit, get, set);
    }

    private void pageShape() {
        final CrosshairProfile p = profile();
        if (p.style == StyleType.DOT) {
            section("Dot");
            row(16, (r, out) -> out.add(stepper(r, "dotsize", "Size", 1, 4, 2, "", () -> p.dotSize, x -> p.dotSize = x)));
            return;
        }
        section("Arms");
        row(16, (r, out) -> out.add(stepper(r, "thickness", "Thickness", 1, 4, 1, "", () -> p.thickness, x -> p.thickness = x)));
        row(16, (r, out) -> out.add(stepper(r, "length", "Length", 1, 6, 1, "", () -> p.length, x -> p.length = x)));
        row(16, (r, out) -> out.add(stepper(r, "gap", "Gap", 0, 4, 0, "px", () -> p.gap, x -> p.gap = x)));
        section("Center");
        row(16, (r, out) -> out.add(new Switch(r.x(), r.y(), r.w(), r.h(), "dot", "Center dot", Ui.ACCENT,
                () -> p.dot, x -> change(() -> p.dot = x))));
        if (p.dot) {
            row(16, (r, out) -> out.add(stepper(r, "dotsize", "Dot size", 1, 4, 2, "", () -> p.dotSize, x -> p.dotSize = x)));
        }
    }

    private void pageDraw() {
        final CrosshairProfile p = profile();
        section("Actual size");
        items.add(new Item(34, null, (c, r) -> {
            int half = (r.w() - 3) / 2;
            Preview.well(c, r.x(), r.y(), half, r.h());
            Preview.draw(c, p, r.x() + half / 2, r.y() + r.h() / 2, 1, r.x(), r.y(), half, r.h());
            int lx = r.x() + half + 3;
            int lw = r.w() - half - 3;
            Ui.rect(c, lx, r.y(), lw, r.h(), BACKDROPS[3]);
            Preview.draw(c, p, lx + lw / 2, r.y() + r.h() / 2, 1, lx, r.y(), lw, r.h());
        }));
        section("Symmetry");
        row(14, (r, out) -> out.add(new Segmented(r.x(), r.y(), r.w(), r.h(), "mirror", new String[]{"Off", "X", "Y", "XY"}, null,
                () -> mirror, x -> mirror = x).withHint("Mirror every stroke across the centre")));
        section("Pixels");
        row(16, (r, out) -> out.add(new Switch(r.x(), r.y(), r.w(), r.h(), "invert", "Invert background", Ui.ACCENT,
                () -> p.drawnInvert, x -> change(() -> p.drawnInvert = x))
                .withHint("Pixels invert what is behind them; colour and outline are ignored")));
        row(16, (r, out) -> out.add(new Button(r.x(), r.y(), r.w(), r.h(), "load-default", "Load starter plus", Icons.MARK,
                Button.Kind.GHOST, () -> setPattern(DrawnPattern.defaultRows())).withHint("Replace the canvas with a small plus")));
        items.add(new Item(10, null, (c, r) -> {
            int n = litCount(p.drawn);
            Ui.text(c, n + (n == 1 ? " pixel" : " pixels") + " · " + DrawnPattern.SIZE + "×" + DrawnPattern.SIZE,
                    r.x() + 1, r.y() + 1, Ui.TEXT_3);
        }));
    }

    private void pageColor() {
        final CrosshairProfile p = profile();
        if (!usesColour(p)) {
            emptyState(Icons.INVERTED, "Inverted", "Colour follows the background");
            if (p.style == StyleType.DRAWN) {
                row(16, (r, out) -> out.add(new Switch(r.x(), r.y(), r.w(), r.h(), "invert", "Invert background", Ui.ACCENT,
                        () -> p.drawnInvert, x -> change(() -> p.drawnInvert = x))));
            }
            return;
        }
        section("Fill");
        row(12, (r, out) -> out.add(new Palette(r.x(), r.y(), r.w(), r.h(), "palette", () -> p.color, x -> p.color = x)));
        row(16, (r, out) -> out.add(new Scrubber(r.x(), r.y(), r.w(), r.h(), "opacity", "Opacity", 20, 255, 255, Ui.ACCENT,
                v -> Math.round(v * 100f / 255f) + "%", () -> (float) ((p.color >>> 24) & 0xFF),
                x -> p.color = (p.color & 0x00FFFFFF) | (((int) (float) x) << 24))));
        row(16, (r, out) -> out.add(new ListRow(r.x(), r.y(), r.w(), r.h(), "rgb",
                customRgb ? Icons.CHEVRON_DOWN : Icons.CHEVRON_RIGHT, "Custom RGB",
                String.format(Locale.ROOT, "#%06X", p.color & 0xFFFFFF), () -> change(() -> customRgb = !customRgb), null)));
        if (customRgb) {
            IntSupplier get = () -> p.color;
            IntConsumer set = x -> p.color = x;
            row(16, (r, out) -> out.add(channel(r, "red", "Red", 16, 255, get, set)));
            row(16, (r, out) -> out.add(channel(r, "green", "Green", 8, 255, get, set)));
            row(16, (r, out) -> out.add(channel(r, "blue", "Blue", 0, 255, get, set)));
        }
        section("Outline");
        row(16, (r, out) -> out.add(new Switch(r.x(), r.y(), r.w(), r.h(), "outline", "Outline", Ui.ACCENT,
                () -> p.outline, x -> change(() -> p.outline = x)).withHint("Border around every shape")));
        if (p.outline) {
            row(16, (r, out) -> out.add(stepper(r, "outline-w", "Width", 1, 4, 1, "px",
                    () -> p.outlineThickness, x -> p.outlineThickness = x)));
            row(12, (r, out) -> out.add(new Palette(r.x(), r.y(), r.w(), r.h(), "outline-palette",
                    () -> p.outlineColor, x -> p.outlineColor = x | 0xFF000000).withHint("Outline colour")));
            row(16, (r, out) -> out.add(new ListRow(r.x(), r.y(), r.w(), r.h(), "outline-rgb",
                    customOutlineRgb ? Icons.CHEVRON_DOWN : Icons.CHEVRON_RIGHT, "Custom outline RGB",
                    String.format(Locale.ROOT, "#%06X", p.outlineColor & 0xFFFFFF),
                    () -> change(() -> customOutlineRgb = !customOutlineRgb), null)));
            if (customOutlineRgb) {
                IntSupplier get = () -> p.outlineColor;
                IntConsumer set = x -> p.outlineColor = x | 0xFF000000;
                row(16, (r, out) -> out.add(channel(r, "o-red", "Red", 16, 0, get, set)));
                row(16, (r, out) -> out.add(channel(r, "o-green", "Green", 8, 0, get, set)));
                row(16, (r, out) -> out.add(channel(r, "o-blue", "Blue", 0, 0, get, set)));
            }
        }
    }

    /** One 0-255 channel of an ARGB colour, read and written through {@code get}/{@code set}. */
    private Scrubber channel(Rect r, String id, String label, int shift, int def, IntSupplier get, IntConsumer set) {
        int mask = ~(0xFF << shift);
        return new Scrubber(r.x(), r.y(), r.w(), r.h(), id, label, 0, 255, def, Ui.ACCENT, String::valueOf,
                () -> (float) ((get.getAsInt() >> shift) & 0xFF),
                x -> set.accept((get.getAsInt() & mask) | (((int) (float) x) << shift)));
    }

    private void pagePresets() {
        row(16, (r, out) -> out.add(new Button(r.x(), r.y(), r.w(), r.h(), "save-preset", "Save current look", Icons.PLUS,
                Button.Kind.GHOST, () -> change(this::savePreset)).withHint("Store " + profileName() + "'s look as a preset")));
        List<CrosshairStudioConfig.CustomPreset> presets = config.savedPresets;
        section(presets.isEmpty() ? "Saved" : "Saved · " + presets.size());
        if (presets.isEmpty()) {
            emptyState(Icons.PRESETS, "No presets yet", "Save a look to reuse it anywhere");
            return;
        }
        for (CrosshairStudioConfig.CustomPreset preset : presets) {
            row(16, (r, out) -> out.add(new ListRow(r.x(), r.y(), r.w(), r.h(), "preset:" + System.identityHashCode(preset),
                    Icons.forStyle(preset.style != null ? preset.style : StyleType.CLASSIC), preset.name,
                    preset.style != null ? preset.style.getDisplayName() : "",
                    () -> change(() -> applyPreset(preset)),
                    () -> change(() -> config.savedPresets.remove(preset)))
                    .withHint("Click to apply to " + profileName() + " · × deletes")));
        }
    }

    private void savePreset() {
        int n = 1;
        while (true) {
            String candidate = "Preset " + n;
            if (config.savedPresets.stream().noneMatch(pr -> candidate.equals(pr.name))) break;
            n++;
        }
        config.savedPresets.add(CrosshairStudioConfig.CustomPreset.of("Preset " + n, profile(), config.showInThirdPerson));
    }

    private void applyPreset(CrosshairStudioConfig.CustomPreset preset) {
        if (editingEnemy) preset.applyLookTo(config.enemy);
        else preset.applyTo(config);
        undo.clear();
        redo.clear();
    }

    private void pageBehavior() {
        section("In game");
        row(16, (r, out) -> out.add(new Switch(r.x(), r.y(), r.w(), r.h(), "enabled", "Custom crosshair", Ui.ACCENT,
                () -> config.enabled, x -> config.enabled = x)));
        row(16, (r, out) -> out.add(new Switch(r.x(), r.y(), r.w(), r.h(), "third", "Third person", Ui.ACCENT,
                () -> config.showInThirdPerson, x -> config.showInThirdPerson = x).withHint("Also show the crosshair in F5 views")));
        section("Enemy profile");
        row(16, (r, out) -> out.add(new Switch(r.x(), r.y(), r.w(), r.h(), "enemy", "Use enemy crosshair", Ui.ENEMY,
                () -> config.enemyEnabled, x -> config.enemyEnabled = x)));
        note("Replaces Primary while you aim at players and hostile mobs.");
        row(16, (r, out) -> out.add(new Button(r.x(), r.y(), r.w(), r.h(), "copy-primary", "Copy Primary to Enemy", null,
                Button.Kind.DANGER, () -> change(() -> config.enemy.copyProfileFrom(config)))
                .withHint("Overwrites the Enemy look with Primary's")));
        section("Keys");
        row(16, (r, out) -> out.add(new ListRow(r.x(), r.y(), r.w(), r.h(), "key-open", null, "Open settings",
                keyName(KeybindManager.openConfigKey), this::openControls, null).asKeycap().withHint("Change in Minecraft Controls")));
        row(16, (r, out) -> out.add(new ListRow(r.x(), r.y(), r.w(), r.h(), "key-toggle", null, "Toggle crosshair",
                keyName(KeybindManager.toggleCrosshairKey), this::openControls, null).asKeycap().withHint("Change in Minecraft Controls")));
    }

    private static String keyName(KeyBinding key) {
        if (key == null || key.isUnbound()) return "None";
        return key.getBoundKeyLocalizedText().getString();
    }

    private void openControls() {
        if (client == null) return;
        ConfigManager.save();
        client.setScreen(new KeybindsScreen(this, client.options));
    }

    /** Lays out the page items from the current scroll position; rows that do not fully fit are hidden. */
    private void placeInspector() {
        Rect v = layout.inspectorView;
        int n = items.size();
        int[] tops = new int[n + 1];
        for (int i = 0; i < n; i++) tops[i + 1] = tops[i] + items.get(i).h() + ITEM_GAP;
        contentHeight = Math.max(0, tops[n] - ITEM_GAP);
        maxScroll = 0;
        while (maxScroll < n - 1 && contentHeight - tops[maxScroll] > v.h()) maxScroll++;
        scrollIndex = Math.max(0, Math.min(scrollIndex, maxScroll));
        scrollTop = tops[scrollIndex];
        hiddenBelow = false;
        List<UiWidget> built = new ArrayList<>();
        for (int i = scrollIndex; i < n; i++) {
            Item item = items.get(i);
            int y = v.y() + tops[i] - scrollTop;
            if (y + item.h() > v.bottom()) {
                hiddenBelow = true;
                break;
            }
            Rect r = new Rect(v.x(), y, v.w(), item.h());
            if (item.paint() != null) placed.add(new Placed(r, item.paint()));
            if (item.build() != null) item.build().build(r, built);
        }
        for (UiWidget w : built) {
            addSelectableChild(w);
            inspectorWidgets.add(w);
        }
    }

    // ================================================================== input

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        if (layout.inspector.contains(mx, my) && maxScroll > 0) {
            int next = Math.max(0, Math.min(maxScroll, scrollIndex - (int) Math.signum(vertical)));
            if (next != scrollIndex) {
                scrollIndex = next;
                init();
            }
            return true;
        }
        if (!drawMode() && layout.canvas.contains(mx, my)) {
            zoomIndex = Math.max(0, Math.min(ZOOMS.length - 1, zoomIndex + (int) Math.signum(vertical)));
            return true;
        }
        return super.mouseScrolled(mx, my, horizontal, vertical);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        boolean handled = super.mouseClicked(mx, my, button);
        if (button == 1 && pixelCanvas != null && pixelCanvas.isMouseOver(mx, my)) rightDrag = pixelCanvas;
        return handled;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (button == 1 && rightDrag != null) {
            rightDrag.dragTo(mx, my);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button == 1) rightDrag = null;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (drawMode()) {
            boolean ctrl = hasControlDown();
            if (ctrl && keyCode == GLFW.GLFW_KEY_Z) {
                if (hasShiftDown()) redo();
                else undo();
                return true;
            }
            if (ctrl && keyCode == GLFW.GLFW_KEY_Y) {
                redo();
                return true;
            }
            if (!ctrl && keyCode == GLFW.GLFW_KEY_P) {
                tool = PixelCanvas.TOOL_PEN;
                return true;
            }
            if (!ctrl && keyCode == GLFW.GLFW_KEY_E) {
                tool = PixelCanvas.TOOL_ERASER;
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // ================================================================== render

    @Override
    public void renderBackground(DrawContext c, int mouseX, int mouseY, float delta) {
        c.fill(0, 0, width, height, Ui.SCRIM);
    }

    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        Ui.tick();
        final AppLayout l = layout;
        final CrosshairProfile p = profile();
        int identity = Ui.mix(Ui.ACCENT, Ui.ENEMY, Ui.motion("identity", editingEnemy ? 1f : 0f, 14f));
        float pulse = updatePulse(p);
        float t = transitionAt == 0L ? 1f : Ui.easeOut((System.nanoTime() - transitionAt) / 1_000_000_000f / TRANSITION_SECONDS);

        // app shell
        Ui.rect(c, l.app.x(), l.app.y(), l.app.w(), l.app.h(), Ui.APP);
        Ui.frame(c, l.app.x() - 1, l.app.y() - 1, l.app.w() + 2, l.app.h() + 2, Ui.LINE);
        Ui.rect(c, l.title.x(), l.title.bottom() - 1, l.title.w(), 1, Ui.LINE);
        Ui.rect(c, l.status.x(), l.status.y(), l.status.w(), 1, Ui.LINE);
        drawTitle(c, l, identity);

        // sidebar
        Ui.rect(c, l.sidebar.x(), l.sidebar.y(), l.sidebar.w(), l.sidebar.h(), Ui.SURFACE);
        Ui.rect(c, l.sidebar.right(), l.sidebar.y(), 1, l.sidebar.h(), Ui.LINE);
        if (l.wideSidebar) {
            Ui.text(c, "Profiles", l.sidebar.x() + 8, l.profilesCaptionY() + 1, Ui.TEXT_3);
            Ui.text(c, "Edit", l.sidebar.x() + 8, l.pagesCaptionY() + 1, Ui.TEXT_3);
            Rect last = l.navItem(AppLayout.NAV_COUNT - 1);
            if (!version.isEmpty() && last.bottom() + 16 < l.sidebar.bottom()) {
                Ui.text(c, "v" + version, l.sidebar.x() + 8, l.sidebar.bottom() - 12, Ui.TEXT_3);
            }
        }
        Rect sel = l.navItem(page.ordinal());
        float iy = Ui.motion("nav-indicator", sel.y(), 22f);
        int ny = Math.round(iy);
        Ui.rect(c, sel.x(), ny, sel.w(), sel.h(), Ui.RAISED);
        Ui.rect(c, l.sidebar.x(), ny + 2, 2, sel.h() - 4, Ui.ACCENT);

        // canvas + inspector surfaces
        drawCanvas(c, l, p, identity, pulse);
        if (l.stacked) Ui.rect(c, l.inspector.x(), l.inspector.y() - 1, l.inspector.w(), 1, Ui.LINE);
        else Ui.rect(c, l.inspector.x() - 1, l.inspector.y(), 1, l.inspector.h(), Ui.LINE);
        Ui.rect(c, l.inspector.x(), l.inspector.y(), l.inspector.w(), l.inspector.h(), Ui.SURFACE);
        Rect ih = l.inspectorHeader;
        Ui.bold(c, Ui.fit(pageTitle(page), ih.w() / 2), ih.x() + 8, ih.y() + 6, Ui.TEXT);
        String who = profileName();
        int whoW = Ui.width(who);
        Ui.rect(c, ih.right() - 8 - whoW - 7, ih.y() + 9, 3, 3, identity);
        Ui.text(c, who, ih.right() - 8 - whoW, ih.y() + 6, Ui.TEXT_2);
        Ui.rect(c, ih.x() + 6, ih.bottom() - 1, ih.w() - 12, 1, Ui.LINE);

        // chrome widgets
        super.render(c, mouseX, mouseY, delta);
        drawCanvasOverlay(c, l, p);

        // inspector content (slides in on page/profile/style changes)
        Rect v = l.inspectorView;
        int slide = Math.round((1f - t) * 10f);
        c.enableScissor(v.x() - 2, v.y(), v.right() + 2, v.bottom());
        c.getMatrices().pushMatrix();
        c.getMatrices().translate(slide, 0);
        for (Placed pl : placed) pl.paint().paint(c, pl.r());
        for (UiWidget w : inspectorWidgets) w.render(c, mouseX - slide, mouseY, delta);
        c.getMatrices().popMatrix();
        c.disableScissor();
        if (t < 1f) Ui.rect(c, v.x() - 2, v.y(), v.w() + 4, v.h(), Ui.alpha(Ui.SURFACE, 1f - t));
        drawScrollbar(c, v);

        drawStatus(c, l, mouseX, mouseY);
    }

    private float updatePulse(CrosshairProfile p) {
        int sig = Objects.hash(p.style, p.thickness, p.length, p.gap, p.color, p.outline, p.outlineThickness, p.outlineColor,
                p.dot, p.dotSize, p.drawn, p.drawnInvert, editingEnemy);
        long now = System.nanoTime();
        if (lastSignature != 0 && sig != lastSignature) pulseAt = now;
        lastSignature = sig;
        if (pulseAt == 0L) return 0f;
        return Math.max(0f, 1f - (now - pulseAt) / 1_000_000_000f / 0.35f);
    }

    private void drawTitle(DrawContext c, AppLayout l, int identity) {
        int x = l.title.x() + 8;
        int ty = l.title.y() + 6;
        Icons.draw(c, Icons.MARK, x, ty, Ui.ACCENT);
        x += Icons.SIZE + 5;
        int limit = l.masterPill.x() - 8;
        String brand = "Custom Crosshair Studio";
        if (x + Ui.boldWidth(brand) > limit) brand = "Crosshair Studio";
        Ui.bold(c, brand, x, ty, Ui.TEXT);
        x += Ui.boldWidth(brand) + 8;
        String crumbProfile = profileName();
        String crumbPage = pageTitle(page);
        int need = Ui.width("/  " + crumbProfile + "  /  " + crumbPage);
        if (x + need <= limit) {
            Ui.text(c, "/", x, ty, Ui.TEXT_3);
            x += Ui.width("/  ");
            Ui.text(c, crumbProfile, x, ty, identity);
            x += Ui.width(crumbProfile + "  ");
            Ui.text(c, "/", x, ty, Ui.TEXT_3);
            x += Ui.width("/  ");
            Ui.text(c, crumbPage, x, ty, Ui.TEXT_2);
        }
    }

    private void drawBackdrop(DrawContext c, Rect r, int which) {
        switch (which) {
            case 1 -> c.fillGradient(r.x(), r.y(), r.right(), r.bottom(), 0xFF6E98DD, 0xFFBBD3F5);
            case 2 -> {
                int[] greys = {0xFF77797E, 0xFF7F8186, 0xFF6F7176, 0xFF85888E};
                int cell = 10;
                for (int gy = r.y(); gy < r.bottom(); gy += cell) {
                    for (int gx = r.x(); gx < r.right(); gx += cell) {
                        int h = (gx * 73856093) ^ (gy * 19349663);
                        Ui.rect(c, gx, gy, Math.min(cell, r.right() - gx), Math.min(cell, r.bottom() - gy), greys[(h >>> 7) & 3]);
                    }
                }
            }
            case 3 -> Ui.rect(c, r.x(), r.y(), r.w(), r.h(), BACKDROPS[3]);
            default -> {
                Ui.rect(c, r.x(), r.y(), r.w(), r.h(), BACKDROPS[0]);
                for (int gy = r.y() + 6; gy < r.bottom() - 2; gy += 8) {
                    for (int gx = r.x() + 6; gx < r.right() - 2; gx += 8) Ui.rect(c, gx, gy, 1, 1, 0x16FFFFFF);
                }
            }
        }
    }

    private void drawCanvas(DrawContext c, AppLayout l, CrosshairProfile p, int identity, float pulse) {
        Rect cv = l.canvas;
        int marks = Ui.mix(Ui.alpha(identity, 0.55f), 0xFFFFFFFF, pulse * 0.6f);
        if (l.drawMode) {
            drawBackdrop(c, cv, 0);
            Ui.cropMarks(c, cv.x() + 3, cv.y() + 3, cv.w() - 6, cv.h() - 6, 6, marks);
        } else {
            drawBackdrop(c, cv, backdrop);
            int cx = cv.centerX();
            int cy = cv.y() + cv.h() / 2;
            Preview.draw(c, p, cx, cy, ZOOMS[zoomIndex], cv.x() + 1, cv.y() + 1, cv.w() - 2, cv.h() - 2);
            int guide = Ui.alpha(identity, 0.5f);
            Ui.rect(c, cx, cv.y() + 1, 1, 4, guide);
            Ui.rect(c, cx, cv.bottom() - 5, 1, 4, guide);
            Ui.rect(c, cv.x() + 1, cy, 4, 1, guide);
            Ui.rect(c, cv.right() - 5, cy, 4, 1, guide);
            Ui.cropMarks(c, cv.x() + 3, cv.y() + 3, cv.w() - 6, cv.h() - 6, 6, marks);
        }

        // breadcrumb chip: which profile, which style
        String chip = profileName() + " · " + p.style.getDisplayName() + (config.enabled ? "" : " · hidden");
        String shown = Ui.fit(chip, l.chipMaxWidth(editingEnemy) - 16);
        int cw = Ui.width(shown) + 16;
        int chx = cv.x() + 6, chy = cv.y() + 5;
        Ui.rect(c, chx, chy, cw, 13, Ui.alpha(Ui.WELL, 0.88f));
        Ui.frame(c, chx, chy, cw, 13, Ui.alpha(identity, 0.5f));
        Ui.rect(c, chx + 5, chy + 5, 3, 3, identity);
        Ui.text(c, shown, chx + 11, chy + 3, Ui.TEXT);
    }

    private void drawCanvasOverlay(DrawContext c, AppLayout l, CrosshairProfile p) {
        if (!l.drawMode || pixelCanvas == null) return;
        Rect cv = l.canvas;
        int by = cv.bottom() - 13;
        int[] off = pixelCanvas.hoveredOffset();
        String pos = off == null ? "Anchor at centre" : String.format(Locale.ROOT, "x %+d   y %+d", off[0], off[1]);
        int left = l.railButton(0).right() + 8;
        int avail = cv.right() - 8 - left;
        int n = litCount(p.drawn);
        String count = n + " px";
        String withMode = mirror == PixelCanvas.MIRROR_OFF ? count
                : "Mirror " + new String[]{"", "X", "Y", "XY"}[mirror] + "   " + count;
        String right = Ui.width(pos) + 10 + Ui.width(withMode) <= avail ? withMode : count;
        Ui.textRight(c, right, cv.right() - 8, by, Ui.TEXT_3);
        if (Ui.width(pos) + 10 + Ui.width(right) <= avail) Ui.text(c, pos, left, by, off == null ? Ui.TEXT_3 : Ui.TEXT_2);
    }

    private void drawScrollbar(DrawContext c, Rect v) {
        if (maxScroll <= 0) return;
        int trackX = v.right() + 3;
        Ui.rect(c, trackX, v.y(), 1, v.h(), Ui.LINE);
        int thumbH = Math.max(8, v.h() * v.h() / Math.max(1, contentHeight));
        int thumbY = v.y() + (v.h() - thumbH) * scrollIndex / maxScroll;
        Ui.rect(c, trackX, thumbY, 1, thumbH, Ui.TEXT_3);
        if (hiddenBelow) c.fillGradient(v.x(), v.bottom() - 8, v.right(), v.bottom(), 0x00101216, Ui.SURFACE);
    }

    private void drawStatus(DrawContext c, AppLayout l, int mouseX, int mouseY) {
        String hint = null;
        Optional<Element> hovered = hoveredElement(mouseX, mouseY);
        if (hovered.isPresent() && hovered.get() instanceof UiWidget w) hint = w.hint();
        if (hint == null) hint = defaultHint();
        int maxW = l.reset.x() - 8 - (l.status.x() + 8);
        Ui.text(c, Ui.fit(hint, maxW), l.status.x() + 8, l.status.y() + 5, Ui.TEXT_3);
    }

    private String defaultHint() {
        return switch (page) {
            case STYLE -> "Pick a style · thumbnails show your own crosshair";
            case SHAPE -> drawMode() ? "Draw on the canvas · the marked cell is the exact centre" : "Right-click any value to reset it";
            case COLOR -> "Right-click any value to reset it";
            case PRESETS -> "Presets apply to the profile you are editing";
            case BEHAVIOR -> "Global settings · shared by both profiles";
        };
    }

    // ================================================================== lifecycle

    @Override
    public void close() {
        ConfigManager.save();
        if (client != null) client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
