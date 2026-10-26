package com.bookworm.reader;
import java.io.*;
import java.util.Properties;

/** Logical seed marker survives asset updates; SHA-256 import also closes the crash-before-marker window. */
public final class LibrarySeeder {
    public interface Source { InputStream open() throws IOException; }
    private LibrarySeeder() { }
    public static synchronized EpubBook ensure(File root, String key, String expectedId, Source source) throws IOException {
        if (!key.matches("[a-z0-9-]+") || !expectedId.matches("[0-9a-f]{64}")) throw new IOException("Invalid seed identity");
        File marker = new File(root, "seed-" + key + ".properties");
        String previous = "";
        try { previous = LibraryFiles.read(marker).getProperty("id", ""); } catch (IOException ignored) { }
        if (previous.matches("[0-9a-f]{64}") && new File(root, previous).exists()) return EpubBook.load(new File(root, previous));
        EpubBook book;
        File existing = new File(root, expectedId);
        if (existing.exists()) book = EpubBook.load(existing);
        else {
            try (InputStream in = source.open()) { book = EpubParser.importBook(in, root); }
            if (!book.id.equals(expectedId)) throw new IOException("Bundled EPUB checksum differs from the packaged identity");
        }
        Properties p = new Properties(); p.setProperty("id", book.id); LibraryFiles.write(marker, p); return book;
    }
}
