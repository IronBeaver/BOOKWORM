package com.bookworm.reader;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import java.util.*;

/** The shelf is a view of the persistent library, without a separate demo catalogue. */
public final class MainActivity extends ThemedActivity {
    private ListView list;
    private TextView status, empty;
    private Button retry;
    private BookshelfAdapter adapter;
    private LinearLayout selectionPanel;
    private String selectedBook;
    private int rowPosition, rowTop;
    private List<EpubBook> books = Collections.emptyList();
    private LibrarySort sort;
    private boolean loaded;
    private int unavailable;
    private String seedMessage = "";
    private LaunchAttempt launchAttempt;
    private final java.util.concurrent.ExecutorService worker = java.util.concurrent.Executors.newSingleThreadExecutor();
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        launchAttempt = new LaunchAttempt(state == null && isTaskRoot() && Intent.ACTION_MAIN.equals(getIntent().getAction())
                && getIntent().hasCategory(Intent.CATEGORY_LAUNCHER));
        sort = LibrarySort.fromKey(getSharedPreferences("bookshelf", MODE_PRIVATE).getString("sort", LibrarySort.TITLE_ASC.name()));
        if (state != null) { rowPosition = state.getInt("row"); rowTop = state.getInt("top"); selectedBook=state.getString("selectedBook"); }
    }
    @Override protected void onResume() {
        super.onResume(); launchAttempt.resume(); loadAppearance(); buildShelf(); LibraryStore.observe(this::refreshLibrary); refreshLibrary();
    }
    private void refreshLibrary() {
        final int launchTicket = launchAttempt.claim();
        worker.execute(() -> {
            String seedError = "";
            try { BundledBook.ensure(getApplicationContext()); }
            catch (java.io.IOException failed) { seedError = "Included Frankenstein could not be prepared: " + failed.getMessage(); }
            final String message = seedError;
            LibraryCatalog.Snapshot snapshot = LibraryCatalog.scan(LibraryStore.root(getApplicationContext()));
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (list.getChildCount() > 0) remember();
                books = snapshot.books; adapter.submit(books, sort);
                restoreSelection();
                loaded = true; unavailable = snapshot.unavailable; seedMessage = message; updateSummary();
                empty.setText("Your library is empty.\nImport an EPUB to begin reading. Use Reload library to retry preparing the included book.");
                retry.setVisibility(snapshot.unavailable > 0 || !message.isEmpty() ? View.VISIBLE : View.GONE);
                list.setSelectionFromTop(Math.min(rowPosition, Math.max(0, adapter.getCount() - 1)), rowTop);
                if (launchAttempt.allowed(launchTicket)) {
                    String id = ContinueReadingStore.begin(this, snapshot, ReaderPreferences.load(this).continueOnLaunch);
                    if (id != null) startActivity(new Intent(this, ReaderActivity.class).putExtra("book", id).putExtra(ContinueReadingStore.AUTO, true));
                }
            });
        });
    }
    private void remember() {
        if (list != null && list.getChildCount() > 0) { rowPosition = list.getFirstVisiblePosition(); rowTop = list.getChildAt(0).getTop(); }
    }
    @Override protected void onPause() { launchAttempt.pause(); LibraryStore.observe(null); remember(); super.onPause(); }
    @Override protected void onDestroy() { worker.shutdown(); super.onDestroy(); }
    @Override protected void onSaveInstanceState(Bundle state) { remember(); state.putInt("row", rowPosition); state.putInt("top", rowTop); state.putString("selectedBook",selectedBook); super.onSaveInstanceState(state); }
    private void buildShelf() {
        LinearLayout root = column(); root.setPadding(dp(12), 0, dp(12), dp(8));
        LinearLayout toolbar = ShelfUi.toolbar(this, "BOOKWORM", false);
        toolbar.addView(ShelfUi.action(this, ShelfIcon.Kind.SORT, "Sort bookshelf", v -> chooseSort()), new LinearLayout.LayoutParams(dp(48), dp(48)));
        toolbar.addView(ShelfUi.action(this, ShelfIcon.Kind.IMPORT, "Import EPUB", v -> startActivity(new Intent(this, ImportActivity.class))), new LinearLayout.LayoutParams(dp(48), dp(48)));
        toolbar.addView(ShelfUi.action(this, ShelfIcon.Kind.SETTINGS, "Settings", v -> startActivity(new Intent(this, SettingsActivity.class))), new LinearLayout.LayoutParams(dp(48), dp(48)));
        root.addView(toolbar);
        status = text("Loading library…", false); status.setTextSize(12); status.setTextColor(ShelfPresentation.muted(appearance)); status.setPadding(dp(8), dp(4), dp(8), dp(4));
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE); root.addView(status); if (loaded) updateSummary();
        retry = button("Reload library", v -> refreshLibrary()); retry.setVisibility(View.GONE); root.addView(retry);
        selectionPanel=new LinearLayout(this); selectionPanel.setOrientation(LinearLayout.HORIZONTAL); selectionPanel.setVisibility(View.GONE);
        root.addView(selectionPanel,new LinearLayout.LayoutParams(-1,-2));
        FrameLayout frame = new FrameLayout(this);
        list = new ListView(this); list.setDivider(null); list.setCacheColorHint(appearance.background); list.setItemsCanFocus(true);
        adapter = new BookshelfAdapter(this, this::openBook, this::bookOptions, this::selectBook);
        final ListView shelf = list;
        final BookshelfAdapter shelfAdapter = adapter;
        Runnable updateShelfWidth = () -> {
            if (list != shelf || adapter != shelfAdapter || isFinishing() || isDestroyed()) return;
            int usableWidth = shelf.getWidth() - shelf.getPaddingLeft() - shelf.getPaddingRight();
            if (usableWidth > 0) shelfAdapter.width(usableWidth);
        };
        list.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob) -> {
            // Coalesce width updates and notify the adapter outside the active layout pass.
            shelf.removeCallbacks(updateShelfWidth);
            shelf.post(updateShelfWidth);
        });
        list.setAdapter(adapter); adapter.submit(books, sort);
        empty = text("Preparing your library…", false); empty.setGravity(android.view.Gravity.CENTER);
        frame.addView(list, new FrameLayout.LayoutParams(-1, -1)); frame.addView(empty, new FrameLayout.LayoutParams(-1, -1)); list.setEmptyView(empty);
        root.addView(frame, new LinearLayout.LayoutParams(-1, 0, 1)); setContentView(root); list.setSelectionFromTop(rowPosition, rowTop);
        restoreSelection();
    }
    private void restoreSelection() {
        if(BookshelfPreferences.load(this,appearance.classic).presentation!=PhysicalShelf.Presentation.SPINES) { dismissSelection(); return; }
        if(selectedBook!=null) {
            for(EpubBook book:books) if(book.id.equals(selectedBook)) { selectBook(book); return; }
            dismissSelection();
        }
    }
    private void selectBook(EpubBook book) {
        selectedBook=book.id; adapter.selected(book.id); selectionPanel.removeAllViews();
        selectionPanel.setPadding(dp(8),dp(6),dp(8),dp(6));
        selectionPanel.setBackgroundColor(ShelfPresentation.blend(appearance.background,appearance.accent,.08f));
        CoverThumbnailView cover=new CoverThumbnailView(this); cover.bind(book,CoverRepository.global(this),null);
        selectionPanel.addView(cover,new LinearLayout.LayoutParams(dp(64),dp(96)));
        LinearLayout information=new LinearLayout(this); information.setOrientation(LinearLayout.VERTICAL); information.setPadding(dp(12),0,0,0);
        TextView title=text(book.title,false); title.setTextSize(16); title.setMaxLines(2); title.setEllipsize(android.text.TextUtils.TruncateAt.END); information.addView(title);
        TextView author=text(book.author,false); author.setTextSize(12); author.setSingleLine(true); author.setEllipsize(android.text.TextUtils.TruncateAt.END); information.addView(author);
        LinearLayout actions=new LinearLayout(this);
        Button read=button("Read",v -> openBook(book)); actions.addView(read,new LinearLayout.LayoutParams(0,-2,1));
        actions.addView(ShelfUi.action(this,ShelfIcon.Kind.MORE,"Book options for "+book.title,v -> bookOptions(book)),new LinearLayout.LayoutParams(dp(48),dp(48)));
        actions.addView(ShelfUi.action(this,ShelfIcon.Kind.BACK,"Dismiss selected book",v -> dismissSelection()),new LinearLayout.LayoutParams(dp(48),dp(48)));
        information.addView(actions); selectionPanel.addView(information,new LinearLayout.LayoutParams(0,-2,1));
        selectionPanel.setVisibility(View.VISIBLE); selectionPanel.setContentDescription("Selected book: "+book.title+", by "+book.author);
        selectionPanel.announceForAccessibility(selectionPanel.getContentDescription());
    }
    private void dismissSelection() {
        selectedBook=null; if(adapter!=null) adapter.selected(null);
        if(selectionPanel!=null) { selectionPanel.removeAllViews(); selectionPanel.setVisibility(View.GONE); }
    }
    @Override public void onBackPressed() { if(selectedBook!=null) dismissSelection(); else super.onBackPressed(); }
    private void chooseSort() {
        LibrarySort[] values = LibrarySort.values(); String[] labels = new String[values.length];
        for (int i = 0; i < values.length; i++) labels[i] = values[i].label;
        AlertDialog menu = new AlertDialog.Builder(this).setCustomTitle(ShelfUi.dialogTitle(this, "Sort bookshelf"))
                .setSingleChoiceItems(ShelfUi.menuAdapter(this, labels, true), sort.ordinal(), (dialog, selected) -> {
            LibrarySort next = values[selected];
            if (!getSharedPreferences("bookshelf", MODE_PRIVATE).edit().putString("sort", next.name()).commit()) {
                Toast.makeText(this, "Could not save sort preference", Toast.LENGTH_LONG).show(); return;
            }
            sort = next; updateSummary(); adapter.submit(books, sort); rowPosition = 0; rowTop = 0; list.setSelection(0); dialog.dismiss();
        }).setNegativeButton("Cancel", null).create(); ShelfUi.show(this, menu);
    }
    private void updateSummary() {
        String summary = ShelfPresentation.summary(books.size(), sort);
        if (unavailable > 0) summary += "\n" + unavailable + " unavailable record(s). Files and reading positions kept.";
        if (!seedMessage.isEmpty()) summary += "\n" + seedMessage; status.setText(summary);
    }
    private void openBook(EpubBook book) {
        boolean included = BundledBook.includes(this, book.id);
        startActivity(new Intent(this, included ? BundledBookActivity.class : ReaderActivity.class).putExtra("book", book.id).putExtra("read", included));
    }
    private void bookOptions(EpubBook book) {
        AlertDialog menu = new AlertDialog.Builder(this).setCustomTitle(ShelfUi.dialogTitle(this, book.title))
                .setAdapter(ShelfUi.menuAdapter(this, new String[]{"Read", "Cover options", "Book details"}, false), (dialog, choice) -> {
                    if (choice == 0) openBook(book);
                    else if (choice == 1) BinderyDialog.show(this, book, () -> { adapter.notifyDataSetChanged(); restoreSelection(); });
                    else bookDetails(book);
                }).setNegativeButton("Cancel", null).create(); ShelfUi.show(this, menu);
    }
    private void bookDetails(EpubBook book) {
        java.text.DateFormat dates = java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT);
        String detail = book.title + "\n" + book.author + "\n\nEPUB · " + book.chapters.size() + " chapters\n"
                + book.tocLinks.size() + " contents entries\nAdded: " + (book.addedAt > 0 ? dates.format(new Date(book.addedAt)) : "Unknown")
                + "\nLast read: " + (book.lastRead > 0 ? dates.format(new Date(book.lastRead)) : "Not yet");
        long bytes = new java.io.File(book.directory, "original.epub").length();
        if (bytes > 0) detail += "\nFile size: " + Math.max(1, (bytes + 1023) / 1024) + " KiB";
        LinearLayout body = column(); TextView content = text(detail, false); content.setTextIsSelectable(true); body.addView(content);
        ScrollView scroll = new ScrollView(this); scroll.addView(body);
        AlertDialog.Builder builder = new AlertDialog.Builder(this).setCustomTitle(ShelfUi.dialogTitle(this, "Book details"))
                .setView(scroll).setPositiveButton("Read", (d, which) -> openBook(book)).setNegativeButton("Close", null);
        if (BundledBook.includes(this, book.id)) builder.setNeutralButton("Source & license", (d, which) -> startActivity(new Intent(this, BundledBookActivity.class)));
        ShelfUi.show(this, builder.create());
    }
}
