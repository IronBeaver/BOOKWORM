package com.bookworm.reader;

/** UI-only dimensions/colours; no preferences or library records are changed here. */
public final class ShelfPresentation {
    private ShelfPresentation() { }
    public static float titleSize(int interfaceSize) { return Math.max(14, Math.min(17, interfaceSize - 3)); }
    public static float authorSize(int interfaceSize) { return Math.max(12, Math.min(14, interfaceSize - 5)); }
    public static int blend(int background, int foreground, float amount) {
        int color = 0xff000000;
        for (int shift : new int[]{16, 8, 0}) {
            int channel = Math.round(((background >> shift) & 255) * (1 - amount) + ((foreground >> shift) & 255) * amount);
            color |= channel << shift;
        }
        return color;
    }
    public static int muted(ThemeSettings theme) {
        for (float weight = .64f; weight < 1; weight += .04f) {
            int color = blend(theme.background, theme.text, weight);
            if (ThemeSettings.contrast(color, theme.background) >= 4.5) return color;
        }
        return theme.text;
    }
    public static int coverWidth(int available, int preferred, int touchTarget) {
        return Math.max(1, Math.min(preferred, available - touchTarget * 2));
    }
    public static String summary(int count, LibrarySort sort) {
        return count + (count == 1 ? " book" : " books") + "  ·  " + sort.label;
    }
}
