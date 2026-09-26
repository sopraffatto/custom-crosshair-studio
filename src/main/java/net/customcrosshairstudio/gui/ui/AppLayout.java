package net.customcrosshairstudio.gui.ui;

/**
 * Geometry of the Custom Crosshair Studio app window, in GUI pixels. Pure maths (no Minecraft types) so every
 * size Minecraft can produce (never smaller than 320x240) is unit-tested.
 *
 * <pre>
 * +-------------------------------------------------------------+
 * | title bar                                                   |
 * +---------+------------------------------+--------------------+
 * | sidebar | canvas (preview / pixel      | inspector          |
 * | profiles|         editor workspace)    | (page controls)    |
 * | pages   |                              |                    |
 * +---------+------------------------------+--------------------+
 * | status bar: contextual hint                  reset   done  |
 * +-------------------------------------------------------------+
 * </pre>
 * Reflow rules: the sidebar collapses to an icon rail on small windows; when the main area is
 * narrow the inspector moves below the canvas. In the Draw workspace the canvas gets priority.
 */
public final class AppLayout {
    public record Rect(int x, int y, int w, int h) {
        public int right() { return x + w; }
        public int bottom() { return y + h; }
        public int centerX() { return x + w / 2; }
        public int centerY() { return y + h / 2; }

        public boolean contains(Rect o) {
            return o.x >= x && o.y >= y && o.right() <= right() && o.bottom() <= bottom();
        }

        public boolean contains(double px, double py) {
            return px >= x && py >= y && px < right() && py < bottom();
        }

        public boolean intersects(Rect o) {
            return x < o.right() && o.x < right() && y < o.bottom() && o.y < bottom();
        }
    }

    public static final int TITLE_H = 20;
    public static final int STATUS_H = 16;
    public static final int WIDE_SIDEBAR_W = 108;
    public static final int RAIL_W = 28;
    public static final int INSPECTOR_HEADER_H = 20;
    public static final int NAV_COUNT = 5;
    public static final int PROFILE_COUNT = 2;
    public static final int RAIL_BUTTONS = 5;
    public static final int CANVAS_TOP_BAND = 24;
    public static final int CANVAS_BOTTOM_BAND = 16;

    public final int screenW;
    public final int screenH;
    public final boolean drawMode;
    public final boolean wideSidebar;
    public final boolean stacked;

    public final Rect app;
    public final Rect title;
    public final Rect status;
    public final Rect body;
    public final Rect sidebar;
    public final Rect main;
    public final Rect canvas;
    public final Rect inspector;
    public final Rect inspectorHeader;
    public final Rect inspectorView;

    public final Rect close;
    public final Rect masterPill;
    public final Rect done;
    public final Rect reset;

    private AppLayout(int screenW, int screenH, boolean drawMode) {
        this.screenW = screenW;
        this.screenH = screenH;
        this.drawMode = drawMode;

        int m = clamp(Math.min(screenW, screenH) / 40, 4, 14);
        int aw = Math.min(screenW - 2 * m, 640);
        int ah = Math.min(screenH - 2 * m, 360);
        app = new Rect((screenW - aw) / 2, (screenH - ah) / 2, aw, ah);
        title = new Rect(app.x, app.y, aw, TITLE_H);
        status = new Rect(app.x, app.bottom() - STATUS_H, aw, STATUS_H);
        body = new Rect(app.x, title.bottom(), aw, status.y - title.bottom());

        wideSidebar = aw >= 470 && body.h >= 190;
        int sw = wideSidebar ? WIDE_SIDEBAR_W : RAIL_W;
        sidebar = new Rect(body.x, body.y, sw, body.h);
        main = new Rect(sidebar.right() + 1, body.y, body.w - sw - 1, body.h);

        stacked = main.w < 300;
        if (!stacked) {
            int iw = clamp(main.w * 38 / 100, 150, 196);
            canvas = new Rect(main.x, main.y, main.w - iw - 1, main.h);
            inspector = new Rect(canvas.right() + 1, main.y, iw, main.h);
        } else {
            int ch = drawMode ? main.h * 58 / 100 : clamp(main.h * 38 / 100, 64, 120);
            canvas = new Rect(main.x, main.y, main.w, ch);
            inspector = new Rect(main.x, canvas.bottom() + 1, main.w, main.h - ch - 1);
        }
        inspectorHeader = new Rect(inspector.x, inspector.y, inspector.w, INSPECTOR_HEADER_H);
        inspectorView = new Rect(inspector.x + 6, inspectorHeader.bottom() + 2, inspector.w - 12,
                inspector.bottom() - 4 - (inspectorHeader.bottom() + 2));

        close = new Rect(title.right() - 4 - 14, title.y + 3, 14, 14);
        masterPill = new Rect(close.x - 4 - 62, title.y + 3, 62, 14);
        done = new Rect(status.right() - 4 - 44, status.y + 2, 44, 12);
        reset = new Rect(done.x - 4 - 72, status.y + 2, 72, 12);
    }

