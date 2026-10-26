package com.bookworm.reader;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

/** Native controls, explicit apply, and no changes written by Back navigation. */
public final class AppearanceActivity extends ThemedActivity {
    private EditText background, foreground, accent;
    private RadioGroup fonts, layouts;
    private RadioGroup covers;
    private android.widget.CheckBox depth;
    private SeekBar size;
    private TextView sizeLabel, error;
    private static final int SERIF = R.id.font_serif, SANS = R.id.font_sans,
            GRID = R.id.layout_grid, CLASSIC = R.id.layout_classic, SPINES = R.id.layout_spines;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); loadAppearance();
        LinearLayout root = column();
        root.addView(text("Appearance", true));
        root.addView(button("Back · discard unapplied changes", v -> finish()));
        ScrollView scroll = new ScrollView(this);
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.addView(text("Choose a preset or edit the colours below. Changes are saved only when you tap Apply.", false));
        form.addView(text("Colour presets", true));
        String[] names = {"Midnight", "Parchment", "Forest"};
        for (int i = 0; i < names.length; i++) {
            final int index = i;
            form.addView(button(names[i], v -> {
                setColors(ThemeSettings.preset(index, true, 18, false));
                error.setText(R.string.preset_selected);
            }));
        }
        background = colorField(form, "Background colour", R.id.color_background);
        foreground = colorField(form, "Text colour", R.id.color_text);
        accent = colorField(form, "Accent colour", R.id.color_accent);
        setColors(appearance);
        form.addView(text("Interface font", true));
        fonts = options(form, "Serif", SERIF, "Sans-serif", SANS);
        fonts.check(appearance.serif ? SERIF : SANS);
        sizeLabel = text("", false); form.addView(sizeLabel);
        size = new SeekBar(this);
        size.setId(R.id.font_size); size.setMax(10); size.setProgress(appearance.fontSize - 14);
        size.setMinimumHeight(dp(48));
        size.setProgressTintList(ColorStateList.valueOf(appearance.accent));
        size.setThumbTintList(ColorStateList.valueOf(appearance.accent));
        size.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar bar, int progress, boolean user) { updateSizeLabel(); }
            public void onStartTrackingTouch(SeekBar bar) { }
            public void onStopTrackingTouch(SeekBar bar) { }
        });
        form.addView(size); updateSizeLabel();
        form.addView(text("Bookshelf layout", true));
        layouts = options(form, "Minimalist grid", GRID, "Classic shelves", CLASSIC);
        RadioButton spines=new RadioButton(this); spines.setId(SPINES); spines.setText("Classic shelves · spines (experimental)"); style(spines);
        spines.setMinHeight(dp(48)); spines.setButtonTintList(ColorStateList.valueOf(appearance.accent)); layouts.addView(spines);
        BookshelfPreferences shelf=BookshelfPreferences.load(this,appearance.classic);
        layouts.check(shelf.presentation==PhysicalShelf.Presentation.SPINES ? SPINES : shelf.presentation==PhysicalShelf.Presentation.COVERS ? CLASSIC : GRID);
        depth=new android.widget.CheckBox(this); depth.setId(R.id.cover_depth); depth.setText("Dimensional cover edges and shadows"); style(depth);
        depth.setMinHeight(dp(48)); depth.setButtonTintList(ColorStateList.valueOf(appearance.accent)); depth.setChecked(shelf.depth); form.addView(depth);
        form.addView(text("Spine view is experimental. Tap a spine to select, then Read; long press or use the panel menu for options. Full metadata is available in Book details.",false));
        form.addView(text("The BOOKWORM Bindery", true));
        form.addView(text("Global cover policy. AUTO and ORIGINAL both prefer usable EPUB art and otherwise generate leather. Each book can override this policy in Cover options.", false));
        covers = new RadioGroup(this);
        String[] coverLabels = {"AUTO · artwork, then leather", "ORIGINAL · artwork with leather fallback", "LEATHER · generated bindings"};
        int[] coverIds = {R.id.global_cover_auto, R.id.global_cover_original, R.id.global_cover_leather};
        for (int i = 0; i < coverIds.length; i++) {
            RadioButton option = new RadioButton(this); option.setId(coverIds[i]); option.setText(coverLabels[i]); style(option);
            option.setMinHeight(dp(48)); option.setButtonTintList(ColorStateList.valueOf(appearance.accent)); covers.addView(option);
        }
        covers.check(coverIds[CoverRepository.global(this).ordinal()]); form.addView(covers);
        form.addView(text("Custom text and accent colours need contrast of at least 4.5:1 against the background.", false));
        error = text("", false);
        error.setAccessibilityLiveRegion(android.view.View.ACCESSIBILITY_LIVE_REGION_POLITE);
        form.addView(error);
        scroll.addView(form);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        root.addView(button("Apply and return to bookshelf", v -> apply()));
        setContentView(root);
        if (state == null && getIntent().getBooleanExtra("focus_bookshelf", false)) {
            scroll.post(() -> scroll.scrollTo(0, Math.max(0, layouts.getTop() - dp(48))));
        }
        if (state != null) {
            background.setText(state.getString("draftBackground"));
            foreground.setText(state.getString("draftText"));
            accent.setText(state.getString("draftAccent"));
            fonts.check(state.getInt("draftFont", SERIF));
            layouts.check(state.getInt("draftLayout", GRID));
            size.setProgress(state.getInt("draftSize", 4));
            covers.check(state.getInt("draftCover", coverIds[CoverRepository.global(this).ordinal()]));
            depth.setChecked(state.getBoolean("draftDepth",shelf.depth));
        }
    }

    private EditText colorField(LinearLayout form, String label, int id) {
        TextView caption = text(label + " (#RRGGBB)", false);
        caption.setLabelFor(id); form.addView(caption);
        EditText field = new EditText(this);
        field.setId(id); style(field); field.setSingleLine(true);
        field.setMinHeight(dp(48));
        field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        field.setFilters(new InputFilter[]{new InputFilter.LengthFilter(7)});
        field.setBackgroundTintList(ColorStateList.valueOf(appearance.accent));
        form.addView(field); return field;
    }
    private RadioGroup options(LinearLayout form, String a, int aid, String b, int bid) {
        RadioGroup group = new RadioGroup(this);
        String[] labels = {a, b}; int[] ids = {aid, bid};
        for (int i = 0; i < 2; i++) {
            RadioButton option = new RadioButton(this);
            option.setId(ids[i]); option.setText(labels[i]); style(option);
            option.setMinHeight(dp(48));
            option.setButtonTintList(ColorStateList.valueOf(appearance.accent));
            group.addView(option);
        }
        form.addView(group); return group;
    }
    private void setColors(ThemeSettings value) {
        background.setText(ThemeSettings.hex(value.background));
        foreground.setText(ThemeSettings.hex(value.text));
        accent.setText(ThemeSettings.hex(value.accent));
    }
    private void updateSizeLabel() {
        String label = "Interface size: " + (size.getProgress() + 14) + " sp";
        sizeLabel.setText(label); size.setContentDescription(label);
    }
    private Integer readColor(EditText field) {
        try { return ThemeSettings.parseHex(field.getText().toString()); }
        catch (IllegalArgumentException invalid) {
            field.setError(invalid.getMessage()); field.requestFocus(); return null;
        }
    }
    private void apply() {
        Integer bg = readColor(background); if (bg == null) return;
        Integer fg = readColor(foreground); if (fg == null) return;
        Integer ac = readColor(accent); if (ac == null) return;
        ThemeSettings value = new ThemeSettings(bg, fg, ac, fonts.getCheckedRadioButtonId() == SERIF,
                size.getProgress() + 14, layouts.getCheckedRadioButtonId() != GRID);
        if (!value.readable()) {
            error.setText(R.string.low_contrast);
            return;
        }
        CoverChoice.Policy policy = covers.getCheckedRadioButtonId() == R.id.global_cover_leather ? CoverChoice.Policy.LEATHER
                : covers.getCheckedRadioButtonId() == R.id.global_cover_original ? CoverChoice.Policy.ORIGINAL : CoverChoice.Policy.AUTO;
        if (!ThemeStore.save(this, value, policy)) {
            error.setText(R.string.save_failed); return;
        }
        PhysicalShelf.Presentation presentation=layouts.getCheckedRadioButtonId()==SPINES ? PhysicalShelf.Presentation.SPINES
                : layouts.getCheckedRadioButtonId()==CLASSIC ? PhysicalShelf.Presentation.COVERS : PhysicalShelf.Presentation.GRID;
        if(!new BookshelfPreferences(presentation,depth.isChecked()).save(this)) { error.setText(R.string.save_failed); return; }
        Toast.makeText(this, "Appearance saved", Toast.LENGTH_SHORT).show(); finish();
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putString("draftBackground", background.getText().toString());
        state.putString("draftText", foreground.getText().toString());
        state.putString("draftAccent", accent.getText().toString());
        state.putInt("draftFont", fonts.getCheckedRadioButtonId());
        state.putInt("draftLayout", layouts.getCheckedRadioButtonId());
        state.putInt("draftSize", size.getProgress());
        state.putInt("draftCover", covers.getCheckedRadioButtonId());
        state.putBoolean("draftDepth",depth.isChecked());
        super.onSaveInstanceState(state);
    }
}
