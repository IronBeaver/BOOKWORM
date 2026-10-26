package com.bookworm.reader;

/** Dependency-free checks: run with the JDK, not an Android mock runtime. */
public final class ModelTests {
    private static int checks;
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        for (int i = 0; i < 3; i++) {
            ThemeSettings p = ThemeSettings.preset(i, false, 22, true);
            check(p.readable(), "Preset contrast " + i);
            check(!p.serif && p.classic && p.fontSize == 22, "Preset preserves other preferences");
            check(ThemeSettings.parseHex(ThemeSettings.hex(p.background)) == p.background, "Colour roundtrip");
        }
        check(ThemeSettings.parseHex(" #aAbBcC ") == 0xffaabbcc, "Mixed case and whitespace");
        check(ThemeSettings.parseHex("123456") == 0xff123456, "Optional hash");
        for (String bad : new String[]{"", "#fff", "#12345678", "#GG0000", "12345!"}) {
            boolean rejected = false;
            try { ThemeSettings.parseHex(bad); } catch (IllegalArgumentException expected) { rejected = true; }
            check(rejected, "Reject " + bad);
        }
        check(!new ThemeSettings(0, 0, 0, true, 18, false).readable(), "Unreadable theme rejected");
        check(new ThemeSettings(0, -1, -1, true, 1, false).fontSize == 14, "Lower size bound");
        check(new ThemeSettings(0, -1, -1, true, 99, false).fontSize == 24, "Upper size bound");
        check(Math.abs(ThemeSettings.contrast(0xff000000, 0xffffffff) - 21) < 0.001, "Black white contrast");
        check(DemoBooks.count() == 6, "Six demo books");
        java.util.HashSet<String> titles = new java.util.HashSet<>();
        for (int i = 0; i < DemoBooks.count(); i++) {
            DemoBooks.Book b = DemoBooks.get(i);
            check(titles.add(b.title), "Unique title");
            check(!b.author.isEmpty() && !b.description.isEmpty(), "Book metadata");
        }
        System.out.println("Passed " + checks + " model checks.");
    }
}
