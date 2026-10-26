package com.bookworm.reader;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/** Procedural cover: no bitmap allocations, downloads, or image cache. */
@android.annotation.SuppressLint("ViewConstructor") // Programmatic only; requires a book.
public final class BookCoverView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int color;
    public BookCoverView(Context context, DemoBooks.Book book) {
        super(context); color = book.color;
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth(), h = getHeight();
        paint.setColor(color); canvas.drawRect(0, 0, w, h, paint);
        paint.setColor(0x55000000); canvas.drawRect(0, 0, w * .08f, h, paint);
        paint.setColor(0xffe9d7af); paint.setStrokeWidth(Math.max(1, w * .008f));
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawRect(w * .17f, h * .12f, w * .86f, h * .88f, paint);
        canvas.drawCircle(w * .515f, h * .42f, w * .16f, paint);
        canvas.drawLine(w * .3f, h * .67f, w * .73f, h * .67f, paint);
        canvas.drawLine(w * .37f, h * .73f, w * .66f, h * .73f, paint);
        paint.setStyle(Paint.Style.FILL);
    }
}
