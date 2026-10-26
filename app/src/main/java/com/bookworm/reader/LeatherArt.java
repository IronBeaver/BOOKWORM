package com.bookworm.reader;

/** One procedural design in a 200x300 coordinate space; no texture assets or stored bitmaps. */
public final class LeatherArt {
    public interface Surface extends CoverTypography.Metrics {
        void background(int color);
        void fill(int color, float x, float y, float width, float height);
        void line(int color, float stroke, float x1, float y1, float x2, float y2);
        void curve(int color, float stroke, float x, float y, float a, float b, float c, float d, float ex, float ey);
        void text(CoverTypography.Layout layout, float x, float y, float width, float height);
    }
    private LeatherArt() { }
    public static int shade(int color, int amount) {
        int r = Math.max(0, Math.min(255, ((color >> 16) & 255) + amount));
        int g = Math.max(0, Math.min(255, ((color >> 8) & 255) + amount));
        int b = Math.max(0, Math.min(255, (color & 255) + amount));
        return 0xff000000 | r << 16 | g << 8 | b;
    }
    public static void draw(Surface s, String id, String title, String author, int color) {
        s.background(color);
        java.util.Random grain = new java.util.Random((id == null ? "" : id).hashCode());
        for (int i = 0; i < 1150; i++) {
            float x = grain.nextFloat() * 200, y = grain.nextFloat() * 300;
            s.fill(i % 2 == 0 ? 0x10000000 : 0x08ffffff, x, y, .35f + grain.nextFloat() * .65f, .35f);
        }
        for (int x = 0; x < 21; x++) s.fill((int) (58 * (1 - x / 21f)) << 24, x, 0, 1, 300);
        s.line(0x70000000, 1.2f, 18, 2, 18, 298);
        s.line(0x40f5dfaa, .7f, 21, 2, 21, 298);
        border(s, 29, 14, 188, 286, 0xb8dfbc75, .85f);
        border(s, 33, 18, 184, 282, 0x72dfbc75, .5f);
        corner(s, 38, 24, 1, 1); corner(s, 179, 24, -1, 1);
        corner(s, 38, 276, 1, -1); corner(s, 179, 276, -1, -1);
        CoverTypography.Layout heading = CoverTypography.fit(title, "Untitled", 128, 119, 9, 27, s);
        s.text(heading, 44, 64, 128, 119);
        s.line(0x90dfbc75, .6f, 72, 204, 98, 204); s.line(0x90dfbc75, .6f, 118, 204, 144, 204);
        diamond(s, 108, 204, 4, CoverChoice.GOLD);
        CoverTypography.Layout byline = CoverTypography.fit(author, "Unknown author", 128, 39, 8, 12, s);
        s.text(byline, 44, 219, 128, 39);
        s.line(0x70dfbc75, .6f, 92, 267, 124, 267);
    }
    private static void border(Surface s, float x, float y, float r, float b, int color, float stroke) {
        s.line(color, stroke, x, y, r, y); s.line(color, stroke, r, y, r, b);
        s.line(color, stroke, r, b, x, b); s.line(color, stroke, x, b, x, y);
    }
    private static void corner(Surface s, float x, float y, int dx, int dy) {
        int gold = 0xc8dfbc75;
        s.curve(gold, .8f, x, y + 19 * dy, x + 2 * dx, y + 6 * dy, x + 11 * dx, y + 12 * dy, x + 18 * dx, y);
        s.curve(gold, .7f, x + 3 * dx, y + 14 * dy, x + 11 * dx, y + 15 * dy, x + 12 * dx, y + 4 * dy, x + 7 * dx, y + 4 * dy);
        diamond(s, x + 3 * dx, y + 3 * dy, 1.7f, gold);
    }
    private static void diamond(Surface s, float x, float y, float radius, int color) {
        s.line(color, .8f, x, y - radius, x + radius, y); s.line(color, .8f, x + radius, y, x, y + radius);
        s.line(color, .8f, x, y + radius, x - radius, y); s.line(color, .8f, x - radius, y, x, y - radius);
    }
}
