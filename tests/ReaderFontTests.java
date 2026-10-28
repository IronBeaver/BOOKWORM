package com.bookworm.reader;

import android.content.*;
import java.util.*;

public final class ReaderFontTests {
    private static int checks;
    private static void check(boolean ok, String message) { checks++; if (!ok) throw new AssertionError(message); }
    private static final class Memory extends Context implements SharedPreferences, SharedPreferences.Editor {
        final Map<String,Object> values = new HashMap<>(), pending = new HashMap<>();
        boolean fail;
        public SharedPreferences getSharedPreferences(String name, int mode) { check(name.equals("reading"), "Existing preference store"); return this; }
        public String getString(String k, String f) { return values.containsKey(k) ? (String) values.get(k) : f; }
        public boolean getBoolean(String k, boolean f) { return values.containsKey(k) ? (Boolean) values.get(k) : f; }
        public int getInt(String k, int f) { return values.containsKey(k) ? (Integer) values.get(k) : f; }
        public Editor edit() { pending.clear(); return this; }
        public Editor putString(String k, String v) { pending.put(k, v); return this; }
        public Editor putBoolean(String k, boolean v) { pending.put(k, v); return this; }
        public Editor putInt(String k, int v) { pending.put(k, v); return this; }
        public boolean commit() { if (fail) return false; values.putAll(pending); return true; }
    }
    public static void main(String[] args) {
        if (args.length >= 1) {
            ReaderPreferences r = new ReaderPreferences(); r.fontId = args[0]; r.serif = true;
            r.paletteId = args.length > 2 ? args[2] : ReaderPalettes.FOLLOW;
            r.size = 20; r.margin = 20; r.spacing = 16;
            System.out.print(r.css(ThemeSettings.preset(args.length > 1 ? Integer.parseInt(args[1]) : 0, true, 18, false))); return;
        }
        Memory memory = new Memory();
        ReaderPreferences r = ReaderPreferences.load(memory);
        check(r.fontId.equals(ReaderFonts.SERIF), "New-user default remains system serif");
        check(r.size == 20 && r.margin == 20 && r.spacing == 16 && !r.paged, "Existing defaults");
        check(memory.values.isEmpty(), "Loading migration writes nothing");
        for (boolean legacy : new boolean[]{true, false}) {
            memory.values.clear(); memory.values.put("serif", legacy);
            memory.values.put("size", 27); memory.values.put("margin", 31); memory.values.put("spacing", 19);
            memory.values.put("paged", true); memory.values.put("controls_on_open", true); memory.values.put("show_progress", false);
            memory.values.put("future_key", "retained");
            r = ReaderPreferences.load(memory);
            check(r.fontId.equals(legacy ? ReaderFonts.SERIF : ReaderFonts.SANS), "Legacy serif/sans migration");
            check(!memory.values.containsKey("font_id"), "No eager migration write");
            for (int i = 0; i < ReaderFonts.count(); i++) {
                r.fontId = ReaderFonts.id(i); check(r.save(memory), "Preference transaction saved");
                r = ReaderPreferences.load(memory);
                check(r.fontId.equals(ReaderFonts.id(i)), "Each font survives restart");
                check(r.size == 27 && r.margin == 31 && r.spacing == 19 && r.paged && r.controlsOnOpen && !r.showProgress, "Other reader settings preserved");
                check("retained".equals(memory.values.get("future_key")), "Unrelated keys preserved");
            }
        }
        memory.values.clear(); memory.values.put("serif", false); memory.values.put("font_id", "unknown_future_font");
        check(ReaderPreferences.load(memory).fontId.equals(ReaderFonts.SANS), "Unknown font falls back to legacy setting");
        Map<String,Object> before = new HashMap<>(memory.values); memory.fail = true;
        check(!ReaderPreferences.load(memory).save(memory) && memory.values.equals(before), "Failed commit leaves store unchanged");
        String[] files = {"EBGaramond", "Literata", "AtkinsonHyperlegibleNext", "Bitter"};
        String[] faces = {"Regular", "Italic", "Bold", "BoldItalic"};
        for (int i = 0; i < ReaderFonts.count(); i++) {
            String id = ReaderFonts.id(i), css = ReaderFonts.css(id);
            if (i < 3) { check(css.isEmpty() && ReaderFonts.alias(id).isEmpty(), "System fonts load no assets"); continue; }
            check(css.split("@font-face", -1).length == 5, "Only selected family's four styles declared");
            for (String face : faces) {
                String path = "/__bookworm_fonts/" + files[i - 3] + '-' + face + ".ttf";
                String url = "https://bookworm.invalid" + path;
                check(ReaderFonts.asset(url, id).equals("fonts/" + files[i - 3] + '-' + face + ".ttf"), "Selected font route");
                for (int other = 0; other < ReaderFonts.count(); other++) if (other != i)
                    check(ReaderFonts.asset(url, ReaderFonts.id(other)) == null, "Other families cannot be served");
                for (String unsafe : new String[]{url + "?x=1", url + "#x", url.replace("https:", "http:"), url.replace("bookworm.invalid", "evil.example"), url.replace("bookworm.invalid", "user@bookworm.invalid"), url.replace("bookworm.invalid", "bookworm.invalid:443"), url.replace("/__bookworm_fonts/", "/__bookworm_fonts/../"), url.replace("/__bookworm_fonts/", "/%5f_bookworm_fonts/")})
                    check(ReaderFonts.asset(unsafe, id) == null, "Unsafe route rejected");
            }
            check(css.contains("font-weight:400;font-style:italic") && css.contains("font-weight:700;font-style:italic"), "Genuine italic styles mapped");
        }
        System.out.println("PASS: " + checks + " production preference migration/persistence and font route/style checks");
    }
}