    public static AppLayout compute(int screenW, int screenH, boolean drawMode) {
        return new AppLayout(screenW, screenH, drawMode);
    }

    // ------------------------------------------------------------------ sidebar

    private int wideTop() {
        return sidebar.y + 6;
    }

    /** Y of the "Profiles" caption (wide sidebar only). */
    public int profilesCaptionY() {
        return wideTop();
    }

    /** Y of the "Edit" caption (wide sidebar only). */
    public int pagesCaptionY() {
        return profileSlot(PROFILE_COUNT - 1).bottom() + 8;
    }

    public Rect profileSlot(int i) {
        if (wideSidebar) return new Rect(sidebar.x + 6, wideTop() + 12 + i * 31, sidebar.w - 12, 28);
        return new Rect(sidebar.x + 3, sidebar.y + 5 + i * 25, 22, 22);
    }

    public Rect navItem(int i) {
        if (wideSidebar) return new Rect(sidebar.x + 4, pagesCaptionY() + 12 + i * 18, sidebar.w - 8, 17);
        return new Rect(sidebar.x + 3, profileSlot(PROFILE_COUNT - 1).bottom() + 9 + i * 21, 22, 20);
    }

    // ------------------------------------------------------------------ canvas furniture (preview mode)

    public Rect backdropPicker() {
        return new Rect(canvas.x + 6, canvas.bottom() - 6 - 14, 4 * 12 + 2, 14);
    }

    public Rect zoomPicker() {
        return new Rect(canvas.right() - 6 - 50, canvas.bottom() - 6 - 14, 50, 14);
    }

    public Rect enemyPill() {
        return new Rect(canvas.right() - 6 - 54, canvas.y + 5, 54, 13);
    }

    /** Space left for the top-left breadcrumb chip. */
    public int chipMaxWidth(boolean withPill) {
        return canvas.w - 12 - (withPill ? enemyPill().w + 6 : 0);
    }

    // ------------------------------------------------------------------ canvas furniture (draw mode)

    public int railButtonSize() {
        int avail = canvas.bottom() - 6 - (canvas.y + CANVAS_TOP_BAND);
        return clamp((avail - 14) / RAIL_BUTTONS, 12, 16);
    }

    /** Tool rail buttons: pen, eraser | undo, redo | clear. */
    public Rect railButton(int i) {
        int b = railButtonSize();
        int y = canvas.y + CANVAS_TOP_BAND;
        for (int k = 0; k < i; k++) y += b + (k == 1 || k == 3 ? 5 : 2);
        return new Rect(canvas.x + 6, y, b, b);
    }

    private Rect gridArea() {
        int x0 = canvas.x + 6 + railButtonSize() + 8;
        int y0 = canvas.y + CANVAS_TOP_BAND;
        return new Rect(x0, y0, canvas.right() - 8 - x0, canvas.bottom() - CANVAS_BOTTOM_BAND - y0);
    }

    public int gridCell(int cells) {
        Rect a = gridArea();
        return clamp(Math.min(a.w, a.h) / cells, 4, 14);
    }

    public Rect grid(int cells) {
        Rect a = gridArea();
        int size = gridCell(cells) * cells;
        return new Rect(a.x + (a.w - size) / 2, a.y + (a.h - size) / 2, size, size);
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
