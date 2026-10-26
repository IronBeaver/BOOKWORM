package com.bookworm.reader;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/** Locally drawn shelf lip and grain. */
public final class ShelfView extends View {
    private final Paint paint = new Paint();
    private final int wood, edge, shadow;
    public ShelfView(Context context) {
        this(context, 0xff171a19, 0xffc9aa78);
    }
    public ShelfView(Context context, int background, int accent) {
        super(context); setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        wood = ShelfPresentation.blend(background, 0xff8d6847, .30f);
        edge = ShelfPresentation.blend(wood, accent, .20f); shadow = ShelfPresentation.blend(wood, 0xff000000, .16f);
    }
    @Override protected void onDraw(Canvas canvas) {
        paint.setColor(wood); canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        paint.setColor(edge); canvas.drawRect(0, 0, getWidth(), getHeight() * .18f, paint);
        paint.setColor(shadow);
        canvas.drawRect(0, getHeight() * .8f, getWidth(), getHeight(), paint);
        paint.setStrokeWidth(1);
        canvas.drawLine(0, getHeight() * .5f, getWidth(), getHeight() * .6f, paint);
    }
}
