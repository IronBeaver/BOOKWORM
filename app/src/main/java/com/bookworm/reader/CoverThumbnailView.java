package com.bookworm.reader;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Every artwork occupies the same portrait 2:3 slot; no bitmap generation in onDraw. */
public final class CoverThumbnailView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RectF destination = new RectF();
    private EpubBook book;
    private CoverChoice.Policy global;
    private CoverChoice draft;
    private Bitmap bitmap;
    private volatile int ticket;
    public CoverThumbnailView(Context context) { super(context); setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO); }
    public void bind(EpubBook book, CoverChoice.Policy global, CoverChoice draft) {
        this.book = book; this.global = global; this.draft = draft; bitmap = null; ticket++; request(); invalidate();
    }
    private void request() {
        if (book == null || getWidth() == 0 || getHeight() == 0) return;
        int height = Math.min(384, Math.min(getHeight(), getWidth() * 3 / 2));
        int width = Math.max(2, height * 2 / 3); height = width * 3 / 2;
        CoverRepository.request(this, ++ticket, book, global, draft, width, Math.max(3, height));
    }
    boolean current(int value) { return value == ticket; }
    void accept(int value, Bitmap ready) { if (current(value)) { bitmap = ready; invalidate(); } }
    @Override protected void onSizeChanged(int w, int h, int oldW, int oldH) { super.onSizeChanged(w, h, oldW, oldH); request(); }
    @Override protected void onAttachedToWindow() { super.onAttachedToWindow(); if (bitmap == null) request(); }
    @Override protected void onDetachedFromWindow() { ticket++; bitmap = null; super.onDetachedFromWindow(); }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bitmap == null) return;
        float h = Math.min(getHeight(), getWidth() * 1.5f), w = h * 2 / 3;
        destination.set((getWidth() - w) / 2, (getHeight() - h) / 2, (getWidth() + w) / 2, (getHeight() + h) / 2);
        canvas.drawBitmap(bitmap, null, destination, paint);
    }
}
