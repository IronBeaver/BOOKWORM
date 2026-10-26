package com.bookworm.reader;

import android.content.Context;
import android.content.SharedPreferences;

/** Single persistence boundary for interface and future reader themes. */
public final class ThemeStore {
    private ThemeStore() { }
    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences("appearance", Context.MODE_PRIVATE);
    }
    public static ThemeSettings load(Context context) {
        SharedPreferences p = preferences(context);
        ThemeSettings d = ThemeSettings.preset(0, true, 18, false);
        ThemeSettings saved = new ThemeSettings(p.getInt("background", d.background),
                p.getInt("text", d.text), p.getInt("accent", d.accent),
                p.getBoolean("serif", d.serif), p.getInt("size", d.fontSize),
                p.getBoolean("classic", d.classic));
        return saved.readable() ? saved : d;
    }
    public static boolean save(Context context, ThemeSettings value) {
        return save(context, value, CoverRepository.global(context));
    }
    public static boolean save(Context context, ThemeSettings value, CoverChoice.Policy coverPolicy) {
        return preferences(context).edit().putInt("background", value.background)
                .putInt("text", value.text).putInt("accent", value.accent)
                .putBoolean("serif", value.serif).putInt("size", value.fontSize)
                .putBoolean("classic", value.classic).putString("cover_policy", coverPolicy.name()).commit();
    }
}
