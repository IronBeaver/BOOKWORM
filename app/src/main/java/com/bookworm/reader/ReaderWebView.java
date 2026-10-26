package com.bookworm.reader;

import android.content.Context;
import android.view.*;
import android.webkit.WebView;

/** Observe legacy Android selection ActionMode without replacing its behavior. */
public final class ReaderWebView extends WebView {
    private ActionMode selection;
    public ReaderWebView(Context context) { super(context); }
    public boolean selecting() { return selection != null; }
    public boolean finishSelection() { if (selection == null) return false; selection.finish(); return true; }
    @Override public ActionMode startActionMode(ActionMode.Callback callback) {
        return super.startActionMode(new ActionMode.Callback() {
            @Override public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                boolean created = callback.onCreateActionMode(mode, menu); if (created) selection = mode; return created;
            }
            @Override public boolean onPrepareActionMode(ActionMode mode, Menu menu) { return callback.onPrepareActionMode(mode, menu); }
            @Override public boolean onActionItemClicked(ActionMode mode, MenuItem item) { return callback.onActionItemClicked(mode, item); }
            @Override public void onDestroyActionMode(ActionMode mode) { selection = null; callback.onDestroyActionMode(mode); }
        });
    }
    @Override public void destroy() { finishSelection(); super.destroy(); }
}
