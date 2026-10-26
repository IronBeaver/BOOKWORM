package com.bookworm.reader;

import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.view.View;
import android.widget.*;

/** Per-book native controls and an immediately updated, unsaved preview. */
public final class BinderyDialog {
    private BinderyDialog() { }
    public static void show(ThemedActivity owner, EpubBook book, Runnable changed) {
        CoverChoice saved = CoverRepository.choice(book);
        CoverChoice.Policy global = CoverRepository.global(owner);
        ScrollView scroll = new ScrollView(owner); LinearLayout form = owner.column(); scroll.addView(form);
        form.addView(owner.text(book.title, true));
        form.addView(owner.text(CoverTypography.normalized(book.author, "Unknown author"), false));
        CoverThumbnailView preview = new CoverThumbnailView(owner);
        form.addView(preview, new LinearLayout.LayoutParams(-1, owner.dp(216)));
        form.addView(owner.text("Cover policy", false)); RadioGroup policy = new RadioGroup(owner);
        String[] names = {"Follow global policy (" + global.name() + ")", "Use original artwork", "Use generated leather"};
        int[] policyIds = {R.id.cover_follow, R.id.cover_original, R.id.cover_leather};
        for (int i = 0; i < names.length; i++) policy.addView(option(owner, names[i], policyIds[i]));
        form.addView(policy); policy.check(policyIds[saved.override.ordinal()]);
        form.addView(owner.text("Leather colour · used when a generated cover is shown", false));
        RadioGroup colors = new RadioGroup(owner);
        int[] colorIds = {R.id.cover_identity, R.id.cover_burgundy, R.id.cover_forest, R.id.cover_navy,
                R.id.cover_cognac, R.id.cover_charcoal, R.id.cover_plum};
        colors.addView(option(owner, "Identity colour · " + CoverChoice.COLOR_NAMES[CoverChoice.identityColor(book.id)], colorIds[0]));
        for (int i = 0; i < CoverChoice.COLOR_NAMES.length; i++) colors.addView(option(owner, CoverChoice.COLOR_NAMES[i], colorIds[i + 1]));
        form.addView(colors); colors.check(colorIds[saved.color + 1]);
        java.util.concurrent.atomic.AtomicReference<CoverChoice> draft = new java.util.concurrent.atomic.AtomicReference<>(saved);
        Runnable update = () -> {
            int selectedPolicy = 0, selectedColor = -1;
            for (int i = 0; i < policyIds.length; i++) if (policy.getCheckedRadioButtonId() == policyIds[i]) selectedPolicy = i;
            for (int i = 0; i < colorIds.length; i++) if (colors.getCheckedRadioButtonId() == colorIds[i]) selectedColor = i - 1;
            CoverChoice value = new CoverChoice(CoverChoice.Override.values()[selectedPolicy], selectedColor);
            draft.set(value); preview.bind(book, global, value);
        };
        policy.setOnCheckedChangeListener((group, checked) -> update.run());
        colors.setOnCheckedChangeListener((group, checked) -> update.run()); preview.bind(book, global, saved);
        form.addView(owner.text("Apply saves this book only. Reset book restores global policy and its identity colour. Cancel discards changes.", false));
        AlertDialog dialog = new AlertDialog.Builder(owner).setTitle("The BOOKWORM Bindery").setView(scroll)
                .setPositiveButton("Apply", null).setNeutralButton("Reset book", null).setNegativeButton("Cancel", null).create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> save(owner, book, draft.get(), changed, dialog));
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> save(owner, book, CoverChoice.defaults(), changed, dialog));
    }
    private static RadioButton option(ThemedActivity owner, String title, int id) {
        RadioButton result = new RadioButton(owner); result.setId(id); result.setText(title); owner.style(result);
        result.setMinHeight(owner.dp(48)); result.setButtonTintList(ColorStateList.valueOf(owner.appearance.accent)); return result;
    }
    private static void save(ThemedActivity owner, EpubBook book, CoverChoice value, Runnable changed, AlertDialog dialog) {
        try { CoverRepository.save(book, value); changed.run(); dialog.dismiss(); }
        catch (java.io.IOException failed) { Toast.makeText(owner, "Could not save cover choice. Please try again.", Toast.LENGTH_LONG).show(); }
    }
}
