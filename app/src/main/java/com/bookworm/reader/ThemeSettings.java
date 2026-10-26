package com.bookworm.reader;

/** Platform-independent appearance model shared by every screen. */
public final class ThemeSettings {
    public final int background, text, accent, fontSize;
    public final boolean serif, classic;
    public ThemeSettings(int background, int text, int accent, boolean serif, int size, boolean classic) {
        this.background = background | 0xff000000;
        this.text = text | 0xff000000;
        this.accent = accent | 0xff000000;
        this.serif = serif;
        this.fontSize = Math.max(14, Math.min(24, size));
        this.classic = classic;
    }
    public static ThemeSettings preset(int index, boolean serif, int size, boolean classic) {
        switch (index) {
            case 1: return new ThemeSettings(0xfff3ead6, 0xff30291f, 0xff795322, serif, size, classic);
            case 2: return new ThemeSettings(0xff14291f, 0xffe8efdf, 0xffadce91, serif, size, classic);
            default: return new ThemeSettings(0xff171a19, 0xffeee7da, 0xffc9aa78, serif, size, classic);
        }
    }
    public static int parseHex(String value) {
        String hex = value.trim();
        if (hex.startsWith("#")) hex = hex.substring(1);
        if (!hex.matches("[0-9a-fA-F]{6}")) throw new IllegalArgumentException("Use six hexadecimal digits, such as #171A19.");
        return (int) Long.parseLong(hex, 16) | 0xff000000;
    }
    public static String hex(int color) {
        return String.format(java.util.Locale.US, "#%06X", color & 0xffffff);
    }
    private static double luminance(int color) {
        double result = 0;
        double[] weights = {0.2126, 0.7152, 0.0722};
        for (int i = 0; i < 3; i++) {
            double c = ((color >> (16 - i * 8)) & 255) / 255.0;
            result += weights[i] * (c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4));
        }
        return result;
    }
    public static double contrast(int first, int second) {
        double a = luminance(first), b = luminance(second);
        return (Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05);
    }
    public boolean readable() {
        return contrast(background, text) >= 4.5 && contrast(background, accent) >= 4.5;
    }
}
