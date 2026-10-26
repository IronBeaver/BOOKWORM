package com.bookworm.reader;

import java.io.*;
import java.util.*;

/** Small metadata record; chapter bodies stay on disk. */
public final class EpubBook {
    public String id, title = "Untitled", author = "Unknown author", cover = "";
    public long addedAt, lastRead;
    public final List<String> chapters = new ArrayList<>();
    public final List<String> tocLabels = new ArrayList<>(), tocLinks = new ArrayList<>();
    public File directory;
    public void save() throws IOException {
        Properties p = new Properties();
        p.setProperty("id", id); p.setProperty("title", title); p.setProperty("author", author);
        p.setProperty("cover", cover); p.setProperty("chapters", "" + chapters.size());
        if (addedAt <= 0) addedAt = System.currentTimeMillis();
        p.setProperty("addedAt", Long.toString(addedAt));
        for (int i = 0; i < chapters.size(); i++) p.setProperty("chapter." + i, chapters.get(i));
        p.setProperty("toc", "" + tocLinks.size());
        for (int i = 0; i < tocLinks.size(); i++) {
            p.setProperty("label." + i, tocLabels.get(i)); p.setProperty("link." + i, tocLinks.get(i));
        }
        try (OutputStream out = new FileOutputStream(new File(directory, "book.properties"))) { p.store(out, "BOOKWORM library"); }
    }
    public static EpubBook load(File directory) throws IOException {
        Properties p = new Properties();
        try (InputStream in = new FileInputStream(new File(directory, "book.properties"))) { p.load(in); }
        EpubBook b = new EpubBook(); b.directory = directory;
        b.id = p.getProperty("id", ""); b.title = p.getProperty("title", "Untitled");
        b.author = p.getProperty("author", "Unknown author"); b.cover = p.getProperty("cover", "");
        if (!b.id.matches("[0-9a-f]{64}") || !directory.getName().equals(b.id)) throw new IOException("Library identity does not match its directory");
        if (b.title.trim().isEmpty()) b.title = "Untitled";
        if (b.author.trim().isEmpty()) b.author = "Unknown author";
        long legacyDate = new File(directory, "original.epub").lastModified();
        if (legacyDate <= 0) legacyDate = new File(directory, "book.properties").lastModified();
        try { b.addedAt = Long.parseLong(p.getProperty("addedAt", Long.toString(legacyDate))); }
        catch (NumberFormatException invalidDate) { b.addedAt = legacyDate; }
        if (b.addedAt <= 0) b.addedAt = legacyDate;
        b.lastRead = LibraryFiles.lastRead(directory);
        try {
            int count = Integer.parseInt(p.getProperty("chapters", "0"));
            if (count < 1 || count > 2048) throw new IOException("Invalid library record");
            for (int i = 0; i < count; i++) {
                String path = p.getProperty("chapter." + i);
                if (!b.content(path).isFile()) throw new IOException("Missing chapter content");
                b.chapters.add(path);
            }
            count = Integer.parseInt(p.getProperty("toc", "0"));
            if (count < 0 || count > 2048) throw new IOException("Invalid table of contents");
            for (int i = 0; i < count; i++) { b.tocLabels.add(p.getProperty("label." + i)); b.tocLinks.add(p.getProperty("link." + i)); }
        } catch (RuntimeException invalid) { throw new IOException("Invalid library record", invalid); }
        return b;
    }
    public File content(String path) throws IOException { return EpubParser.safeFile(new File(directory, "content"), path); }
}
