package com.bookworm.reader;

import android.content.Context;
import android.content.SharedPreferences;

/** Additive shelf-only keys; old classic preference remains a migration fallback. */
public final class BookshelfPreferences {
    public final PhysicalShelf.Presentation presentation;
    public final boolean depth;
    public BookshelfPreferences(PhysicalShelf.Presentation presentation, boolean depth) {
        this.presentation = presentation; this.depth = depth;
    }
    public static BookshelfPreferences load(Context context, boolean classic) {
        SharedPreferences p = context.getSharedPreferences("bookshelf", Context.MODE_PRIVATE);
        return new BookshelfPreferences(PhysicalShelf.Presentation.from(p.getString("presentation", null), classic),
                p.getBoolean("depth", true));
    }
    public boolean save(Context context) {
        return context.getSharedPreferences("bookshelf", Context.MODE_PRIVATE).edit()
                .putString("presentation", presentation.name()).putBoolean("depth", depth).commit();
    }
}
