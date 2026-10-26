package com.bookworm.reader;

import android.content.Context;
import android.graphics.*;
import android.os.*;
import android.util.LruCache;
import java.io.*;
import java.lang.ref.WeakReference;
import java.util.concurrent.*;

/** Process-wide bounded thumbnails. Jobs retain data and weak views, never Activities. */
public final class CoverRepository {
    private static final LruCache<String, Bitmap> CACHE = new LruCache<String, Bitmap>(3 * 1024 * 1024) {
        @Override protected int sizeOf(String key, Bitmap value) { return value.getByteCount(); }
    };
    private static final LruCache<String, CoverChoice> CHOICES = new LruCache<>(128);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final ThreadPoolExecutor WORK = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
            new ArrayBlockingQueue<Runnable>(64), new ThreadPoolExecutor.DiscardOldestPolicy());
    private CoverRepository() { }
    public static CoverChoice.Policy global(Context context) {
        return CoverChoice.Policy.from(context.getSharedPreferences("appearance", Context.MODE_PRIVATE).getString("cover_policy", "AUTO"));
    }
    public static CoverChoice choice(EpubBook book) {
        CoverChoice value = CHOICES.get(book.directory.getPath());
        if (value == null) { value = CoverChoice.load(book.directory); CHOICES.put(book.directory.getPath(), value); }
        return value;
    }
    public static void save(EpubBook book, CoverChoice choice) throws IOException {
        choice.save(book.directory); CHOICES.put(book.directory.getPath(), choice);
    }
    static void request(CoverThumbnailView view, int ticket, EpubBook book, CoverChoice.Policy global,
                        CoverChoice draft, int width, int height) {
        WeakReference<CoverThumbnailView> weak = new WeakReference<>(view);
        WORK.execute(() -> {
            CoverThumbnailView target = weak.get(); if (target == null || !target.current(ticket)) return;
            target = null;
            CoverChoice choice = draft == null ? choice(book) : draft;
            File original = null;
            try { if (!book.cover.isEmpty()) original = book.content(book.cover); } catch (IOException ignored) { }
            String source = original == null ? "" : original.getPath() + ":" + original.lastModified() + ":" + original.length();
            String key = CoverChoice.cacheKey(book.id, book.title, book.author, source, global, choice, width, height);
            Bitmap result = CACHE.get(key);
            if (result == null) {
                if (choice.useOriginal(global, original != null)) result = artwork(original, width, height);
                if (result == null) result = LeatherCoverRenderer.render(book.id, book.title, book.author,
                        CoverChoice.COLORS[choice.colorFor(book.id)], width, height);
                CACHE.put(key, result);
            }
            final Bitmap ready = result;
            MAIN.post(() -> { CoverThumbnailView current = weak.get(); if (current != null) current.accept(ticket, ready); });
        });
    }
    private static Bitmap artwork(File file, int width, int height) {
        try {
            if (!CoverImages.safe(file)) return null;
            BitmapFactory.Options options = new BitmapFactory.Options(); options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getPath(), options); int sample = 1;
            while (options.outWidth / (sample * 2) >= width || options.outHeight / (sample * 2) >= height) sample *= 2;
            options.inJustDecodeBounds = false; options.inSampleSize = sample; options.inPreferredConfig = Bitmap.Config.RGB_565;
            Bitmap decoded = BitmapFactory.decodeFile(file.getPath(), options); if (decoded == null) return null;
            Bitmap slot = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            float scale = Math.min(width / (float) decoded.getWidth(), height / (float) decoded.getHeight());
            float w = decoded.getWidth() * scale, h = decoded.getHeight() * scale;
            new Canvas(slot).drawBitmap(decoded, null, new RectF((width - w) / 2, (height - h) / 2, (width + w) / 2, (height + h) / 2), new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
            decoded.recycle(); return slot;
        } catch (RuntimeException badArtwork) { return null; }
    }
}
