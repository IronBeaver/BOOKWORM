package com.bookworm.reader;

import android.graphics.*;
import android.text.*;

/** Native Canvas/Paint adapter. Only the requested thumbnail bitmap is allocated. */
public final class LeatherCoverRenderer {
    private LeatherCoverRenderer() { }
    public static Bitmap render(String id, String title, String author, int color, int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap); canvas.scale(width / 200f, height / 300f);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        TextPaint text = new TextPaint(Paint.ANTI_ALIAS_FLAG); text.setTypeface(Typeface.SERIF);
        LeatherArt.draw(new LeatherArt.Surface() {
            public void background(int color) {
                paint.setShader(new LinearGradient(0, 0, 200, 300, LeatherArt.shade(color, 8), LeatherArt.shade(color, -9), Shader.TileMode.CLAMP));
                canvas.drawRect(0, 0, 200, 300, paint); paint.setShader(null);
            }
            public void fill(int color, float x, float y, float w, float h) { paint.setColor(color); paint.setStyle(Paint.Style.FILL); canvas.drawRect(x, y, x + w, y + h, paint); }
            public void line(int color, float stroke, float x, float y, float ex, float ey) { paint.setColor(color); paint.setStrokeWidth(stroke); canvas.drawLine(x, y, ex, ey, paint); }
            public void curve(int color, float stroke, float x, float y, float a, float b, float c, float d, float ex, float ey) {
                Path path = new Path(); path.moveTo(x, y); path.cubicTo(a, b, c, d, ex, ey);
                paint.setColor(color); paint.setStrokeWidth(stroke); paint.setStyle(Paint.Style.STROKE); canvas.drawPath(path, paint); paint.setStyle(Paint.Style.FILL);
            }
            public float width(String value, float size) { text.setTextSize(size); return text.measureText(value); }
            public float lineHeight(float size) { text.setTextSize(size); return text.getFontSpacing() * 1.05f; }
            public void text(CoverTypography.Layout fit, float x, float y, float w, float h) {
                text.setTextSize(fit.size);
                StringBuilder value = new StringBuilder(); for (String line : fit.lines) { if (value.length() > 0) value.append('\n'); value.append(line); }
                StaticLayout layout = new StaticLayout(value, text, (int) w, Layout.Alignment.ALIGN_CENTER, 1.05f, 0, false);
                // Native fallback fonts can have different extents from the primary serif metrics.
                while (layout.getHeight() > h && text.getTextSize() > 1) {
                    text.setTextSize(Math.max(1, text.getTextSize() - .5f));
                    layout = new StaticLayout(value, text, (int) w, Layout.Alignment.ALIGN_CENTER, 1.05f, 0, false);
                }
                canvas.save(); canvas.clipRect(x - 1, y - 1, x + w + 1, y + h + 1);
                canvas.translate(x, y + Math.max(0, (h - layout.getHeight()) / 2f));
                text.setColor(0xff69491f); canvas.save(); canvas.translate(.5f, .75f); layout.draw(canvas); canvas.restore();
                text.setColor(CoverChoice.GOLD); layout.draw(canvas); canvas.restore();
            }
        }, id, title, author, color);
        return bitmap;
    }
}
