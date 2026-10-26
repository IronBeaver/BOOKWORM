package com.bookworm.reader;
import android.app.Activity;
import android.content.*;
import android.os.Bundle;
import android.widget.LinearLayout;
import java.io.*;
import java.util.concurrent.Executors;

/** SAF selection and bounded copying, without storage permission. */
public final class ImportActivity extends ThemedActivity {
    private static final int PICK = 30;
    private boolean importing;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); loadAppearance(); show("Choose an unencrypted reflowable EPUB.");
        if (state == null) choose();
    }
    private void show(String message) {
        LinearLayout root = column(); root.addView(text("Import EPUB", true));
        root.addView(text(message, false));
        if (!importing) {
            root.addView(button("Choose EPUB", v -> choose()));
            root.addView(button("Alternate file picker", v -> alternate()));
        }
        root.addView(button("Back to bookshelf", v -> finish())); setContentView(root);
    }
    private void choose() {
        Intent intent = pickerIntent(Intent.ACTION_OPEN_DOCUMENT);
        try { startActivityForResult(intent, PICK); }
        catch (ActivityNotFoundException absent) {
            alternate();
        }
    }
    private void alternate() {
        try { startActivityForResult(pickerIntent(Intent.ACTION_GET_CONTENT), PICK); }
        catch (ActivityNotFoundException absent) { show("No document picker is available on this device."); }
    }
    private static Intent pickerIntent(String action) {
        // Older providers may report EPUBs as ZIP, generic binary, or an unknown MIME type.
        // Do not narrow this with EXTRA_MIME_TYPES: validate the selected stream in EpubParser.
        return new Intent(action).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE);
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data); if (request != PICK) return;
        if (result != Activity.RESULT_OK || data == null || data.getData() == null) { show("No file selected. You can try the alternate picker."); return; }
        importing = true;
        show("Validating and copying EPUB into private storage…");
        android.net.Uri uri = data.getData(); android.content.Context app = getApplicationContext();
        java.util.concurrent.ExecutorService worker = Executors.newSingleThreadExecutor();
        worker.execute(() -> {
            try {
                java.util.List<EpubBook> before = LibraryStore.list(app); EpubBook imported;
                try (InputStream input = app.getContentResolver().openInputStream(uri)) {
                    if (input == null) throw new IOException("Document provider could not open this file");
                    imported = EpubParser.importBook(input, LibraryStore.root(app));
                }
                boolean duplicate = false; for (EpubBook b : before) if (b.id.equals(imported.id)) duplicate = true;
                final String message = duplicate ? "Already in your library: " + imported.title : "Imported: " + imported.title;
                runOnUiThread(() -> { LibraryStore.notifyChanged(); importing = false; if (!isFinishing() && !isDestroyed()) show(message); });
            } catch (Exception failed) {
                final String message = "Could not import this EPUB: " + failed.getMessage();
                runOnUiThread(() -> { importing = false; if (!isFinishing() && !isDestroyed()) show(message); });
            } finally { worker.shutdown(); }
        });
    }
}
