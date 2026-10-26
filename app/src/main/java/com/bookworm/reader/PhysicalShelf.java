package com.bookworm.reader;

/** Pure, deterministic geometry and selection for the optional physical shelf. */
public final class PhysicalShelf {
    public enum Presentation { GRID, COVERS, SPINES;
        public static Presentation from(String value, boolean classic) {
            try { return valueOf(value); } catch (IllegalArgumentException | NullPointerException bad) {
                return classic ? COVERS : GRID;
            }
        }
    }
    private String selected;
    public String selected() { return selected; }
    public void select(String id) { selected = id; }
    public void dismiss() { selected = null; }
    public boolean isSelected(String id) { return id != null && id.equals(selected); }
    private static int hash(String id) { return (id == null ? "" : id).hashCode() & 0x7fffffff; }
    public static int width(String id) { return 56 + hash(id) % 21; }
    public static int height(String id) { return 210 + (hash(id) / 23) % 49; }
    public static int columns(int availableDp) { return Math.max(1, Math.min(8, availableDp / 84)); }
    public static int rows(int books, int columns) { return (Math.max(0, books) + Math.max(1, columns) - 1) / Math.max(1, columns); }
    public static String cacheKey(String id, String title, String author, int color, int width, int height, float fontScale) {
        // Length prefixes prevent ambiguous concatenation of metadata.
        StringBuilder key = new StringBuilder("spine-1:");
        for (String part : new String[]{id, title, author, Integer.toString(color), width + "x" + height, Float.toString(fontScale)}) {
            String s = part == null ? "" : part; key.append(s.length()).append(':').append(s);
        }
        return key.toString();
    }
    private PhysicalShelf() { }
    public static PhysicalShelf selection() { return new PhysicalShelf(); }
}
