package com.bookworm.reader;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Both shelf styles use the same real-book rows, measured from card content on API 22. */
public final class BookshelfAdapter extends BaseAdapter {
    private final ThemedActivity owner;
    public interface OpenBook { void open(EpubBook book); }
    private final OpenBook open;
    private final OpenBook options;
    private LibraryRows rows = new LibraryRows(Collections.emptyList(), LibrarySort.TITLE_ASC);
    public BookshelfAdapter(ThemedActivity owner, OpenBook open, OpenBook options) { this.owner = owner; this.open = open; this.options = options; }
    public void submit(List<EpubBook> books, LibrarySort sort) { rows = new LibraryRows(books, sort); notifyDataSetChanged(); }
    @Override public int getCount() { return rows.rowCount(); }
    @Override public EpubBook getItem(int row) { return rows.at(row, 0); }
    @Override public long getItemId(int row) { return row; }
    @Override public View getView(int index, View recycled, ViewGroup parent) {
        LinearLayout block = new LinearLayout(owner); block.setOrientation(LinearLayout.VERTICAL);
        LinearLayout row = new LinearLayout(owner); row.setBaselineAligned(false);
        for (int column = 0; column < 2; column++) {
            EpubBook book = rows.at(index, column);
            View card = book == null ? new View(owner) : card(book);
            // MATCH_PARENT here collapses odd rows to the empty cell on Android 5.1.
            row.addView(card, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        }
        block.addView(row, new LinearLayout.LayoutParams(-1, LinearLayout.LayoutParams.WRAP_CONTENT));
        if (owner.appearance.classic) block.addView(new ShelfView(owner, owner.appearance.background, owner.appearance.accent), new LinearLayout.LayoutParams(-1, owner.dp(8)));
        return block;
    }
    private View card(EpubBook book) {
        LinearLayout content = new LinearLayout(owner); content.setOrientation(LinearLayout.VERTICAL);
        CoverThumbnailView cover = new CoverThumbnailView(owner);
        cover.bind(book, CoverRepository.global(owner), null);
        cover.setBackgroundColor(owner.appearance.background); cover.setElevation(owner.dp(1));
        content.addView(cover, new LinearLayout.LayoutParams(-1, owner.dp(210)));
        TextView title = owner.text(book.title, false); title.setTextSize(ShelfPresentation.titleSize(owner.appearance.fontSize));
        title.setLines(2); title.setEllipsize(android.text.TextUtils.TruncateAt.END); title.setPadding(0, owner.dp(8), 0, 0); content.addView(title);
        TextView author = owner.text(book.author, false); author.setTextSize(ShelfPresentation.authorSize(owner.appearance.fontSize));
        author.setTextColor(ShelfPresentation.muted(owner.appearance)); author.setSingleLine(true); author.setEllipsize(android.text.TextUtils.TruncateAt.END);
        author.setPadding(0, owner.dp(3), 0, owner.dp(4)); content.addView(author);
        title.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO); author.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        ImageButton overflow = ShelfUi.action(owner, ShelfIcon.Kind.MORE, "Book options for " + book.title, v -> options.open(book));
        FrameLayout card = new FrameLayout(owner) {
            @Override protected void onMeasure(int widthSpec, int heightSpec) {
                int available = View.MeasureSpec.getSize(widthSpec) - getPaddingLeft() - getPaddingRight();
                int width = ShelfPresentation.coverWidth(available, owner.dp(140), owner.dp(48));
                FrameLayout.LayoutParams column = (FrameLayout.LayoutParams) content.getLayoutParams(); column.width = width;
                cover.getLayoutParams().height = width * 3 / 2;
                ((FrameLayout.LayoutParams) overflow.getLayoutParams()).topMargin = width * 3 / 2 + owner.dp(4);
                super.onMeasure(widthSpec, heightSpec);
            }
        };
        card.setPadding(owner.dp(8), owner.dp(16), owner.dp(8), owner.dp(16)); card.setMinimumHeight(owner.dp(48));
        card.addView(content, new FrameLayout.LayoutParams(owner.dp(140), -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL));
        card.addView(overflow, new FrameLayout.LayoutParams(owner.dp(48), owner.dp(48), Gravity.TOP | Gravity.END));
        card.setFocusable(true); card.setClickable(true); card.setBackground(ShelfUi.feedback(owner));
        card.setContentDescription(book.title + ", by " + book.author + ". Read book. Long press for book options.");
        card.setOnClickListener(v -> open.open(book)); card.setOnLongClickListener(v -> { options.open(book); return true; }); return card;
    }
}
