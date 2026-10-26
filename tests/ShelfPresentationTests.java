package com.bookworm.reader;

public final class ShelfPresentationTests {
    private static int checks;
    private static void check(boolean value, String name) { checks++; if (!value) throw new AssertionError(name); }
    public static void main(String[] args) {
        for (int preset = 0; preset < 3; preset++) {
            ThemeSettings theme = ThemeSettings.preset(preset, true, 18, false);
            int muted = ShelfPresentation.muted(theme);
            check(ThemeSettings.contrast(muted, theme.background) >= 4.5, "Muted author/status retains contrast in preset " + preset);
            check(muted != theme.text, "Metadata is visibly secondary in preset " + preset);
        }
        ThemeSettings custom = new ThemeSettings(0xff000000, 0xff777777, 0xffaaaaaa, false, 18, false);
        check(ThemeSettings.contrast(ShelfPresentation.muted(custom), custom.background) >= 4.5, "Custom near-threshold theme does not lose author contrast");
        for (int size = 14; size <= 24; size++) {
            float title = ShelfPresentation.titleSize(size), author = ShelfPresentation.authorSize(size);
            check(title >= 14 && title <= 17, "Restrained title size");
            check(author >= 12 && author <= 14 && author < title, "Readable secondary author size");
        }
        for (int available : new int[]{144, 180, 224, 272, 352}) {
            int cover = ShelfPresentation.coverWidth(available, 140, 48);
            check(cover > 0 && cover <= 140 && cover + 2 * 48 <= available, "Card reserves separate accessible overflow space");
            int left = (available - cover) / 2, right = left + cover;
            check(right <= available - 48, "Overflow does not obscure cover or metadata");
            check(cover * 3 / 2 > cover, "Portrait cover geometry");
        }
        check(ShelfPresentation.coverWidth(272, 140, 48) == 140, "Tablet two-column cover is 140 by 210 dp");
        for (LibrarySort sort : LibrarySort.values()) {
            check(ShelfPresentation.summary(1, sort).equals("1 book  ·  " + sort.label), "Singular count and active sort");
            check(ShelfPresentation.summary(0, sort).equals("0 books  ·  " + sort.label), "Empty count and active sort");
            check(ShelfPresentation.summary(7, sort).equals("7 books  ·  " + sort.label), "Plural count and active sort");
        }
        System.out.println("Passed " + checks + " shelf presentation/contrast/geometry/state checks.");
    }
}
