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
    private final OpenBook select;
    private final BookshelfPreferences physical;
    private final PhysicalShelf selection = PhysicalShelf.selection();
    private List<EpubBook> spineBooks = Collections.emptyList();
    private int columns = 1;
    private java.lang.ref.WeakReference<ViewGroup> visibleParent = new java.lang.ref.WeakReference<>(null);
    private LibraryRows rows = new LibraryRows(Collections.emptyList(), LibrarySort.TITLE_ASC);
    public BookshelfAdapter(ThemedActivity owner, OpenBook open, OpenBook options, OpenBook select) {
        this.owner = owner; this.open = open; this.options = options; this.select=select;
        physical=BookshelfPreferences.load(owner,owner.appearance.classic);
    }
    public void submit(List<EpubBook> books, LibrarySort sort) { rows = new LibraryRows(books, sort); spineBooks=sort.ordered(books); notifyDataSetChanged(); }
    public void width(int pixels) {
        int next=PhysicalShelf.columns((int)(pixels/owner.getResources().getDisplayMetrics().density));
        if(columns!=next) { columns=next; if(spines()) notifyDataSetChanged(); }
    }
    private boolean spines() { return physical.presentation==PhysicalShelf.Presentation.SPINES; }
    public void selected(String id) {
        selection.select(id); ViewGroup parent=visibleParent.get();
        if(parent!=null) updateSelection(parent);
    }
    private void updateSelection(View view) {
        if(view.getTag() instanceof String) { view.setSelected(selection.isSelected((String)view.getTag())); view.setActivated(view.isSelected()); }
        if(view instanceof ViewGroup) for(int i=0;i<((ViewGroup)view).getChildCount();i++) updateSelection(((ViewGroup)view).getChildAt(i));
    }
    @Override public int getCount() { if(spines()) return PhysicalShelf.rows(spineBooks.size(),columns); return rows.rowCount(); }
    @Override public EpubBook getItem(int row) { return spines() ? spineBooks.get(row*columns) : rows.at(row, 0); }
    @Override public long getItemId(int row) { return row; }
    @Override public View getView(int index, View recycled, ViewGroup parent) {
        visibleParent=new java.lang.ref.WeakReference<>(parent);
        if(spines()) return spineRow(index);
        LinearLayout block = new LinearLayout(owner); block.setOrientation(LinearLayout.VERTICAL);
        LinearLayout row = new LinearLayout(owner); row.setBaselineAligned(false);
        for (int column = 0; column < 2; column++) {
            EpubBook book = rows.at(index, column);
            View card = book == null ? new View(owner) : card(book);
            // MATCH_PARENT here collapses odd rows to the empty cell on Android 5.1.
            row.addView(card, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        }
        block.addView(row, new LinearLayout.LayoutParams(-1, LinearLayout.LayoutParams.WRAP_CONTENT));
        return block;
    }
    private View card(EpubBook book) {
        LinearLayout content = new LinearLayout(owner); content.setOrientation(LinearLayout.VERTICAL);
        DimensionalCoverView frame = new DimensionalCoverView(owner,physical.depth);
        CoverThumbnailView cover = frame.cover;
        cover.bind(book, CoverRepository.global(owner), null);
        content.addView(frame, new LinearLayout.LayoutParams(-1, owner.dp(210)));
        TextView title = owner.text(book.title, false); title.setTextSize(ShelfPresentation.titleSize(owner.appearance.fontSize));
        title.setLines(2); title.setEllipsize(android.text.TextUtils.TruncateAt.END); title.setPadding(0, owner.dp(8), 0, 0); content.addView(title);
        TextView author = owner.text(book.author, false); author.setTextSize(ShelfPresentation.authorSize(owner.appearance.fontSize));
        author.setTextColor(ShelfPresentation.muted(owner.appearance)); author.setSingleLine(true); author.setEllipsize(android.text.TextUtils.TruncateAt.END);
        author.setPadding(0, owner.dp(3), 0, owner.dp(4)); content.addView(author);
        title.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO); author.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        ImageButton overflow = ShelfUi.action(owner, ShelfIcon.Kind.MORE, "Book options for " + book.title, v -> options.open(book));
        ShelfView support=physical.presentation==PhysicalShelf.Presentation.COVERS
                ? new ShelfView(owner,owner.appearance.background,owner.appearance.accent) : null;
        FrameLayout card = new FrameLayout(owner) {
            @Override protected void onMeasure(int widthSpec, int heightSpec) {
                int available = View.MeasureSpec.getSize(widthSpec) - getPaddingLeft() - getPaddingRight();
                int width = ShelfPresentation.coverWidth(available, owner.dp(140), owner.dp(48));
                FrameLayout.LayoutParams column = (FrameLayout.LayoutParams) content.getLayoutParams(); column.width = width;
                frame.getLayoutParams().height = width * 3 / 2;
                ((FrameLayout.LayoutParams) overflow.getLayoutParams()).topMargin = width * 3 / 2 + owner.dp(4);
                if(support!=null) ((FrameLayout.LayoutParams)support.getLayoutParams()).topMargin=width*3/2;
                super.onMeasure(widthSpec, heightSpec);
            }
        };
        card.setPadding(owner.dp(8), owner.dp(16), owner.dp(8), owner.dp(16)); card.setMinimumHeight(owner.dp(48));
        card.addView(content, new FrameLayout.LayoutParams(owner.dp(140), -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL));
        // A shallow supporting lip sits at the cover base, with metadata below it.
        if(support!=null) card.addView(support,new FrameLayout.LayoutParams(-1,owner.dp(6),Gravity.TOP));
        card.addView(overflow, new FrameLayout.LayoutParams(owner.dp(48), owner.dp(48), Gravity.TOP | Gravity.END));
        card.setFocusable(true); card.setClickable(true); card.setBackground(ShelfUi.feedback(owner));
        card.setContentDescription(book.title + ", by " + book.author + ". Read book. Long press for book options.");
        card.setOnClickListener(v -> open.open(book)); card.setOnLongClickListener(v -> { options.open(book); return true; }); return card;
    }
    private View spineRow(int index) {
        LinearLayout block=new LinearLayout(owner); block.setOrientation(LinearLayout.VERTICAL);
        LinearLayout row=new LinearLayout(owner); row.setGravity(Gravity.BOTTOM); row.setBaselineAligned(false);
        for(int column=0;column<columns;column++) {
            int at=index*columns+column;
            FrameLayout cell=new FrameLayout(owner);
            if(at<spineBooks.size()) {
                EpubBook book=spineBooks.get(at);
                cell.setTag(book.id);
                cell.setPadding(owner.dp(4),owner.dp(4),owner.dp(4),0);
                SpineView spine=new SpineView(owner,book);
                cell.addView(spine,new FrameLayout.LayoutParams(owner.dp(PhysicalShelf.width(book.id)),owner.dp(PhysicalShelf.height(book.id)),Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL));
                cell.setMinimumWidth(owner.dp(48)); cell.setFocusable(true); cell.setClickable(true);
                cell.setSelected(selection.isSelected(book.id)); cell.setActivated(selection.isSelected(book.id));
                android.graphics.drawable.GradientDrawable selected=new android.graphics.drawable.GradientDrawable();
                selected.setColor(ShelfPresentation.blend(owner.appearance.background,owner.appearance.accent,.14f));
                selected.setStroke(owner.dp(2),owner.appearance.accent);
                android.graphics.drawable.StateListDrawable feedback=new android.graphics.drawable.StateListDrawable();
                feedback.addState(new int[]{android.R.attr.state_selected},selected);
                feedback.addState(new int[]{},ShelfUi.feedback(owner)); cell.setBackground(feedback);
                cell.setContentDescription(book.title+", by "+book.author+". Select book. Long press for options.");
                cell.setOnClickListener(v -> { selected(book.id); select.open(book); });
                cell.setOnLongClickListener(v -> { selected(book.id); select.open(book); options.open(book); return true; });
            } else cell.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            row.addView(cell,new LinearLayout.LayoutParams(0,owner.dp(266),1));
        }
        block.addView(row,new LinearLayout.LayoutParams(-1,-2));
        block.addView(new ShelfView(owner,owner.appearance.background,owner.appearance.accent),new LinearLayout.LayoutParams(-1,owner.dp(8)));
        block.setPadding(0,owner.dp(12),0,owner.dp(12)); return block;
    }
}
