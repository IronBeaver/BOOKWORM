package com.bookworm.reader;
import java.util.*;

/** A single immutable real-library snapshot drives both row count and card access. */
public final class LibraryRows {
    private final List<EpubBook> books;
    public LibraryRows(List<EpubBook> source, LibrarySort sort) { books = sort.ordered(source); }
    public int bookCount() { return books.size(); }
    public int rowCount() { return (books.size() + 1) / 2; }
    public EpubBook at(int row, int column) {
        if (row < 0 || row >= rowCount() || column < 0 || column > 1) return null;
        int index = row * 2 + column; return index < books.size() ? books.get(index) : null;
    }
}
