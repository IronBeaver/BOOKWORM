package com.bookworm.reader;

import java.util.*;

/** Stable ordering only; never edits identifiers, book files or reading positions. */
public enum LibrarySort {
    TITLE_ASC("Title A–Z"), TITLE_DESC("Title Z–A"), AUTHOR_ASC("Author A–Z"),
    AUTHOR_DESC("Author Z–A"), ADDED_NEWEST("Date added · newest"),
    ADDED_OLDEST("Date added · oldest"), RECENT("Recently read");
    public final String label;
    LibrarySort(String label) { this.label = label; }
    public static LibrarySort fromKey(String key) {
        try { return valueOf(key); } catch (IllegalArgumentException | NullPointerException unknown) { return TITLE_ASC; }
    }
    public List<EpubBook> ordered(List<EpubBook> books) {
        List<EpubBook> result = new ArrayList<>(books);
        Collections.sort(result, (a, b) -> {
            int primary;
            switch (this) {
                case TITLE_DESC: primary = b.title.compareToIgnoreCase(a.title); break;
                case AUTHOR_ASC: primary = a.author.compareToIgnoreCase(b.author); break;
                case AUTHOR_DESC: primary = b.author.compareToIgnoreCase(a.author); break;
                case ADDED_NEWEST: primary = Long.compare(b.addedAt, a.addedAt); break;
                case ADDED_OLDEST: primary = Long.compare(a.addedAt, b.addedAt); break;
                case RECENT:
                    primary = Long.compare(b.lastRead, a.lastRead);
                    if (primary == 0) primary = Long.compare(b.addedAt, a.addedAt);
                    break;
                default: primary = a.title.compareToIgnoreCase(b.title);
            }
            if (primary == 0) primary = a.title.compareToIgnoreCase(b.title);
            if (primary == 0) primary = a.author.compareToIgnoreCase(b.author);
            return primary == 0 ? a.id.compareTo(b.id) : primary;
        });
        return Collections.unmodifiableList(result);
    }
}
