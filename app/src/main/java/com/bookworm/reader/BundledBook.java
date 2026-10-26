package com.bookworm.reader;
import android.content.Context;
import java.io.*;

/** Fixed, unmodified Project Gutenberg older-reader edition, available entirely offline. */
public final class BundledBook {
    public static final String KEY = "frankenstein-gutenberg-84";
    public static final String ID = "eb696684c1314092bfe973be1241ab1f44cde101ac19bc36a574893de24016c2";
    public static final String ASSET = "books/frankenstein.epub";
    private BundledBook() { }
    public static EpubBook ensure(Context context) throws IOException {
        return LibrarySeeder.ensure(LibraryStore.root(context), KEY, ID, () -> context.getAssets().open(ASSET));
    }
    public static boolean includes(Context context, String id) {
        if (ID.equals(id)) return true;
        try { return id.equals(LibraryFiles.read(new File(LibraryStore.root(context), "seed-" + KEY + ".properties")).getProperty("id")); }
        catch (IOException ignored) { return false; }
    }
}
