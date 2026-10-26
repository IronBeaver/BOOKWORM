package com.bookworm.reader;
import android.content.Context;
import java.io.*;
import java.util.*;

public final class LibraryStore {
    private static Runnable listener;
    public static void observe(Runnable changed) { listener = changed; }
    public static void notifyChanged() { if (listener != null) listener.run(); }
    private LibraryStore() { }
    public static File root(Context context) { return new File(context.getFilesDir(), "library"); }
    public static EpubBook open(Context context, String id) throws IOException {
        if (id == null || !id.matches("[0-9a-f]{64}")) throw new IOException("Invalid library ID");
        return EpubBook.load(new File(root(context), id));
    }
    public static List<EpubBook> list(Context context) {
        return LibrarySort.TITLE_ASC.ordered(LibraryCatalog.scan(root(context)).books);
    }
}
