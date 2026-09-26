package net.customcrosshairstudio;

import net.customcrosshairstudio.gui.ui.AppLayout;
import net.customcrosshairstudio.gui.ui.AppLayout.Rect;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Layout invariants for every GUI size Minecraft can produce (never below 320x240), in both canvas modes. */
class AppLayoutTest {
    private static final int TALLEST_ITEM = 48;

    private static List<int[]> sizes() {
        List<int[]> out = new ArrayList<>(List.of(
                new int[]{320, 240}, new int[]{427, 240}, new int[]{480, 270}, new int[]{640, 240}, new int[]{640, 360},
                new int[]{854, 480}, new int[]{960, 540}, new int[]{1280, 720}, new int[]{1920, 1080}));
        for (int w = 320; w <= 900; w += 11) {
            for (int h = 240; h <= 520; h += 13) out.add(new int[]{w, h});
        }
        return out;
    }

    private static void inside(String at, Rect outer, Rect inner) {
        assertTrue(outer.contains(inner), at + ": " + inner + " not inside " + outer);
    }

    private static void apart(String at, Rect a, Rect b) {
        assertFalse(a.intersects(b), at + ": " + a + " overlaps " + b);
    }

    @Test
    void shellRegionsFitAndDoNotOverlap() {
        for (int[] s : sizes()) {
            for (boolean draw : new boolean[]{false, true}) {
                AppLayout l = AppLayout.compute(s[0], s[1], draw);
                String at = s[0] + "x" + s[1] + (draw ? " draw" : "");
                inside(at, new Rect(0, 0, s[0], s[1]), l.app);
                for (Rect r : new Rect[]{l.title, l.status, l.body, l.sidebar, l.main, l.canvas, l.inspector,
                        l.inspectorHeader, l.inspectorView, l.close, l.masterPill, l.done, l.reset}) {
                    inside(at, l.app, r);
                    assertTrue(r.w() > 0 && r.h() > 0, at + " empty " + r);
                }
                apart(at, l.title, l.body);
                apart(at, l.body, l.status);
                apart(at, l.sidebar, l.main);
                apart(at, l.canvas, l.inspector);
                inside(at, l.main, l.canvas);
                inside(at, l.main, l.inspector);
                inside(at, l.inspector, l.inspectorView);
                apart(at, l.inspectorHeader, l.inspectorView);

                inside(at, l.title, l.close);
                inside(at, l.title, l.masterPill);
                apart(at, l.close, l.masterPill);
                assertTrue(l.masterPill.x() - (l.title.x() + 70) >= 0, at + " brand room");
                inside(at, l.status, l.done);
                inside(at, l.status, l.reset);
                apart(at, l.done, l.reset);
                assertTrue(l.reset.x() - l.status.x() >= 100, at + " hint room");

                assertTrue(l.inspectorView.h() >= TALLEST_ITEM + 4, at + " inspector view " + l.inspectorView.h());
                assertTrue(l.inspectorView.w() >= 120, at + " inspector width " + l.inspectorView.w());
                assertTrue(l.canvas.h() >= 64, at + " canvas height");
            }
        }
    }

    @Test
    void sidebarItemsFitAndDoNotOverlap() {
        for (int[] s : sizes()) {
            AppLayout l = AppLayout.compute(s[0], s[1], false);
            String at = s[0] + "x" + s[1];
            List<Rect> all = new ArrayList<>();
            for (int i = 0; i < AppLayout.PROFILE_COUNT; i++) all.add(l.profileSlot(i));
            for (int i = 0; i < AppLayout.NAV_COUNT; i++) all.add(l.navItem(i));
            for (int i = 0; i < all.size(); i++) {
                inside(at, l.sidebar, all.get(i));
                for (int j = i + 1; j < all.size(); j++) apart(at, all.get(i), all.get(j));
            }
            if (l.wideSidebar) {
                assertTrue(l.profilesCaptionY() + 10 <= l.profileSlot(0).y(), at + " profiles caption");
                assertTrue(l.pagesCaptionY() >= l.profileSlot(1).bottom(), at + " pages caption");
                assertTrue(l.pagesCaptionY() + 10 <= l.navItem(0).y(), at + " pages caption gap");
            }
        }
    }

    @Test
    void previewCanvasControlsFit() {
        for (int[] s : sizes()) {
            AppLayout l = AppLayout.compute(s[0], s[1], false);
            String at = s[0] + "x" + s[1];
            Rect band = new Rect(l.canvas.x(), l.canvas.y(), l.canvas.w(), AppLayout.CANVAS_TOP_BAND);
            for (Rect r : new Rect[]{l.backdropPicker(), l.zoomPicker(), l.enemyPill()}) inside(at, l.canvas, r);
            apart(at, l.backdropPicker(), l.zoomPicker());
            apart(at, l.backdropPicker(), band);
            apart(at, l.zoomPicker(), band);
            inside(at, band, l.enemyPill());
            assertTrue(l.chipMaxWidth(true) >= 60, at + " chip room " + l.chipMaxWidth(true));
        }
    }

    @Test
    void drawWorkspaceFits() {
        for (int[] s : sizes()) {
            AppLayout l = AppLayout.compute(s[0], s[1], true);
            String at = s[0] + "x" + s[1] + " draw";
            Rect cv = l.canvas;
            Rect grid = l.grid(15);
            int cell = l.gridCell(15);
            assertTrue(cell >= 4, at + " cell " + cell);
            assertEquals(cell * 15, grid.w());
            inside(at, cv, grid);
            // rulers sit 6px outside the grid and must clear the top band chip and the rail
            assertTrue(grid.y() - 6 >= cv.y() + 18, at + " top ruler");
            assertTrue(grid.bottom() <= cv.bottom() - AppLayout.CANVAS_BOTTOM_BAND, at + " bottom band");
            for (int i = 0; i < AppLayout.RAIL_BUTTONS; i++) {
                Rect b = l.railButton(i);
                inside(at, cv, b);
                assertTrue(b.y() >= cv.y() + AppLayout.CANVAS_TOP_BAND, at + " rail below chip");
                assertTrue(b.bottom() <= cv.bottom() - 4, at + " rail bottom");
                assertTrue(b.right() + 6 <= grid.x() - 1, at + " rail vs left ruler");
                for (int j = i + 1; j < AppLayout.RAIL_BUTTONS; j++) apart(at, b, l.railButton(j));
            }
        }
    }

    @Test
    void reflowsBetweenWideAndCompact() {
        AppLayout wide = AppLayout.compute(960, 540, false);
        assertTrue(wide.wideSidebar);
        assertFalse(wide.stacked);
        AppLayout small = AppLayout.compute(320, 240, false);
        assertFalse(small.wideSidebar);
        assertTrue(small.stacked);
        AppLayout smallDraw = AppLayout.compute(320, 240, true);
        assertTrue(smallDraw.canvas.h() > small.canvas.h(), "draw workspace gets a bigger canvas");
    }
}
