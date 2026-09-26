package net.customcrosshairstudio.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Pixel pattern for the Drawn style: SIZE x SIZE cells stored as rows of '#' (on) and '.' (off).
 * SIZE is odd so the centre cell sits on the crosshair anchor pixel.
 */
public final class DrawnPattern {
    public static final int SIZE = 15;
    public static final int CENTER = SIZE / 2;
    private static final char ON = '#';
    private static final char OFF = '.';

    private DrawnPattern() {}

    public static List<String> empty() {
        List<String> rows = new ArrayList<>(SIZE);
        for (int y = 0; y < SIZE; y++) rows.add(String.valueOf(OFF).repeat(SIZE));
        return rows;
    }

    /** Small plus: centre pixel with 2px arms and no gap. */
    public static List<String> defaultRows() {
        List<String> rows = empty();
        for (int i = -2; i <= 2; i++) {
            rows = with(rows, CENTER + i, CENTER, true);
            rows = with(rows, CENTER, CENTER + i, true);
        }
        return rows;
    }

    /** Returns a well-formed SIZE x SIZE pattern; malformed rows/characters become off cells. */
    public static List<String> normalize(List<String> rows) {
        List<String> out = new ArrayList<>(SIZE);
        for (int y = 0; y < SIZE; y++) {
            String row = rows != null && y < rows.size() ? rows.get(y) : null;
            StringBuilder sb = new StringBuilder(SIZE);
            for (int x = 0; x < SIZE; x++) {
                sb.append(row != null && x < row.length() && row.charAt(x) == ON ? ON : OFF);
            }
            out.add(sb.toString());
        }
        return out;
    }

    public static boolean isSet(List<String> rows, int x, int y) {
        if (rows == null || x < 0 || y < 0 || x >= SIZE || y >= SIZE || y >= rows.size()) return false;
        String row = rows.get(y);
        return x < row.length() && row.charAt(x) == ON;
    }

    /** Returns a copy of {@code rows} with cell (x, y) set to {@code on}. */
    public static List<String> with(List<String> rows, int x, int y, boolean on) {
        List<String> out = normalize(rows);
        if (x < 0 || y < 0 || x >= SIZE || y >= SIZE) return out;
        char[] chars = out.get(y).toCharArray();
        chars[x] = on ? ON : OFF;
        out.set(y, new String(chars));
        return out;
    }

    public static boolean isEmpty(List<String> rows) {
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if (isSet(rows, x, y)) return false;
            }
        }
        return true;
    }
}
