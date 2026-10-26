package com.bookworm.reader;
import java.io.*;
import java.util.*;

/** Host integration checks exercise actual file formats/importer, not Android framework mocks. */
public final class LibraryTests {
    private static int checks;
    private static final String BUNDLE = "eb696684c1314092bfe973be1241ab1f44cde101ac19bc36a574893de24016c2";
    private static void check(boolean value, String message) { checks++; if (!value) throw new AssertionError(message); }
    private static byte[] bytes(File file) throws IOException { return EpubParser.read(file, EpubParser.MAX_ENTRY); }
    private static EpubBook add(File root, String title, String author) throws Exception {
        Map<String, byte[]> data = EpubTests.fixture(true);
        String opf = new String(data.get("OPS/book.opf"), "UTF-8").replace("Local Test Book", title).replace("Ada Test", author);
        data.put("OPS/book.opf", opf.getBytes("UTF-8"));
        return EpubParser.importBook(new ByteArrayInputStream(EpubTests.archive(data)), root);
    }
    private static EpubBook sample(String id, String title, String author, long added, long read) {
        EpubBook b = new EpubBook(); b.id = id; b.title = title; b.author = author; b.addedAt = added; b.lastRead = read; return b;
    }
    private static void first(List<EpubBook> books, LibrarySort sort, String id, String message) { check(sort.ordered(books).get(0).id.equals(id), message); }
    public static void main(String[] args) throws Exception {
        File root = new File("app/build/library-tests-" + UUID.randomUUID()); root.mkdirs();
        try {
            File library = new File(root, "library"); library.mkdirs();
            check(LibraryCatalog.scan(library).books.isEmpty(), "Empty library has no demos");
            check(new LibraryRows(Collections.emptyList(), LibrarySort.TITLE_ASC).rowCount() == 0, "Empty shelf has zero rows");
            EpubBook one = add(library, "Alpha", "Zora");
            LibraryCatalog.Snapshot snapshot = LibraryCatalog.scan(library);
            LibraryRows single = new LibraryRows(snapshot.books, LibrarySort.TITLE_ASC);
            check(single.bookCount() == 1 && single.rowCount() == 1 && single.at(0, 0).id.equals(one.id), "One imported book has a real openable row");
            check(single.at(0, 1) == null, "Odd final row has no fictitious book");
            check(single.at(0, 0).content(single.at(0, 0).chapters.get(0)).isFile(), "Shelf identity resolves to reader content");
            EpubBook two = add(library, "Beta", "Ada"), three = add(library, "Gamma", "Mira");
            snapshot = LibraryCatalog.scan(library); LibraryRows refreshed = new LibraryRows(snapshot.books, LibrarySort.TITLE_ASC);
            check(refreshed.bookCount() == 3 && refreshed.rowCount() == 2, "Import refresh produces new real row count");
            Set<String> visible = new HashSet<>();
            for (int row = 0; row < refreshed.rowCount(); row++) for (int col = 0; col < 2; col++) {
                EpubBook b = refreshed.at(row, col); if (b != null) visible.add(b.id);
            }
            check(visible.equals(new HashSet<>(Arrays.asList(one.id, two.id, three.id))), "All odd/even real books exposed exactly once");
            check(single.bookCount() == 1, "Published snapshots cannot be changed behind row access");
            check(LibraryCatalog.scan(library).books.size() == 3, "Cold reopen preserves imported library");
            new ReadingPosition(one.chapters.get(1), 234).save(one.directory);
            File metadata = new File(one.directory, "book.properties"); Properties legacy = new Properties();
            try (InputStream in = new FileInputStream(metadata)) { legacy.load(in); }
            legacy.remove("addedAt");
            try (OutputStream out = new FileOutputStream(metadata)) { legacy.store(out, "Version 4 format"); }
            check(new File(one.directory, "original.epub").setLastModified(1234567890000L), "Legacy date fixture established");
            byte[] oldMetadata = bytes(metadata), oldPosition = bytes(new File(one.directory, "position.properties"));
            EpubBook migrated = EpubBook.load(one.directory);
            check(migrated.id.equals(one.id) && migrated.addedAt == 1234567890000L, "Version 4 date fallback keeps original ID");
            check(migrated.lastRead > 0 && ReadingPosition.load(one.directory, "").offset == 234, "Legacy recent-read fallback retains text position");
            for (LibrarySort sort : LibrarySort.values()) {
                LibraryRows sorted = new LibraryRows(LibraryCatalog.scan(library).books, sort);
                check(sorted.bookCount() == 3 && sorted.rowCount() == 2, "Sorting keeps every card: " + sort);
                check(LibrarySort.fromKey(sort.name()) == sort, "Persisted sort key restores: " + sort);
            }
            check(Arrays.equals(oldMetadata, bytes(metadata)) && Arrays.equals(oldPosition, bytes(new File(one.directory, "position.properties"))), "Migration/scan/sorting do not rewrite legacy metadata or progress");
            long readAt = System.currentTimeMillis() + 60000; LibraryFiles.markRead(one.directory, readAt);
            check(EpubBook.load(one.directory).lastRead == readAt, "Opening updates durable recent-read time");
            check(Arrays.equals(oldPosition, bytes(new File(one.directory, "position.properties"))), "Recent-read sidecar does not change reading offset");
            List<EpubBook> order = Arrays.asList(sample("b", "beta", "Ada", 20, 0), sample("a", "Alpha", "Zora", 10, 90), sample("c", "Gamma", "Mira", 30, 80));
            first(order, LibrarySort.TITLE_ASC, "a", "Title A-Z"); first(order, LibrarySort.TITLE_DESC, "c", "Title Z-A");
            first(order, LibrarySort.AUTHOR_ASC, "b", "Author A-Z"); first(order, LibrarySort.AUTHOR_DESC, "a", "Author Z-A");
            first(order, LibrarySort.ADDED_NEWEST, "c", "Newest first"); first(order, LibrarySort.ADDED_OLDEST, "a", "Oldest first"); first(order, LibrarySort.RECENT, "a", "Recently read first");
            for (LibrarySort sort : LibrarySort.values()) {
                List<EpubBook> ties = Arrays.asList(sample("z", "Same", "Same", 10, 20), sample("a", "same", "same", 10, 20));
                first(ties, sort, "a", "Equal keys use stable ID: " + sort);
                Collections.reverse(ties); first(ties, sort, "a", "Tie order independent of enumeration: " + sort);
            }
            check(LibrarySort.fromKey("obsolete-key") == LibrarySort.TITLE_ASC, "Unknown sort preference falls back safely");
            File bundleAsset = new File("app/src/main/assets/books/frankenstein.epub");
            int[] opened = {0};
            LibrarySeeder.Source source = () -> { opened[0]++; return new FileInputStream(bundleAsset); };
            File firstRun = new File(root, "first-run");
            LibrarySeeder.ensure(firstRun, "frankenstein-gutenberg-84", BUNDLE, () -> new FileInputStream(bundleAsset));
            check(LibraryCatalog.scan(firstRun).books.size() == 1 && LibraryCatalog.scan(firstRun).books.get(0).id.equals(BUNDLE), "First launch creates an absent library containing exactly Frankenstein");
            EpubBook included = LibrarySeeder.ensure(library, "frankenstein-gutenberg-84", BUNDLE, source);
            check(included.id.equals(BUNDLE) && opened[0] == 1 && LibraryCatalog.scan(library).books.size() == 4, "First seed adds bundled book and preserves existing library");
            new ReadingPosition(included.chapters.get(4), 333).save(included.directory);
            byte[] bundledPosition = bytes(new File(included.directory, "position.properties"));
            LibrarySeeder.Source never = () -> { throw new IOException("Asset must not reopen"); };
            LibrarySeeder.ensure(library, "frankenstein-gutenberg-84", BUNDLE, never);
            check(LibraryCatalog.scan(library).books.size() == 4, "Repeated launches do not duplicate bundled book");
            LibrarySeeder.ensure(library, "frankenstein-gutenberg-84", String.join("", Collections.nCopies(64, "0")), never);
            check(Arrays.equals(bundledPosition, bytes(new File(included.directory, "position.properties"))), "Logical seed survives asset update without replacing progress");
            File recovered = new File(root, "recovered"); recovered.mkdirs(); EpubBook preexisting;
            try (InputStream in = new FileInputStream(bundleAsset)) { preexisting = EpubParser.importBook(in, recovered); }
            LibrarySeeder.ensure(recovered, "frankenstein-gutenberg-84", BUNDLE, never);
            check(LibraryCatalog.scan(recovered).books.size() == 1, "Import-before-marker crash/preexisting exact EPUB is not duplicated");
            check(included.title.contains("Frankenstein") && included.author.contains("Shelley") && included.chapters.size() == 31, "Bundled EPUB metadata and full spine");
            check(EpubHtml.chapter(included, included.chapters.get(0), "").contains("<img"), "Bundled first reading section renders its actual raster cover");
            for (int i = 1; i <= 24; i++) check(included.tocLabels.contains("Chapter " + i), "Bundled chapter " + i);
            for (int i = 1; i <= 4; i++) check(included.tocLabels.contains("Letter " + i), "Bundled letter " + i);
            int textLength = 0;
            for (String chapter : included.chapters) textLength += EpubHtml.chapter(included, chapter, "").length();
            check(textLength > 400000, "All bundled reading content sanitizes, not an excerpt");
            check(javax.imageio.ImageIO.read(included.content(included.cover)) != null, "Bundled real cover decodes");
            String license = new String(bytes(new File("app/src/main/assets/books/gutenberg-license.txt")), "UTF-8");
            check(license.contains("THE FULL PROJECT GUTENBERG") && license.replaceAll("\\s+", " ").contains("Section 5"), "Full license is packaged offline");
            String notice = new String(bytes(new File("app/src/main/assets/books/gutenberg-notice.txt")), "UTF-8");
            check(notice.contains("This eBook is for the use") && notice.contains("Mary Wollstonecraft Shelley") && notice.contains("at no charge"), "Attribution and original notice packaged");
            File missing = new File(library, String.join("", Collections.nCopies(64, "f"))); missing.mkdir();
            check(new File(two.directory, "content/" + two.chapters.get(0)).delete(), "Missing content fixture");
            try (OutputStream out = new FileOutputStream(new File(three.directory, "book.properties"))) { out.write("chapters=invalid".getBytes("UTF-8")); }
            snapshot = LibraryCatalog.scan(library);
            check(snapshot.unavailable == 3 && snapshot.books.size() == 2, "Missing/corrupt records reported without crashing healthy shelf");
            check(new File(two.directory, "original.epub").isFile() && Arrays.equals(oldPosition, bytes(new File(one.directory, "position.properties"))), "Unavailable records do not delete imported files or other progress");
            System.out.println("Passed " + checks + " library, migration, sorting and bundled-seed checks.");
        } finally { EpubParser.deleteTree(root); }
    }
}
