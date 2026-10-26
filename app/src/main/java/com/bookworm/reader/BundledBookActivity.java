package com.bookworm.reader;
import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import java.io.*;

/** Original notice and full license are accessible offline before and during reading. */
public final class BundledBookActivity extends ThemedActivity {
    private boolean full;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); loadAppearance(); show(state != null && state.getBoolean("full"));
    }
    private String asset(String name) throws IOException {
        try (InputStream in = getAssets().open("books/" + name); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] bytes = new byte[4096]; int n;
            while ((n = in.read(bytes)) != -1) { if (out.size() + n > 128 * 1024) throw new IOException("License asset too large"); out.write(bytes, 0, n); }
            return out.toString("UTF-8");
        }
    }
    private void show(boolean license) {
        full = license; LinearLayout root = column(); root.addView(text(license ? "Project Gutenberg license" : "Frankenstein · source and license", true));
        root.addView(button("Back", v -> { if (full) show(false); else finish(); }));
        ScrollView scroll = new ScrollView(this);
        try { TextView notice = text(asset(license ? "gutenberg-license.txt" : "gutenberg-notice.txt"), false); notice.setTextIsSelectable(true); scroll.addView(notice); }
        catch (IOException missing) { scroll.addView(text("Could not open offline notice: " + missing.getMessage(), false)); }
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        if (!license) {
            root.addView(button("Full license · offline", v -> show(true)));
            String id = getIntent().getStringExtra("book");
            if (getIntent().getBooleanExtra("read", false) && id != null) root.addView(button("Read Frankenstein", v -> {
                startActivity(new Intent(this, ReaderActivity.class).putExtra("book", id)); finish();
            }));
        }
        setContentView(root);
    }
    @Override public void onBackPressed() { if (full) show(false); else super.onBackPressed(); }
    @Override protected void onSaveInstanceState(Bundle state) { state.putBoolean("full", full); super.onSaveInstanceState(state); }
}
