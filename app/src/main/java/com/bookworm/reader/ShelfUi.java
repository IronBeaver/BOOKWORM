package com.bookworm.reader;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;

/** Compact themed controls, confined to shelf/settings UI. */
public final class ShelfUi {
    private ShelfUi() { }
    public static StateListDrawable feedback(ThemedActivity owner) {
        StateListDrawable states = new StateListDrawable();
        int pressed = ShelfPresentation.blend(owner.appearance.background, owner.appearance.accent, .16f);
        if (ThemeSettings.contrast(owner.appearance.text, pressed) < 4.5) pressed = owner.appearance.background;
        GradientDrawable outline = new GradientDrawable(); outline.setColor(pressed); outline.setStroke(owner.dp(1), owner.appearance.accent);
        states.addState(new int[]{android.R.attr.state_pressed}, outline);
        GradientDrawable focus = new GradientDrawable(); focus.setColor(pressed); focus.setStroke(owner.dp(1), owner.appearance.accent);
        states.addState(new int[]{android.R.attr.state_focused}, focus);
        states.addState(new int[]{}, new ColorDrawable(owner.appearance.background)); return states;
    }
    public static ImageButton action(ThemedActivity owner, ShelfIcon.Kind icon, String label, View.OnClickListener click) {
        ImageButton view = new ImageButton(owner); view.setImageDrawable(new ShelfIcon(icon, owner.appearance.text, owner.dp(24)));
        view.setScaleType(ImageView.ScaleType.CENTER); view.setPadding(owner.dp(12), owner.dp(12), owner.dp(12), owner.dp(12));
        view.setBackground(feedback(owner)); view.setBackgroundTintList(null);
        view.setMinimumWidth(owner.dp(48)); view.setMinimumHeight(owner.dp(48)); view.setContentDescription(label);
        view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES); view.setOnClickListener(click);
        view.setOnLongClickListener(v -> { Toast.makeText(owner, label, Toast.LENGTH_SHORT).show(); return true; }); return view;
    }
    public static LinearLayout toolbar(ThemedActivity owner, String title, boolean back) {
        LinearLayout bar = new LinearLayout(owner); bar.setGravity(Gravity.CENTER_VERTICAL); bar.setMinimumHeight(owner.dp(56));
        if (back) bar.addView(action(owner, ShelfIcon.Kind.BACK, "Back to bookshelf", v -> owner.finish()), new LinearLayout.LayoutParams(owner.dp(48), owner.dp(48)));
        TextView label = owner.text(title, false); label.setTextSize(18); label.setSingleLine(true); label.setEllipsize(android.text.TextUtils.TruncateAt.END);
        label.setContentDescription(title); label.setPadding(owner.dp(8), 0, owner.dp(4), 0);
        bar.addView(label, new LinearLayout.LayoutParams(0, -2, 1)); return bar;
    }
    public static android.widget.ArrayAdapter<String> menuAdapter(ThemedActivity owner, String[] items, boolean radio) {
        return new ArrayAdapter<String>(owner, radio ? android.R.layout.simple_list_item_single_choice : android.R.layout.simple_list_item_1, items) {
            @Override public View getView(int position, View reused, ViewGroup parent) {
                TextView row = (TextView) super.getView(position, reused, parent); owner.style(row); row.setTextSize(Math.min(18, owner.appearance.fontSize));
                row.setMinHeight(owner.dp(48)); row.setBackground(feedback(owner));
                if (row instanceof CheckedTextView) ((CheckedTextView) row).setCheckMarkTintList(android.content.res.ColorStateList.valueOf(owner.appearance.accent));
                return row;
            }
        };
    }
    public static TextView dialogTitle(ThemedActivity owner, String title) {
        TextView label = owner.text(title, false); label.setTextSize(18); label.setMaxLines(2); label.setEllipsize(android.text.TextUtils.TruncateAt.END);
        label.setPadding(owner.dp(20), owner.dp(20), owner.dp(20), owner.dp(12)); return label;
    }
    public static void show(ThemedActivity owner, AlertDialog dialog) {
        dialog.show(); dialog.getWindow().setBackgroundDrawable(new ColorDrawable(owner.appearance.background));
        for (int which : new int[]{DialogInterface.BUTTON_POSITIVE, DialogInterface.BUTTON_NEGATIVE, DialogInterface.BUTTON_NEUTRAL}) {
            Button button = dialog.getButton(which); if (button != null) { button.setTextColor(owner.appearance.accent); button.setMinHeight(owner.dp(48)); }
        }
    }
}
