package com.bookworm.reader;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.File;

/** Bounds-first decoding; no full-size cover bitmaps. */
public final class CoverImages {
    private static final android.util.LruCache<String, Bitmap> CACHE = new android.util.LruCache<String, Bitmap>(2 * 1024 * 1024) {
        @Override protected int sizeOf(String key, Bitmap bitmap) { return bitmap.getByteCount(); }
    };
    private CoverImages() { }
    public static boolean safe(File file) {
        return pixels(file) > 0;
    }
    public static int pixels(File file) {
        BitmapFactory.Options o = new BitmapFactory.Options(); o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getPath(), o);
        return o.outWidth > 0 && o.outHeight > 0 && o.outWidth <= 8192 && o.outHeight <= 8192 && (long) o.outWidth * o.outHeight <= 16000000 ? o.outWidth * o.outHeight : 0;
    }
    public static Bitmap thumbnail(File file) {
        Bitmap cached = CACHE.get(file.getPath()); if (cached != null) return cached;
        if (!safe(file)) return null;
        BitmapFactory.Options o = new BitmapFactory.Options(); o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getPath(), o); int sample = 1;
        while (Math.max(o.outWidth, o.outHeight) / sample > 256) sample *= 2;
        o.inJustDecodeBounds = false; o.inSampleSize = sample;
        Bitmap decoded = BitmapFactory.decodeFile(file.getPath(), o); if (decoded != null) CACHE.put(file.getPath(), decoded); return decoded;
    }
}
