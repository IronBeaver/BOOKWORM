package com.bookworm.reader;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Small standard-action glyphs drawn locally; no bitmap assets or icon library. */
public final class ShelfIcon extends Drawable {
    public enum Kind { SORT, IMPORT, SETTINGS, MORE, BACK }
    private final Kind kind;
    private final int size;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    public ShelfIcon(Kind kind, int color, int size) { this.kind = kind; this.size = size; paint.setColor(color); paint.setStrokeCap(Paint.Cap.ROUND); paint.setStrokeJoin(Paint.Join.ROUND); }
    @Override public int getIntrinsicWidth() { return size; }
    @Override public int getIntrinsicHeight() { return size; }
    @Override public void draw(Canvas canvas) {
        canvas.save(); canvas.translate(getBounds().left, getBounds().top); canvas.scale(getBounds().width() / 24f, getBounds().height() / 24f);
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(1.7f);
        switch (kind) {
            case SORT:
                canvas.drawLine(4, 6, 20, 6, paint); canvas.drawLine(4, 12, 16, 12, paint); canvas.drawLine(4, 18, 11, 18, paint); break;
            case IMPORT:
                canvas.drawLine(12, 3, 12, 15, paint); canvas.drawLine(8, 11, 12, 15, paint); canvas.drawLine(12, 15, 16, 11, paint);
                path.reset(); path.moveTo(4, 15); path.lineTo(4, 20); path.lineTo(20, 20); path.lineTo(20, 15); canvas.drawPath(path, paint); break;
            case SETTINGS:
                path.reset();
                for (int i = 0; i < 32; i++) {
                    double angle = i * Math.PI / 16; float radius = i % 4 == 0 || i % 4 == 3 ? 9 : 7.3f;
                    float x = 12 + (float) Math.cos(angle) * radius, y = 12 + (float) Math.sin(angle) * radius;
                    if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
                }
                path.close(); canvas.drawPath(path, paint); canvas.drawCircle(12, 12, 3, paint); break;
            case MORE:
                paint.setStyle(Paint.Style.FILL); for (int y = 5; y <= 19; y += 7) canvas.drawCircle(12, y, 1.5f, paint); break;
            case BACK:
                canvas.drawLine(5, 12, 20, 12, paint); canvas.drawLine(5, 12, 12, 5, paint); canvas.drawLine(5, 12, 12, 19, paint); break;
        }
        canvas.restore();
    }
    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
