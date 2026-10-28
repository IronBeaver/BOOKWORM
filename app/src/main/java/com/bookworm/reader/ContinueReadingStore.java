package com.bookworm.reader;

import android.content.Context;
import android.content.SharedPreferences;

/** Identity/attempt metadata only. ReadingPosition remains the only position store. */
public final class ContinueReadingStore {
    public static final String AUTO = "continue_on_launch";
    private static SharedPreferences prefs(Context context) { return context.getSharedPreferences("reading_launch", Context.MODE_PRIVATE); }
    public static boolean opened(Context context, String id) {
        return valid(id) && prefs(context).edit().putString("last_book", id).putString("pending_book", "").commit();
    }
    public static String begin(Context context, LibraryCatalog.Snapshot library, boolean enabled) {
        if (!enabled) return null;
        SharedPreferences p = prefs(context);
        String id = p.getString("last_book", "");
        if (id.isEmpty()) {
            // One-time compatibility with books read before this identity record existed.
            if (library.unavailable > 0) return null;
            EpubBook last = null;
            for (EpubBook b : library.books) if (b.lastRead > 0 && (last == null || b.lastRead > last.lastRead
                    || (b.lastRead == last.lastRead && b.id.compareTo(last.id) < 0))) last = b;
            if (last == null) return null;
            id = last.id;
        }
        if (!valid(id) || id.equals(p.getString("pending_book", ""))) return null;
        boolean available = false;
        for (EpubBook book : library.books) if (book.id.equals(id)) { available = true; break; }
        if (!available) return null; // Never silently open an older book when the remembered one is missing.
        // Persist before navigating. A crash/interruption cannot cause repeated automatic attempts.
        return p.edit().putString("last_book", id).putString("pending_book", id).commit() ? id : null;
    }
    private static boolean valid(String id) { return id != null && id.matches("[0-9a-f]{64}"); }
    private ContinueReadingStore() { }
}
