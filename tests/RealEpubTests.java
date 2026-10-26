package com.bookworm.reader;
import java.io.*;
import java.util.*;

/** Optional downloaded samples stay under build/ and are never bundled in the APK. */
public final class RealEpubTests {
    public static void main(String[] args) throws Exception {
        File root = new File("app/build/real-epub-tests-" + UUID.randomUUID()); root.mkdirs();
        try {
            for (String name : new String[]{"alice-epub2.epub", "alice-epub3.epub"}) {
                File source = new File("app/build/test-fixtures", name); if (!source.isFile()) continue;
                EpubBook book;
                try (InputStream in = new FileInputStream(source)) { book = EpubParser.importBook(in, root); }
                if (!book.title.contains("Alice") || book.chapters.size() < 12 || book.tocLinks.isEmpty() || book.cover.isEmpty()) throw new AssertionError("Real EPUB metadata/navigation missing");
                int chars = 0;
                for (String path : book.chapters) chars += EpubHtml.chapter(book, path, "body{font-size:20px;}").length();
                System.out.println(name + ": " + book.title + "; " + book.author + "; " + book.chapters.size() + " spine entries, " + book.tocLinks.size() + " TOC links, cover " + book.cover + "; all chapters sanitized (" + chars + " characters).");
            }
        } finally { EpubParser.deleteTree(root); }
    }
}
