package com.bookworm.reader;
import java.io.*;
import java.util.*;

/** Read-only legacy-compatible scan. Unavailable records are counted, never deleted. */
public final class LibraryCatalog {
    public static final class Snapshot {
        public final List<EpubBook> books;
        public final int unavailable;
        Snapshot(List<EpubBook> books, int unavailable) {
            this.books = Collections.unmodifiableList(books); this.unavailable = unavailable;
        }
    }
    private LibraryCatalog() { }
    public static Snapshot scan(File root) {
        List<EpubBook> books = new ArrayList<>(); int unavailable = 0;
        File[] directories = root.listFiles();
        if (directories == null) return new Snapshot(books, root.exists() ? 1 : 0);
        for (File directory : directories) {
            if (!directory.getName().matches("[0-9a-f]{64}")) continue;
            try { books.add(EpubBook.load(directory)); }
            catch (IOException | RuntimeException invalid) { unavailable++; }
        }
        return new Snapshot(books, unavailable);
    }
}
