package net.customcrosshairstudio;

import com.google.gson.Gson;
import net.customcrosshairstudio.config.CrosshairStudioConfig;
import net.customcrosshairstudio.config.CrosshairProfile;
import net.customcrosshairstudio.config.DrawnPattern;
import net.customcrosshairstudio.render.RenderUtils;
import net.customcrosshairstudio.render.RenderUtils.Layout;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GeometryAndConfigTest {

    private static boolean coveredBy(List<int[]> rects, int x, int y) {
        for (int[] r : rects) {
            if (x >= r[0] && x < r[2] && y >= r[1] && y < r[3]) return true;
        }
        return false;
    }

    @ParameterizedTest
    @CsvSource({
            "213.5, 120.5, 1, 1, 0, 3", "213.5, 120.5, 1, 1, 1, 3", "213.5, 120.5, 3, 4, 2, 2",
            "427.0, 241.0, 4, 6, 4, 4", "213.0, 120.0, 2, 3, 0, 1", "426.5, 239.5, 3, 6, 3, 3"
    })
    void armsAreSymmetricAroundTheCentre(float cx, float cy, float thick, float len, float gap, int scale) {
        Layout l = Layout.of(cx, cy, thick, len, gap, scale);
        int[][] a = l.arms(); // right, left, bottom, top
        assertEquals(a[0][2] - l.cx(), l.cx() - a[1][0], "right/left reach");
        assertEquals(a[2][3] - l.cy(), l.cy() - a[3][1], "bottom/top reach");
        assertEquals(a[0][2] - l.cx(), a[2][3] - l.cy(), "horizontal/vertical reach");
        assertEquals(a[0][0] - l.cx(), l.cx() - a[1][2], "inner edges");
        for (int[] arm : a) {
            boolean horizontal = arm[3] - arm[1] == 2 * l.half();
            boolean vertical = arm[2] - arm[0] == 2 * l.half();
            assertTrue(horizontal || vertical);
        }
        // bar thickness is symmetric about the anchor
        assertEquals(l.cy() - a[0][1], a[0][3] - l.cy());
        assertEquals(l.cx() - a[2][0], a[2][2] - l.cx());
    }

    @Test
    void defaultSizeIsIndependentOfGuiScale() {
        // thickness 1, length 1, gap 0 -> 6 framebuffer px across at any GUI scale (before outline)
        for (int scale : new int[]{1, 2, 3, 4}) {
            Layout l = Layout.of(213.5f, 120.5f, 1, 1, 0, scale);
            int[][] a = l.arms();
            assertEquals(6, a[0][2] - a[1][0]);
            assertEquals(6, a[2][3] - a[3][1]);
            assertEquals(2, a[0][3] - a[0][1]);
        }
    }

    @Test
    void gapOneLeavesATwoPixelHoleAndZeroGapJoinsIntoAPlus() {
        Layout g0 = Layout.of(100, 100, 1, 1, 0, 2);
        assertEquals(g0.arms()[1][2], g0.arms()[0][0], "gap 0: arms touch at the centre");
        Layout g1 = Layout.of(100, 100, 1, 1, 1, 2);
        assertEquals(2, g1.arms()[0][0] - g1.arms()[1][2], "gap 1: 2px hole");
        // arm length does not shrink when the gap grows
        assertEquals(g0.arms()[0][2] - g0.arms()[0][0], g1.arms()[0][2] - g1.arms()[0][0]);
    }

    @ParameterizedTest
    @CsvSource({"1,1", "1,2", "1,3", "2,1", "3,2", "4,3", "4,4"})
    void dotIsConcentricWithBars(float thick, float dot) {
        Layout l = Layout.of(213.5f, 120.5f, thick, 3, 2, 3);
        int[] d = l.dot(dot);
        assertEquals(l.cx() - d[0], d[2] - l.cx());
        assertEquals(l.cy() - d[1], d[3] - l.cy());
        assertEquals(l.cx() - l.half(), l.cx() - l.half());
    }

    @ParameterizedTest
    @CsvSource({"213.5, 120.5, 2", "214.0, 121.0, 3", "427.0, 241.0, 4"})
    void drawnCentreCellIsCentredOnTheAnchor(float cx, float cy, int scale) {
        int[] o = RenderUtils.drawnOrigin(cx, cy, scale);
        // one drawn cell = one GUI pixel; centre cell is the same GUI pixel vanilla puts its centre on
        int cellX1 = o[0] + DrawnPattern.CENTER * scale;
        int cellY1 = o[1] + DrawnPattern.CENTER * scale;
        assertEquals((int) Math.floor(cx - 0.5f) * scale, cellX1);
        assertEquals((int) Math.floor(cy - 0.5f) * scale, cellY1);
        assertEquals(cx * scale - scale / 2.0f, cellX1 + scale / 2.0f, scale, "within one GUI pixel of the true centre");
    }

    @Test
    void invertedTilesAreDisjointAndCoverTheUnion() {
        for (float gap : new float[]{0, 1, 2, 4}) {
            for (float thick : new float[]{1, 3, 4}) {
                Layout l = Layout.of(213.5f, 120.5f, thick, 2, gap, 3);
                List<int[]> shapes = new ArrayList<>(List.of(l.arms()));
                shapes.add(l.dot(4));
                List<int[]> tiles = RenderUtils.disjointUnion(shapes);
                for (int y = l.cy() - 40; y < l.cy() + 40; y++) {
                    for (int x = l.cx() - 40; x < l.cx() + 40; x++) {
                        int hits = 0;
                        for (int[] t : tiles) {
                            if (x >= t[0] && x < t[2] && y >= t[1] && y < t[3]) hits++;
                        }
                        assertEquals(coveredBy(shapes, x, y) ? 1 : 0, hits,
                                "pixel " + x + "," + y + " gap=" + gap + " thick=" + thick);
                    }
                }
            }
        }
    }

    @Test
    void drawnPatternEditingAndNormalization() {
        var rows = DrawnPattern.empty();
        assertTrue(DrawnPattern.isEmpty(rows));
        rows = DrawnPattern.with(rows, 3, 4, true);
        assertTrue(DrawnPattern.isSet(rows, 3, 4));
        assertFalse(DrawnPattern.isSet(rows, 4, 3));
        rows = DrawnPattern.with(rows, 3, 4, false);
        assertTrue(DrawnPattern.isEmpty(rows));
        assertEquals(DrawnPattern.SIZE, DrawnPattern.with(rows, 99, 99, true).size());
        var bad = DrawnPattern.normalize(List.of("#x#", "###"));
        assertEquals(DrawnPattern.SIZE, bad.size());
        assertTrue(bad.stream().allMatch(r -> r.length() == DrawnPattern.SIZE));
        assertTrue(DrawnPattern.isSet(bad, 0, 0));
        assertFalse(DrawnPattern.isSet(bad, 1, 0));
        assertEquals(DrawnPattern.SIZE, DrawnPattern.normalize(null).size());
    }

    @Test
    void defaultDrawnPatternIsSymmetric() {
        var rows = DrawnPattern.defaultRows();
        for (int i = 1; i <= 2; i++) {
            assertEquals(DrawnPattern.isSet(rows, DrawnPattern.CENTER - i, DrawnPattern.CENTER),
                    DrawnPattern.isSet(rows, DrawnPattern.CENTER + i, DrawnPattern.CENTER));
            assertEquals(DrawnPattern.isSet(rows, DrawnPattern.CENTER, DrawnPattern.CENTER - i),
                    DrawnPattern.isSet(rows, DrawnPattern.CENTER, DrawnPattern.CENTER + i));
        }
    }

    @Test
    void enemyProfileIsIndependentAndSelectedOnlyWhenEnabledAndTargeted() {
        CrosshairStudioConfig config = new CrosshairStudioConfig();
        config.enemy.style = CrosshairStudioConfig.StyleType.DRAWN;
        config.enemy.drawn = DrawnPattern.with(DrawnPattern.empty(), 7, 7, true);

        assertSame(config, config.activeProfile(true), "disabled -> normal even when targeting");
        config.enemyEnabled = true;
        assertSame(config, config.activeProfile(false), "not targeting -> normal");
        assertSame(config.enemy, config.activeProfile(true), "targeting -> enemy");

        config.style = CrosshairStudioConfig.StyleType.INVERTED;
        config.thickness = 3;
        config.color = 0xFF00FF00;
        assertEquals(CrosshairStudioConfig.StyleType.DRAWN, config.enemy.style);
        assertEquals(1.0f, config.enemy.thickness);
        assertEquals(0xFFFF3B3B, config.enemy.color);
        config.enemy.gap = 4;
        assertEquals(0.0f, config.gap);
    }

    @Test
    void drawnAsNormalAndAsEnemySurvivesJsonRoundTrip() {
        CrosshairStudioConfig config = new CrosshairStudioConfig();
        config.style = CrosshairStudioConfig.StyleType.DRAWN;
        config.drawn = DrawnPattern.with(DrawnPattern.empty(), 2, 9, true);
        config.enemyEnabled = true;
        config.enemy.style = CrosshairStudioConfig.StyleType.DRAWN;
        config.enemy.drawn = DrawnPattern.with(DrawnPattern.empty(), 11, 3, true);
        config.savedPresets.add(new CrosshairStudioConfig.CustomPreset("D", config));

        Gson gson = new Gson();
        CrosshairStudioConfig loaded = gson.fromJson(gson.toJson(config), CrosshairStudioConfig.class);
        loaded.normalize();

        assertEquals(CrosshairStudioConfig.StyleType.DRAWN, loaded.style);
        assertTrue(DrawnPattern.isSet(loaded.drawn, 2, 9));
        assertFalse(DrawnPattern.isSet(loaded.drawn, 11, 3));
        assertTrue(loaded.enemyEnabled);
        assertEquals(CrosshairStudioConfig.StyleType.DRAWN, loaded.enemy.style);
        assertTrue(DrawnPattern.isSet(loaded.enemy.drawn, 11, 3));
        assertFalse(DrawnPattern.isSet(loaded.enemy.drawn, 2, 9));
        assertTrue(DrawnPattern.isSet(loaded.savedPresets.getFirst().drawn, 2, 9));
    }

    @Test
    void oldConfigWithoutNewFieldsLoadsWithSafeDefaults() {
        String old = "{\"configVersion\":4,\"enabled\":true,\"style\":\"CLASSIC\",\"color\":-1,"
                + "\"outline\":true,\"thickness\":2.0,\"length\":9.0,\"gap\":7.0,\"dot\":false,"
                + "\"dotSize\":2.0,\"showInThirdPerson\":true,\"savedPresets\":[{\"name\":\"A\",\"style\":\"DOT\"}]}";
        CrosshairStudioConfig loaded = new Gson().fromJson(old, CrosshairStudioConfig.class);
        loaded.normalize();

        assertEquals(6.0f, loaded.length);
        assertEquals(4.0f, loaded.gap);
        assertTrue(loaded.outline);
        assertTrue(loaded.showInThirdPerson);
        assertFalse(loaded.enemyEnabled);
        assertNotNull(loaded.enemy);
        assertEquals(DrawnPattern.SIZE, loaded.drawn.size());
        assertEquals(DrawnPattern.SIZE, loaded.savedPresets.getFirst().drawn.size());
        assertEquals(CrosshairStudioConfig.CURRENT_CONFIG_VERSION, loaded.configVersion);
    }

    @Test
    void copyFromKeepsEnemyProfileIndependent() {
        CrosshairStudioConfig source = new CrosshairStudioConfig();
        source.enemyEnabled = true;
        source.enemy.gap = 3;
        CrosshairStudioConfig copy = new CrosshairStudioConfig();
        copy.copyFrom(source);
        copy.enemy.gap = 1;
        assertTrue(copy.enemyEnabled);
        assertEquals(3.0f, source.enemy.gap);
        assertNotSame(source.enemy, copy.enemy);
    }

    @Test
    void resetToDefaultsLeavesEnemyProfileAlone() {
        CrosshairStudioConfig config = new CrosshairStudioConfig();
        config.enemy.color = 0xFF123456;
        config.thickness = 3;
        config.resetToDefaults();
        assertEquals(1.0f, config.thickness);
        assertEquals(0xFF123456, config.enemy.color);
    }

    @Test
    void configNormalizationRemovesMalformedPresetData() {
        CrosshairStudioConfig config = new CrosshairStudioConfig();
        config.style = null;
        config.thickness = Float.NaN;
        config.length = 99.0f;
        config.gap = -1.0f;
        config.dotSize = 99.0f;
        config.outlineThickness = 50.0f;
        config.savedPresets = new ArrayList<>();
        config.savedPresets.add(null);
        CrosshairStudioConfig.CustomPreset preset = new CrosshairStudioConfig.CustomPreset();
        preset.name = "";
        preset.style = null;
        preset.thickness = Float.POSITIVE_INFINITY;
        config.savedPresets.add(preset);
        config.normalize();

        assertEquals(CrosshairStudioConfig.StyleType.CLASSIC, config.style);
        assertEquals(1.0f, config.thickness);
        assertEquals(6.0f, config.length);
        assertEquals(0.0f, config.gap);
        assertEquals(4.0f, config.dotSize);
        assertEquals(4.0f, config.outlineThickness);
        assertEquals(1, config.savedPresets.size());
        assertEquals("Preset", config.savedPresets.getFirst().name);
        assertEquals(1.0f, config.savedPresets.getFirst().thickness);
    }

    @Test
    void shippedDefaultsMatchSpec() {
        CrosshairStudioConfig config = new CrosshairStudioConfig();
        assertEquals(1.0f, config.thickness);
        assertEquals(1.0f, config.length);
        assertEquals(0.0f, config.gap);
        assertEquals(1.0f, config.outlineThickness);
        assertFalse(config.dot);
        assertFalse(config.showInThirdPerson);
        assertFalse(config.outline);
        assertEquals(0xFFFFFFFF, config.color);
    }

    @Test
    void copyFromDeepCopiesPresets() {
        CrosshairStudioConfig source = new CrosshairStudioConfig();
        source.showInThirdPerson = true;
        source.outlineThickness = 3.0f;
        source.savedPresets.add(new CrosshairStudioConfig.CustomPreset("Competitive", source));
        CrosshairStudioConfig copy = new CrosshairStudioConfig();
        copy.copyFrom(source);
        assertTrue(copy.showInThirdPerson);
        assertEquals(3.0f, copy.savedPresets.getFirst().outlineThickness);
        copy.savedPresets.getFirst().name = "Changed";
        assertEquals("Competitive", source.savedPresets.getFirst().name);
    }

    @Test
    void presetsSnapshotAndApplyLookToEitherProfile() {
        CrosshairStudioConfig config = new CrosshairStudioConfig();
        config.enemy.style = CrosshairStudioConfig.StyleType.DRAWN;
        config.enemy.drawn = DrawnPattern.with(DrawnPattern.empty(), 4, 4, true);
        config.enemy.drawnInvert = true;
        config.enemy.color = 0xFF112233;

        CrosshairStudioConfig.CustomPreset fromEnemy = CrosshairStudioConfig.CustomPreset.of("E", config.enemy, false);
        config.enemy.drawn = DrawnPattern.empty();
        assertTrue(DrawnPattern.isSet(fromEnemy.drawn, 4, 4), "preset keeps its own copy");

        config.showInThirdPerson = true;
        fromEnemy.applyLookTo(config);
        assertEquals(CrosshairStudioConfig.StyleType.DRAWN, config.style);
        assertTrue(config.drawnInvert);
        assertEquals(0xFF112233, config.color);
        assertTrue(DrawnPattern.isSet(config.drawn, 4, 4));
        assertTrue(config.showInThirdPerson, "look-only apply leaves global settings alone");
        assertTrue(DrawnPattern.isEmpty(config.enemy.drawn), "applying to Primary leaves Enemy untouched");

        CrosshairStudioConfig.CustomPreset bad = new CrosshairStudioConfig.CustomPreset();
        bad.style = null;
        bad.thickness = 99;
        bad.applyLookTo(config.enemy);
        assertEquals(CrosshairStudioConfig.StyleType.CLASSIC, config.enemy.style);
        assertEquals(4.0f, config.enemy.thickness, "applied look is normalized");
    }

    @Test
    void enemyDrawnInvertSurvivesJsonRoundTrip() {
        CrosshairStudioConfig config = new CrosshairStudioConfig();
        config.enemy.style = CrosshairStudioConfig.StyleType.DRAWN;
        config.enemy.drawnInvert = true;
        Gson gson = new Gson();
        CrosshairStudioConfig loaded = gson.fromJson(gson.toJson(config), CrosshairStudioConfig.class);
        loaded.normalize();
        assertTrue(loaded.enemy.drawnInvert);
        assertFalse(loaded.drawnInvert);
    }
    @Test
    void outlineColourDefaultsBlackPersistsAndStaysPerProfile() {
        CrosshairStudioConfig config = new CrosshairStudioConfig();
        assertEquals(0xFF000000, config.outlineColor);
        assertEquals(0xFF000000, config.enemy.outlineColor);

        config.outlineColor = 0xFFFFFFFF;
        config.enemy.outlineColor = 0xFF2BE8FF;
        config.savedPresets.add(CrosshairStudioConfig.CustomPreset.of("W", config, false));

        Gson gson = new Gson();
        CrosshairStudioConfig loaded = gson.fromJson(gson.toJson(config), CrosshairStudioConfig.class);
        loaded.normalize();
        assertEquals(0xFFFFFFFF, loaded.outlineColor);
        assertEquals(0xFF2BE8FF, loaded.enemy.outlineColor);
        assertEquals(0xFFFFFFFF, loaded.savedPresets.getFirst().outlineColor);

        loaded.savedPresets.getFirst().applyLookTo(loaded.enemy);
        assertEquals(0xFFFFFFFF, loaded.enemy.outlineColor, "preset carries outline colour");
        loaded.enemy.copyProfileFrom(CrosshairProfile.enemyDefault());
        assertEquals(0xFF000000, loaded.enemy.outlineColor, "enemy reset restores black");
        assertEquals(0xFFFFFFFF, loaded.outlineColor, "primary untouched");

        loaded.resetProfile();
        assertEquals(0xFF000000, loaded.outlineColor);
    }

    @Test
    void outlineColourFromOldConfigIsBlackAndAlwaysOpaque() {
        String old = "{\"configVersion\":5,\"outline\":true,\"enemy\":{\"outline\":true},"
                + "\"savedPresets\":[{\"name\":\"A\",\"outline\":true}]}";
        CrosshairStudioConfig loaded = new Gson().fromJson(old, CrosshairStudioConfig.class);
        loaded.normalize();
        assertEquals(0xFF000000, loaded.outlineColor);
        assertEquals(0xFF000000, loaded.enemy.outlineColor);
        assertEquals(0xFF000000, loaded.savedPresets.getFirst().outlineColor);

        loaded.outlineColor = 0x00FF0000;
        loaded.normalize();
        assertEquals(0xFFFF0000, loaded.outlineColor, "a transparent outline is made opaque");
    }}
