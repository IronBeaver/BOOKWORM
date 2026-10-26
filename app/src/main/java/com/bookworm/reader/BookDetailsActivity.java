package com.bookworm.reader;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;

public final class BookDetailsActivity extends ThemedActivity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); loadAppearance();
        int index = getIntent().getIntExtra("book", -1);
        if (index < 0 || index >= DemoBooks.count()) { finish(); return; }
        DemoBooks.Book book = DemoBooks.get(index);
        LinearLayout root = column();
        root.addView(button("Back to bookshelf", v -> finish()));
        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.addView(new BookCoverView(this, book), new LinearLayout.LayoutParams(dp(144), dp(192)));
        content.addView(text(book.title, true));
        content.addView(text("By " + book.author, false));
        content.addView(text("DEMO BOOK · FICTIONAL SAMPLE", false));
        content.addView(text(book.description, false));
        content.addView(text("This sample has no reading content. Book importing and reading will arrive in a future milestone.", false));
        scroll.addView(content); root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
    }
}
