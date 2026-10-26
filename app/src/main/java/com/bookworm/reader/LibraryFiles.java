package com.bookworm.reader;
import java.io.*;
import java.util.Properties;

/** Small atomic sidecar records; existing book metadata and position files are never rewritten. */
public final class LibraryFiles {
    private LibraryFiles() { }
    public static Properties read(File file) throws IOException {
        IOException failure = null;
        for (String suffix : new String[]{"", ".previous"}) {
            try (InputStream in = new FileInputStream(new File(file.getPath() + suffix))) {
                Properties p = new Properties(); p.load(in); return p;
            } catch (IOException | IllegalArgumentException bad) { failure = new IOException("Invalid library sidecar", bad); }
        }
        throw failure;
    }
    public static synchronized void write(File file, Properties value) throws IOException {
        File next = new File(file.getPath() + ".next"), backup = new File(file.getPath() + ".previous");
        try (FileOutputStream out = new FileOutputStream(next)) { value.store(out, "BOOKWORM library"); out.getFD().sync(); }
        if (file.exists()) {
            if (backup.exists() && !backup.delete()) throw new IOException("Cannot rotate library sidecar");
            if (!file.renameTo(backup)) throw new IOException("Cannot preserve previous library sidecar");
        }
        if (!next.renameTo(file)) throw new IOException("Cannot commit library sidecar");
    }
    public static void markRead(File directory, long time) throws IOException {
        Properties p = new Properties(); p.setProperty("lastRead", Long.toString(time));
        write(new File(directory, "usage.properties"), p);
    }
    public static long lastRead(File directory) {
        long time = Math.max(new File(directory, "position.properties").lastModified(), new File(directory, "position.previous").lastModified());
        try { time = Math.max(time, Long.parseLong(read(new File(directory, "usage.properties")).getProperty("lastRead", "0"))); }
        catch (IOException | NumberFormatException ignored) { /* Old records use their existing position timestamp. */ }
        return time;
    }
}
