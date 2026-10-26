package com.bookworm.reader;

import android.app.Activity;
import android.graphics.Typeface;
import android.content.res.ColorStateList;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Platform-view helpers; colours and typography come from the shared model. */
public abstract class ThemedActivity extends Activity {
    protected ThemeSettings appearance;
    protected int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    protected void loadAppearance() {
        appearance = ThemeStore.load(this);
        getWindow().setStatusBarColor(appearance.background);
        getWindow().setNavigationBarColor(appearance.background);
    }
    protected void style(TextView view) {
        view.setTextColor(appearance.text);
        view.setTextSize(appearance.fontSize);
        view.setTypeface(appearance.serif ? Typeface.SERIF : Typeface.SANS_SERIF);
    }
    protected TextView text(String value, boolean heading) {
        TextView view = new TextView(this);
        view.setText(value); style(view);
        if (heading) view.setTextSize(appearance.fontSize + 8);
        view.setPadding(0, dp(8), 0, dp(8));
        return view;
    }
    protected Button button(String label, android.view.View.OnClickListener action) {
        Button button = new Button(this);
        button.setText(label); button.setAllCaps(false); style(button);
        button.setMinHeight(dp(48));
        button.setTextColor(appearance.accent);
        button.setBackgroundTintList(ColorStateList.valueOf(appearance.background));
        button.setOnClickListener(action);
        return button;
    }
    protected LinearLayout column() {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setPadding(dp(20), dp(12), dp(20), dp(20));
        view.setBackgroundColor(appearance.background);
        view.setFitsSystemWindows(true);
        return view;
    }
}
