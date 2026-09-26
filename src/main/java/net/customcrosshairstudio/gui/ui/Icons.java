package net.customcrosshairstudio.gui.ui;

import net.customcrosshairstudio.config.CrosshairStudioConfig.StyleType;
import net.minecraft.client.gui.DrawContext;

/** 7x7 pixel icons, drawn as merged horizontal runs so they stay crisp at every GUI scale. */
public final class Icons {
    public static final int SIZE = 7;

    public static final String[] CLASSIC = {
            "...#...",
            "...#...",
            ".......",
            "##...##",
            ".......",
            "...#...",
            "...#..."};
    public static final String[] DOT = {
            ".......",
            ".......",
            "..###..",
            "..###..",
            "..###..",
            ".......",
            "......."};
    public static final String[] INVERTED = {
            "..###..",
            ".####.#",
            "####..#",
            "####..#",
            "####..#",
            ".####.#",
            "..###.."};
    public static final String[] DRAWN = {
            "#.#.#.#",
            ".......",
            "#..#..#",
            "..###..",
            "#..#..#",
            ".......",
            "#.#.#.#"};
    public static final String[] STYLE = {
            "###.###",
            "###.###",
            "###.###",
            ".......",
            "###.###",
            "###.###",
            "###.###"};
    public static final String[] SHAPE = {
            ".#...#.",
            ".#...#.",
            "###..#.",
            ".#..###",
            ".#...#.",
            ".#...#.",
            "......."};
    public static final String[] PEN = {
            ".....##",
            "....###",
            "...###.",
            "..###..",
            ".###...",
            "#.#....",
            "##....."};
    public static final String[] ERASER = {
            "....#..",
            "...###.",
            "..#####",
            ".#.###.",
            "#...#..",
            ".#.#...",
            "..#...."};
    public static final String[] COLOR = {
            "...#...",
            "..###..",
            ".#####.",
            "#######",
            "#######",
            ".#####.",
            "..###.."};
    public static final String[] PRESETS = {
            ".#####.",
            ".......",
            "#######",
            ".......",
            "#######",
            "#.....#",
            "#######"};
    public static final String[] BEHAVIOR = {
            ".......",
            "..###..",
            ".#...#.",
            "#..#..#",
            ".#...#.",
            "..###..",
            "......."};
    public static final String[] UNDO = {
            "..#....",
            ".##....",
            "######.",
            ".##...#",
            "..#...#",
            "......#",
            "..####."};
    public static final String[] TRASH = {
            "..###..",
            "#######",
            ".#...#.",
            ".#.#.#.",
            ".#.#.#.",
            ".#...#.",
            "..###.."};
    public static final String[] CLOSE = {
            "#.....#",
            ".#...#.",
            "..#.#..",
            "...#...",
            "..#.#..",
            ".#...#.",
            "#.....#"};
    public static final String[] PLUS = {
            "...#...",
            "...#...",
            "...#...",
            "#######",
            "...#...",
            "...#...",
            "...#..."};
    public static final String[] CHEVRON_RIGHT = {
            ".......",
            "..#....",
            "..##...",
            "..###..",
            "..##...",
            "..#....",
            "......."};
    public static final String[] CHEVRON_DOWN = {
            ".......",
            ".......",
            ".#####.",
            "..###..",
            "...#...",
            ".......",
            "......."};
    public static final String[] MARK = {
            ".......",
            "...#...",
            "...#...",
            ".#####.",
            "...#...",
            "...#...",
            "......."};

    private Icons() {}

    public static String[] forStyle(StyleType type) {
        return switch (type) {
            case CLASSIC -> CLASSIC;
            case DOT -> DOT;
            case INVERTED -> INVERTED;
            case DRAWN -> DRAWN;
        };
    }

    public static void draw(DrawContext c, String[] mask, int x, int y, int colour) {
        draw(c, mask, x, y, colour, false);
    }

    /** Draws {@code mask} with its top-left at (x, y); {@code mirror} flips it horizontally. */
    public static void draw(DrawContext c, String[] mask, int x, int y, int colour, boolean mirror) {
        for (int row = 0; row < mask.length; row++) {
            String line = mask[row];
            int col = 0;
            while (col < line.length()) {
                int src = mirror ? line.length() - 1 - col : col;
                if (line.charAt(src) != '#') { col++; continue; }
                int start = col;
                while (col < line.length() && line.charAt(mirror ? line.length() - 1 - col : col) == '#') col++;
                c.fill(x + start, y + row, x + col, y + row + 1, colour);
            }
        }
    }
}
