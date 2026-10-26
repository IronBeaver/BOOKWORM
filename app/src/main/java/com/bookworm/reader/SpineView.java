package com.bookworm.reader;

import android.graphics.*;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.LruCache;
import android.view.View;

/** Small opaque spine bitmaps, generated once per metadata/colour/size key; never per draw. */
public final class SpineView extends View {
    private static final LruCache<String, Bitmap> CACHE = new LruCache<String, Bitmap>(1536 * 1024) {
        @Override protected int sizeOf(String key, Bitmap bitmap) { return bitmap.getByteCount(); }
    };
    private final EpubBook book;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RectF target = new RectF();
    private Bitmap bitmap;
    public SpineView(ThemedActivity owner, EpubBook book) {
        super(owner); this.book=book; setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    @Override protected void onSizeChanged(int w,int h,int oldW,int oldH) {
        super.onSizeChanged(w,h,oldW,oldH); prepare();
    }
    @Override protected void onAttachedToWindow() { super.onAttachedToWindow(); if(bitmap==null) prepare(); }
    @Override protected void onDetachedFromWindow() { bitmap=null; super.onDetachedFromWindow(); }
    private void prepare() {
        if(getWidth()<=0 || getHeight()<=0) return;
        int h=Math.min(640,getHeight()), w=Math.max(1,Math.min(192, Math.round(getWidth()*h/(float)getHeight())));
        int color=CoverChoice.COLORS[CoverRepository.choice(book).colorFor(book.id)];
        float scale=getResources().getConfiguration().fontScale;
        String key=PhysicalShelf.cacheKey(book.id,book.title,book.author,color,w,h,scale);
        bitmap=CACHE.get(key);
        if(bitmap==null) { bitmap=render(book.title,book.author,color,w,h,scale); CACHE.put(key,bitmap); }
        invalidate();
    }
    static Bitmap render(String title,String author,int color,int w,int h,float scale) {
        Bitmap result=Bitmap.createBitmap(w,h,Bitmap.Config.RGB_565);
        Canvas c=new Canvas(result); Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); float u=w/64f;
        p.setShader(new LinearGradient(0,0,w,0,new int[]{0xff101413,color,color,0xff151815},new float[]{0,.19f,.72f,1},Shader.TileMode.CLAMP));
        c.drawRect(0,0,w,h,p); p.setShader(null);
        p.setColor(0x1effffff); c.drawLine(5*u,0,5*u,h,p);
        p.setColor(0x28000000); c.drawLine(w-5*u,0,w-5*u,h,p);
        p.setColor(CoverChoice.GOLD); p.setStrokeWidth(Math.max(1,u));
        for(float y:new float[]{16*u,23*u,h-23*u,h-16*u}) c.drawLine(7*u,y,w-7*u,y,p);
        // Two restrained embossed bands, then lettering along the spine's long axis.
        p.setColor(0x36ffffff); c.drawRect(2*u,10*u,w-2*u,12*u,p); c.drawRect(2*u,h-12*u,w-2*u,h-10*u,p);
        c.save(); c.translate(w/2f,h/2f); c.rotate(90);
        TextPaint text=new TextPaint(Paint.ANTI_ALIAS_FLAG); text.setColor(CoverChoice.GOLD); text.setTypeface(Typeface.create("serif",Typeface.BOLD));
        float available=Math.max(1,h-82*u);
        CoverTypography.Layout layout=SpineTypography.title(title,w,h,scale,new CoverTypography.Metrics() {
            public float width(String value,float size) { text.setTextSize(size); return text.measureText(value); }
            public float lineHeight(float size) { text.setTextSize(size); return text.getFontSpacing(); }
        });
        text.setTextSize(layout.size);
        c.save(); c.clipRect(-available/2,-25*u,available/2,12*u);
        float baseline=-22*u-text.ascent();
        for(String line:layout.lines) { c.drawText(line,-available/2,baseline,text); baseline+=layout.lineHeight; }
        c.restore();
        if(author!=null && !author.trim().isEmpty()) {
            text.setTypeface(Typeface.create("serif",Typeface.NORMAL)); text.setTextSize(8*u);
            String by=TextUtils.ellipsize(clean(author),text,available,TextUtils.TruncateAt.END).toString();
            c.drawText(by,-available/2,24*u,text);
        }
        c.restore(); return result;
    }
    private static String clean(String s) { return s==null || s.trim().isEmpty() ? "Untitled" : s.replaceAll("\\s+"," ").trim(); }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas); if(bitmap==null) return;
        target.set(0,0,getWidth(),getHeight()); canvas.drawBitmap(bitmap,null,target,paint);
    }
}
