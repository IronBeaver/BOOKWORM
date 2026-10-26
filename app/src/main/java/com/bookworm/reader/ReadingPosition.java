package com.bookworm.reader;
import java.io.*;
import java.util.Properties;

/** Chapter path + sanitized UTF-16 text offset, independent of pixel geometry or reading mode. */
public final class ReadingPosition {
    public final String chapter;
    public final int offset;
    public ReadingPosition(String chapter, int offset) { this.chapter = chapter; this.offset = Math.max(0, offset); }
    public void save(File directory) throws IOException {
        Properties p = new Properties(); p.setProperty("chapter", chapter); p.setProperty("offset", "" + offset);
        File next = new File(directory, "position.next"), current = new File(directory, "position.properties");
        try (FileOutputStream out = new FileOutputStream(next)) { p.store(out, "BOOKWORM text anchor"); out.getFD().sync(); }
        // Android/Linux rename replaces atomically. Host tests use the backup for Windows replacement semantics.
        File backup = new File(directory, "position.previous");
        if (backup.exists() && !backup.delete()) throw new IOException("Could not rotate position backup");
        if (current.exists() && !current.renameTo(backup)) throw new IOException("Could not back up reading position");
        if (!next.renameTo(current)) throw new IOException("Could not save reading position");
    }
    public static ReadingPosition load(File directory, String fallback) {
        for (String name : new String[]{"position.properties", "position.previous"}) {
            try (InputStream in = new FileInputStream(new File(directory, name))) {
                Properties p = new Properties(); p.load(in);
                return new ReadingPosition(p.getProperty("chapter", fallback), Integer.parseInt(p.getProperty("offset", "0")));
            } catch (IOException | IllegalArgumentException ignored) { /* Try last complete position, then chapter start. */ }
        }
        return new ReadingPosition(fallback, 0);
    }
}
