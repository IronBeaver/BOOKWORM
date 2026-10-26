package com.bookworm.reader;

import android.view.*;
import android.widget.*;

/** Native overlay siblings; no control ever changes the WebView's measured viewport. */
public final class ReaderChrome {
    public interface Actions {
        void library(); void contents(); void settings(); void page(int direction); void chapter(int direction); void source();
    }
    public final TextView progress;
    private final ThemedActivity owner;
    private final FrameLayout overlay;
    private final LinearLayout dock;
    private final TextView tiny;
    private final View library;
    private boolean showProgress;
    public ReaderChrome(ThemedActivity owner, FrameLayout root, String title, boolean paged, boolean source,
                        boolean showProgress, Actions actions, Runnable hide, Runnable show) {
        this.owner = owner; this.showProgress = showProgress;
        overlay = new FrameLayout(owner);
        LinearLayout top = new LinearLayout(owner); top.setOrientation(LinearLayout.VERTICAL); top.setBackgroundColor(owner.appearance.background);
        top.setClickable(true); top.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout bar = new LinearLayout(owner); bar.setGravity(Gravity.CENTER_VERTICAL); bar.setMinimumHeight(owner.dp(48));
        library = ShelfUi.action(owner, ShelfIcon.Kind.BACK, "Back to library", v -> actions.library());
        bar.addView(library, new LinearLayout.LayoutParams(owner.dp(48), owner.dp(48)));
        TextView book = owner.text(title, false); book.setTextSize(16); book.setSingleLine(true); book.setEllipsize(android.text.TextUtils.TruncateAt.END);
        bar.addView(book, new LinearLayout.LayoutParams(0, -2, 1));
        bar.addView(control("Hide", "Hide reader controls", hide), new LinearLayout.LayoutParams(-2, owner.dp(48))); top.addView(bar);
        LinearLayout tools = new LinearLayout(owner);
        tools.addView(control("Contents", "Table of contents", actions::contents), new LinearLayout.LayoutParams(0, -2, 1));
        tools.addView(control("Reading", "Reading settings", actions::settings), new LinearLayout.LayoutParams(0, -2, 1));
        if (source) tools.addView(control("Source/license", "Frankenstein source and license", actions::source), new LinearLayout.LayoutParams(0, -2, 1));
        top.addView(tools); overlay.addView(top, new FrameLayout.LayoutParams(-1, -2, Gravity.TOP));
        LinearLayout bottom = new LinearLayout(owner); bottom.setOrientation(LinearLayout.VERTICAL); bottom.setBackgroundColor(owner.appearance.background);
        bottom.setClickable(true); bottom.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        progress = owner.text("Loading chapter…", false); progress.setTextSize(14); progress.setLines(2); progress.setPadding(owner.dp(12), owner.dp(4), owner.dp(12), owner.dp(4)); bottom.addView(progress);
        if (paged) row(bottom, "Previous page", "Next page", () -> actions.page(-1), () -> actions.page(1));
        row(bottom, "Previous chapter", "Next chapter", () -> actions.chapter(-1), () -> actions.chapter(1));
        overlay.addView(bottom, new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM));
        dock = new LinearLayout(owner); dock.setGravity(Gravity.BOTTOM | Gravity.END); dock.setPadding(owner.dp(8), 0, owner.dp(8), 0);
        tiny = owner.text("Loading chapter…", false); tiny.setTextSize(11); tiny.setSingleLine(true); tiny.setEllipsize(android.text.TextUtils.TruncateAt.END);
        tiny.setTextColor(ShelfPresentation.muted(owner.appearance)); tiny.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        tiny.setBackgroundColor(owner.appearance.background); tiny.setPadding(owner.dp(4), owner.dp(2), owner.dp(4), owner.dp(2));
        dock.addView(tiny, new LinearLayout.LayoutParams(0, -2, 1));
        Button peek = control("Controls", "Show reader controls: library, contents and reading settings", show); peek.setTextSize(12); peek.setMinWidth(owner.dp(88));
        dock.addView(peek, new LinearLayout.LayoutParams(-2, -2));
        root.addView(dock, new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM));
        root.addView(overlay, new FrameLayout.LayoutParams(-1, -1));
    }
    private Button control(String title, String label, Runnable action) {
        Button button = owner.button(title, v -> action.run()); button.setTextSize(14); button.setContentDescription(label);
        button.setBackground(ShelfUi.feedback(owner)); button.setTextColor(owner.appearance.text); button.setBackgroundTintList(null);
        button.setMinHeight(owner.dp(48)); button.setStateListAnimator(null); return button;
    }
    private void row(LinearLayout parent, String left, String right, Runnable previous, Runnable next) {
        LinearLayout row = new LinearLayout(owner);
        row.addView(control(left, left, previous), new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(control(right, right, next), new LinearLayout.LayoutParams(0, -2, 1)); parent.addView(row);
    }
    public void label(CharSequence value) { tiny.setText(value); }
    public void reserveFallback(View content) {
        // A stable, compact rail keeps the visible fallback out of book text/links.
        // INVISIBLE (not GONE) retains this measurement while the overlay is open.
        dock.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            FrameLayout.LayoutParams area = (FrameLayout.LayoutParams) content.getLayoutParams();
            if (b > t && area.bottomMargin != b - t) { area.bottomMargin = b - t; content.setLayoutParams(area); }
        });
    }
    public void progressPreference(boolean visible) { showProgress = visible; tiny.setVisibility(visible ? View.VISIBLE : View.GONE); }
    public void visibility(boolean shown, boolean announce) {
        overlay.setVisibility(shown ? View.VISIBLE : View.GONE); dock.setVisibility(shown ? View.INVISIBLE : View.VISIBLE);
        tiny.setVisibility(showProgress ? View.VISIBLE : View.GONE);
        if (shown && announce) overlay.announceForAccessibility("Reader controls shown");
    }
}
