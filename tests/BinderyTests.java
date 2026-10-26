package com.bookworm.reader;

import java.io.*;
import java.util.*;

public final class BinderyTests {
    private static int checks;
    private static void check(boolean value, String label) { checks++; if (!value) throw new AssertionError(label); }
    private static String key(CoverChoice choice, CoverChoice.Policy global, String title, String author, String source, int w, int h) {
        return CoverChoice.cacheKey("book-id", title, author, source, global, choice, w, h);
    }
    public static void main(String[] args) throws Exception {
        File root = new File("app/build/bindery-tests-" + UUID.randomUUID()); root.mkdirs();
        try {
            boolean[][] original = {{true, true, false}, {true, true, false}, {false, true, false}};
            for (CoverChoice.Policy global : CoverChoice.Policy.values()) {
                check(CoverChoice.Policy.from(global.name()) == global, "Global preference key roundtrip " + global);
                for (CoverChoice.Override override : CoverChoice.Override.values()) {
                    CoverChoice value = new CoverChoice(override, -1);
                    check(value.useOriginal(global, true) == original[global.ordinal()][override.ordinal()], "Usable artwork selection " + global + "/" + override);
                    check(!value.useOriginal(global, false), "Every unavailable artwork path falls back to leather");
                }
            }
            check(CoverChoice.Policy.from("unknown") == CoverChoice.Policy.AUTO, "Old/missing global preference defaults to AUTO");
            check(CoverChoice.Policy.from(null) == CoverChoice.Policy.AUTO, "Null global fallback");
            Set<Integer> colors = new HashSet<>();
            for (int i = 0; i < 100; i++) {
                String id = String.format(java.util.Locale.US, "%064x", i);
                int color = CoverChoice.identityColor(id); colors.add(color);
                check(color >= 0 && color < 6 && color == CoverChoice.identityColor(new String(id)), "Deterministic identity colour in palette");
            }
            check(colors.size() == 6, "Stable defaults use all six colours");
            check(CoverChoice.identityColor("polygenelubricants") >= 0, "MIN_VALUE hash does not overflow palette");
            for (int color : CoverChoice.COLORS) check(ThemeSettings.contrast(CoverChoice.GOLD, LeatherArt.shade(color, 8)) >= 4.5, "Readable gold on brightest leather gradient");
            check(CoverChoice.load(root).override == CoverChoice.Override.GLOBAL && CoverChoice.load(root).color == -1, "Existing library without cover sidecar remains compatible");
            for (CoverChoice.Override override : CoverChoice.Override.values()) for (int color = -1; color < 6; color++) {
                CoverChoice saved = new CoverChoice(override, color); saved.save(root); CoverChoice restored = CoverChoice.load(root);
                check(restored.override == override && restored.color == color, "Per-book policy/colour survive file reload");
            }
            CoverChoice.defaults().save(root);
            check(CoverChoice.load(root).override == CoverChoice.Override.GLOBAL && CoverChoice.load(root).color == -1, "Reset restores global policy and identity colour");
            Properties corrupt = new Properties(); corrupt.setProperty("policy", "unknown"); corrupt.setProperty("color", "999"); LibraryFiles.write(new File(root, "cover.properties"), corrupt);
            check(CoverChoice.load(root).override == CoverChoice.Override.GLOBAL && CoverChoice.load(root).color == -1, "Invalid cover record falls back safely");
            CoverChoice base = CoverChoice.defaults(); String baseline = key(base, CoverChoice.Policy.AUTO, "Title", "Author", "file:1:20", 160, 240);
            check(baseline.equals(key(base, CoverChoice.Policy.AUTO, "Title", "Author", "file:1:20", 160, 240)), "Stable render cache key");
            check(!baseline.equals(key(base, CoverChoice.Policy.LEATHER, "Title", "Author", "file:1:20", 160, 240)), "Global policy invalidates cache");
            check(!baseline.equals(key(new CoverChoice(CoverChoice.Override.LEATHER, -1), CoverChoice.Policy.AUTO, "Title", "Author", "file:1:20", 160, 240)), "Per-book policy invalidates cache");
            check(!baseline.equals(key(new CoverChoice(CoverChoice.Override.GLOBAL, (base.colorFor("book-id") + 1) % 6), CoverChoice.Policy.AUTO, "Title", "Author", "file:1:20", 160, 240)), "Colour invalidates cache");
            check(!baseline.equals(key(base, CoverChoice.Policy.AUTO, "New title", "Author", "file:1:20", 160, 240)), "Title invalidates cache");
            check(!baseline.equals(key(base, CoverChoice.Policy.AUTO, "Title", "New author", "file:1:20", 160, 240)), "Author invalidates cache");
            check(!baseline.equals(key(base, CoverChoice.Policy.AUTO, "Title", "Author", "file:2:21", 160, 240)), "Original artwork revision invalidates cache");
            check(!baseline.equals(key(base, CoverChoice.Policy.AUTO, "Title", "Author", "file:1:20", 200, 300)), "Required resolution invalidates cache");
            BinderyPreview metrics = new BinderyPreview(200, 300);
            String[] titles = {"Frankenstein; or, The Modern Prometheus", "An unusually long title with many ordinary words about a quiet library and its ancient collection of volumes", "SupercalifragilisticexpialidociousWithoutAnySpacesAtAll", "Été, lumière et mémoire — collected letters", "星の王子さま 日本語の長い題名", "العربية: حكايات المكتبة", "Cafe\u0301 and 📚 stories", "", String.join("", Collections.nCopies(500, "Long "))};
            for (String title : titles) {
                CoverTypography.Layout layout = CoverTypography.fit(title, "Untitled", 128, 119, 9, 27, metrics);
                check(!layout.lines.isEmpty() && layout.size >= 9 && layout.size <= 27, "Title chooses bounded font size");
                check(layout.lines.size() * layout.lineHeight <= 119.1f, "Wrapped title fits vertical safe box");
                for (String line : layout.lines) check(metrics.width(line, layout.size) <= 128.1f, "Wrapped title fits horizontal safe box");
                for (String line : layout.lines) check(line.isEmpty() || !Character.isHighSurrogate(line.charAt(line.length() - 1)), "No split surrogate at line ending");
            }
            check(!CoverTypography.fit(titles[0], "Untitled", 128, 119, 9, 27, metrics).truncated, "Actual Frankenstein title fits without elision");
            CoverTypography.Layout words = CoverTypography.fit(titles[0], "Untitled", 128, 119, 9, 27, metrics);
            check(String.join(" ", words.lines).equals(titles[0]), "Wrapping preserves all words and their separators");
            check(CoverTypography.fit(null, "Unknown author", 128, 39, 8, 12, metrics).lines.get(0).contains("Unknown"), "Missing author handled gracefully");
            metrics.canvas.dispose();
            Map<String, byte[]> fixture = EpubTests.fixture(true); EpubBook book = EpubParser.importBook(new ByteArrayInputStream(EpubTests.archive(fixture)), new File(root, "library"));
            new ReadingPosition(book.chapters.get(1), 178).save(book.directory);
            byte[] epub = EpubParser.read(new File(book.directory, "original.epub"), EpubParser.MAX_ENTRY);
            byte[] record = EpubParser.read(new File(book.directory, "book.properties"), EpubParser.MAX_XML);
            byte[] position = EpubParser.read(new File(book.directory, "position.properties"), 4096);
            new CoverChoice(CoverChoice.Override.LEATHER, 3).save(book.directory); CoverChoice.defaults().save(book.directory);
            check(Arrays.equals(epub, EpubParser.read(new File(book.directory, "original.epub"), EpubParser.MAX_ENTRY)), "Cover edits never alter EPUB contents");
            check(Arrays.equals(record, EpubParser.read(new File(book.directory, "book.properties"), EpubParser.MAX_XML)), "Cover edits never alter book metadata/identity");
            check(Arrays.equals(position, EpubParser.read(new File(book.directory, "position.properties"), 4096)), "Cover edits never alter reading progress");
            System.out.println("Passed " + checks + " bindery policy, colour, persistence, cache-key and title-layout checks.");
        } finally { EpubParser.deleteTree(root); }
    }
}
