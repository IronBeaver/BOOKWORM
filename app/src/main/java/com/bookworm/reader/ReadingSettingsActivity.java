package com.bookworm.reader;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.widget.*;

public final class ReadingSettingsActivity extends ThemedActivity {
    private RadioGroup mode, font;
    private SeekBar size, margin, spacing;
    private ReaderPreferences draft;
    private CheckBox controls, progress;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); loadAppearance(); draft = ReaderPreferences.load(this);
        LinearLayout root = column(); root.addView(text("Reading appearance", true));
        root.addView(button("Back · discard unapplied changes", v -> finish()));
        ScrollView scroll = new ScrollView(this); LinearLayout form = new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL);
        form.addView(text("Apply changes to reflow the current chapter around your saved text position.", false));
        form.addView(text("Reading mode", true));
        mode = choices(form, "Continuous chapter scroll", R.id.mode_scroll, "Traditional pages · tap screen edges", R.id.mode_pages);
        mode.check(draft.paged ? R.id.mode_pages : R.id.mode_scroll);
        form.addView(text("Reader controls", true));
        controls = check(form, "Show controls when opening a book", R.id.reader_controls_default, draft.controlsOnOpen);
        progress = check(form, "Show subtle reading progress", R.id.reader_show_progress, draft.showProgress);
        form.addView(text("Centre tap toggles controls. In page mode, the left and right regions turn pages. Links and text selection take priority. The Controls button and hardware Back remain available.", false));
        form.addView(text("Reading font", true));
        font = choices(form, "Serif", R.id.read_serif, "Sans-serif", R.id.read_sans);
        font.check(draft.serif ? R.id.read_serif : R.id.read_sans);
        size = slider(form, "Font size", 14, 32, draft.size, R.id.read_size, "px");
        margin = slider(form, "Page margins", 8, 40, draft.margin, R.id.read_margin, "px");
        spacing = slider(form, "Line spacing (tenths)", 12, 22, draft.spacing, R.id.read_spacing, " / 10");
        form.addView(button("Colours · open Appearance", v -> startActivity(new Intent(this, AppearanceActivity.class))));
        form.addView(text("The reader reuses your saved background, text and accent colours. Colour changes are saved separately in Appearance.", false));
        scroll.addView(form); root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        root.addView(button("Apply reading settings", v -> {
            draft.paged = mode.getCheckedRadioButtonId() == R.id.mode_pages;
            draft.controlsOnOpen = controls.isChecked(); draft.showProgress = progress.isChecked();
            draft.serif = font.getCheckedRadioButtonId() == R.id.read_serif;
            draft.size = size.getProgress() + 14; draft.margin = margin.getProgress() + 8; draft.spacing = spacing.getProgress() + 12;
            if (draft.save(this)) finish(); else Toast.makeText(this, "Could not save reading settings", Toast.LENGTH_LONG).show();
        }));
        setContentView(root);
        if (state != null) {
            mode.check(state.getInt("mode")); font.check(state.getInt("font"));
            size.setProgress(state.getInt("size")); margin.setProgress(state.getInt("margin")); spacing.setProgress(state.getInt("spacing"));
            controls.setChecked(state.getBoolean("controls", draft.controlsOnOpen)); progress.setChecked(state.getBoolean("progress", draft.showProgress));
        }
    }
    private RadioGroup choices(LinearLayout form, String a, int aid, String b, int bid) {
        RadioGroup group = new RadioGroup(this); String[] labels = {a,b}; int[] ids = {aid,bid};
        for (int i = 0; i < 2; i++) {
            RadioButton option = new RadioButton(this); option.setId(ids[i]); option.setText(labels[i]); style(option);
            option.setMinHeight(dp(48)); option.setButtonTintList(ColorStateList.valueOf(appearance.accent)); group.addView(option);
        }
        form.addView(group); return group;
    }
    private CheckBox check(LinearLayout form, String label, int id, boolean value) {
        CheckBox control = new CheckBox(this); control.setId(id); control.setText(label); style(control); control.setMinHeight(dp(48));
        control.setButtonTintList(ColorStateList.valueOf(appearance.accent)); control.setChecked(value); form.addView(control); return control;
    }
    private SeekBar slider(LinearLayout form, String name, int min, int max, int value, int id, String suffix) {
        TextView label = text(name + ": " + value + suffix, false); form.addView(label);
        SeekBar control = new SeekBar(this); control.setId(id); control.setMax(max - min); control.setProgress(value - min);
        control.setMinimumHeight(dp(48)); control.setProgressTintList(ColorStateList.valueOf(appearance.accent)); control.setThumbTintList(ColorStateList.valueOf(appearance.accent));
        control.setContentDescription(label.getText());
        control.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar bar, int progress, boolean user) { String text = name + ": " + (progress + min) + suffix; label.setText(text); bar.setContentDescription(text); }
            public void onStartTrackingTouch(SeekBar bar) { }
            public void onStopTrackingTouch(SeekBar bar) { }
        });
        form.addView(control); return control;
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putInt("mode", mode.getCheckedRadioButtonId()); state.putInt("font", font.getCheckedRadioButtonId());
        state.putInt("size", size.getProgress()); state.putInt("margin", margin.getProgress()); state.putInt("spacing", spacing.getProgress());
        state.putBoolean("controls", controls.isChecked()); state.putBoolean("progress", progress.isChecked());
        super.onSaveInstanceState(state);
    }
}
