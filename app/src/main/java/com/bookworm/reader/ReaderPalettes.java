package com.bookworm.reader;

/** Reader-only opaque colours. Follow Appearance intentionally adds no CSS overrides. */
public final class ReaderPalettes {
    public static final String FOLLOW = "appearance";
    private static final String[] IDS = {FOLLOW, "warm_paper", "soft_blue", "blue_night", "soft_dark"};
    private static final String[] LABELS = {"Follow Appearance", "Warm Paper", "Soft Blue", "Blue Night", "Soft Dark"};
    private static final int[] BACKGROUNDS = {0, 0xfff5ebd4, 0xffdceaf2, 0xff183f73, 0xff252a32};
    private static final int[] TEXT = {0, 0xff30291f, 0xff213b54, 0xffffffff, 0xffe8e8e3};
    private static final int[] ACCENTS = {0, 0xff795322, 0xff245478, 0xffffe0a0, 0xffb9d8ff};
    public static int count() { return IDS.length; }
    public static String id(int i) { return IDS[i]; }
    public static String label(int i) { return LABELS[i]; }
    public static int index(String id) { for (int i = 0; i < IDS.length; i++) if (IDS[i].equals(id)) return i; return 0; }
    public static String selected(String id) { return IDS[index(id)]; }
    public static ThemeSettings resolve(String id, ThemeSettings appearance) {
        int i = index(id);
        return i == 0 ? appearance : new ThemeSettings(BACKGROUNDS[i], TEXT[i], ACCENTS[i], appearance.serif, appearance.fontSize, appearance.classic);
    }
    public static String selectionCss(String id, ThemeSettings palette) {
        return index(id) == 0 ? "" : "::selection{background:" + ThemeSettings.hex(palette.accent)
            + "!important;color:" + ThemeSettings.hex(palette.background) + "!important;}";
    }
    public static int systemBar(String id, ThemeSettings palette) {
        // API 22 cannot request dark system-bar icons. Keep white icons legible.
        return index(id) == 0 || ThemeSettings.contrast(0xffffffff, palette.background) >= 4.5
                ? palette.background : palette.text;
    }
    private ReaderPalettes() { }
}
