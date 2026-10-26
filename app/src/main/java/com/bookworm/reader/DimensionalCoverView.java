package com.bookworm.reader;

import android.graphics.*;
import android.view.View;
import android.widget.FrameLayout;

/** A shallow physical frame around unchanged Bindery/artwork thumbnails. No bitmap copies. */
public final class DimensionalCoverView extends FrameLayout {
    public final CoverThumbnailView cover;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final boolean depth;
    private final float unit;
    public DimensionalCoverView(ThemedActivity owner, boolean depth) {
        super(owner); this.depth = depth; unit = getResources().getDisplayMetrics().density;
        setWillNotDraw(false); setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        cover = new CoverThumbnailView(owner);
        int edge = depth ? owner.dp(6) : 0;
        setPadding(depth ? owner.dp(2) : 0, depth ? owner.dp(2) : 0, edge, edge);
        addView(cover, new FrameLayout.LayoutParams(-1, -1));
    }
    @Override protected void onDraw(Canvas canvas) {
        if (!depth) return;
        float left=getPaddingLeft(), top=getPaddingTop(), right=getWidth()-getPaddingRight(), bottom=getHeight()-getPaddingBottom();
        // Three translucent, inset silhouettes approximate a soft contact shadow cheaply.
        for (int i=3; i>=1; i--) {
            paint.setColor(0x10000000); rect.set(left+i*unit,top+i*unit,right+(i+1)*unit,bottom+(i+1)*unit);
            canvas.drawRoundRect(rect,2*unit,2*unit,paint);
        }
        paint.setColor(0xffa99e85); canvas.drawRect(right,top+2*unit,right+3*unit,bottom+2*unit,paint);
        paint.setColor(0xffded2b7); canvas.drawRect(left+2*unit,bottom,right+3*unit,bottom+2*unit,paint);
        paint.setColor(0x55433429); canvas.drawLine(right+unit,top+3*unit,right+unit,bottom,paint);
    }
    @Override protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas); if (!depth) return;
        float left=getPaddingLeft(), right=getWidth()-getPaddingRight(), top=getPaddingTop(), bottom=getHeight()-getPaddingBottom();
        paint.setColor(0x33000000); canvas.drawRect(left,top,left+3*unit,bottom,paint);
        paint.setColor(0x30ffffff); canvas.drawRect(left+3*unit,top,left+4*unit,bottom,paint);
        canvas.drawRect(left+4*unit,top,right,top+unit,paint);
        paint.setColor(0x25000000); canvas.drawRect(left,bottom-unit,right,bottom,paint);
    }
}
