package com.bookworm.reader;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.*;

/** Navigation only: existing screens remain the sole owners of their preferences. */
public final class SettingsActivity extends ThemedActivity {
    @Override protected void onCreate(Bundle state) { super.onCreate(state); }
    @Override protected void onResume() {
        super.onResume(); loadAppearance();
        LinearLayout root = column(); root.setPadding(dp(12), 0, dp(12), dp(12)); root.addView(ShelfUi.toolbar(this, "Settings", true));
        ScrollView scroll = new ScrollView(this); LinearLayout groups = new LinearLayout(this); groups.setOrientation(LinearLayout.VERTICAL);
        section(groups, "Appearance", "Theme & Bindery", "Colours, interface type and global cover policy", () -> startActivity(new Intent(this, AppearanceActivity.class)));
        section(groups, "Bookshelf", "Shelf presentation & depth", "Grid, classic covers or experimental spines; dimensional cover effects", () -> startActivity(new Intent(this, AppearanceActivity.class).putExtra("focus_bookshelf", true)));
        section(groups, "Reading", "Reading appearance", "Mode, font, size, margins and line spacing", () -> startActivity(new Intent(this, ReadingSettingsActivity.class)));
        section(groups, "About", "About BOOKWORM", "Application information and Frankenstein attribution", this::about);
        scroll.addView(groups); root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1)); setContentView(root);
    }
    private void section(LinearLayout parent, String heading, String title, String detail, Runnable open) {
        TextView label = text(heading, false); label.setTextSize(13); label.setTextColor(appearance.accent); label.setPadding(dp(12), dp(22), dp(12), dp(8)); parent.addView(label);
        LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.VERTICAL); row.setPadding(dp(12), dp(10), dp(12), dp(10)); row.setMinimumHeight(dp(64));
        TextView name = text(title, false); name.setTextSize(16); name.setPadding(0, 0, 0, dp(4)); row.addView(name);
        TextView description = text(detail, false); description.setTextSize(13); description.setTextColor(ShelfPresentation.muted(appearance)); description.setPadding(0, 0, 0, 0); row.addView(description);
        row.setBackground(ShelfUi.feedback(this)); row.setFocusable(true); row.setClickable(true); row.setContentDescription(title + ". " + detail); row.setOnClickListener(v -> open.run()); parent.addView(row);
    }
    private void about() {
        String version;
        try { version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName; }
        catch (android.content.pm.PackageManager.NameNotFoundException impossible) { version = ""; }
        LinearLayout content = column(); content.addView(text("BOOKWORM " + version + "\n" + getString(R.string.welcome_subtitle) + "\n\nAn offline EPUB reader. Frankenstein is included at no charge from Project Gutenberg. Its original notice and complete license are available offline.", false));
        ScrollView scroll = new ScrollView(this); scroll.addView(content);
        AlertDialog dialog = new AlertDialog.Builder(this).setCustomTitle(ShelfUi.dialogTitle(this, "About BOOKWORM")).setView(scroll)
                .setPositiveButton("Frankenstein · source & license", (d, which) -> startActivity(new Intent(this, BundledBookActivity.class)))
                .setNegativeButton("Close", null).create(); ShelfUi.show(this, dialog);
    }
}
