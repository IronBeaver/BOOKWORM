package com.bookworm.reader;
import java.io.*;
import java.util.*;

/** One-time asset preparation: extract the license verbatim as readable text; EPUB bytes stay unchanged. */
public final class PrepareBundledBook {
    public static void main(String[] args) throws Exception {
        File root = new File("app/build/bundle-inspection-" + UUID.randomUUID()); root.mkdirs();
        try {
            EpubBook book;
            try (InputStream in = new FileInputStream("app/src/main/assets/books/frankenstein.epub")) { book = EpubParser.importBook(in, root); }
            System.out.println(book.id + "; " + book.title + "; " + book.author + "; " + book.chapters.size() + " chapters; " + book.tocLabels);
            StringBuilder all = new StringBuilder();
            for (String path : book.chapters) {
                org.w3c.dom.Document doc = EpubParser.xml(EpubParser.read(book.content(path), EpubParser.MAX_XML));
                all.append(EpubParser.elements(doc, "body").get(0).getTextContent()).append('\n');
                EpubHtml.chapter(book, path, "");
            }
            String text = all.toString();
            int noticeStart = text.indexOf("The Project Gutenberg eBook"), noticeEnd = text.indexOf("*** START OF THE PROJECT GUTENBERG");
            if (noticeStart < 0 || noticeEnd < noticeStart) throw new AssertionError("Original Gutenberg notice not found");
            try (Writer out = new OutputStreamWriter(new FileOutputStream("app/src/main/assets/books/gutenberg-notice.txt"), "UTF-8")) {
                out.write(text.substring(noticeStart, noticeEnd).trim());
                out.write("\n\nBOOKWORM includes this unmodified EPUB at no charge. Source: https://www.gutenberg.org/ebooks/84\nNo affiliation with or endorsement by Project Gutenberg is claimed.\nThe full Project Gutenberg license is available offline using the Full license button and inside the EPUB.\n");
            }
            int start = text.indexOf("THE FULL PROJECT GUTENBERG");
            if (start < 0 || !text.substring(start).replaceAll("\\s+", " ").contains("Section 5")) throw new AssertionError("Full Gutenberg license not found");
            try (Writer out = new OutputStreamWriter(new FileOutputStream("app/src/main/assets/books/gutenberg-license.txt"), "UTF-8")) { out.write(text.substring(start).trim()); }
            System.out.println(text.substring(0, Math.min(2200, text.length())));
        } finally { EpubParser.deleteTree(root); }
    }
}
