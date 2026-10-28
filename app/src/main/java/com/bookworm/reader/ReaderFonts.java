package com.bookworm.reader;

import java.net.URI;
import java.net.URISyntaxException;

/** Stable preference IDs and exact packaged-font routes; no runtime font service. */
public final class ReaderFonts {
    public static final String SERIF = "system_serif", SANS = "system_sans", MONO = "system_monospace";
    private static final String[] IDS = {SERIF, SANS, MONO, "eb_garamond", "literata", "atkinson_next", "bitter"};
    private static final String[] LABELS = {"System Serif", "System Sans", "System Monospace", "EB Garamond", "Literata", "Atkinson Hyperlegible Next", "Bitter"};
    private static final String[] FILES = {"", "", "", "EBGaramond", "Literata", "AtkinsonHyperlegibleNext", "Bitter"};
    private static final String[] FACES = {"Regular", "Italic", "Bold", "BoldItalic"};
    private static final String PREFIX = "/__bookworm_fonts/";

    public static int count() { return IDS.length; }
    public static String id(int index) { return IDS[index]; }
    public static String label(int index) { return LABELS[index]; }
    public static String selected(String id, boolean legacySerif) {
        for (String known : IDS) if (known.equals(id)) return known;
        return legacySerif ? SERIF : SANS;
    }
    private static int index(String id) {
        for (int i = 0; i < IDS.length; i++) if (IDS[i].equals(id)) return i;
        return 0;
    }
    public static String alias(String id) { int i = index(id); return i < 3 ? "" : "BWReader" + FILES[i]; }
    public static String family(String id) {
        if (MONO.equals(id)) return "monospace";
        if (SANS.equals(id)) return "sans-serif";
        String alias = alias(id);
        return alias.isEmpty() ? "serif" : alias + ("atkinson_next".equals(id) ? ",sans-serif" : ",serif");
    }
    public static String css(String id) {
        int i = index(id); if (i < 3) return "";
        StringBuilder css = new StringBuilder();
        for (int face = 0; face < FACES.length; face++) {
            css.append("@font-face{font-family:").append(alias(id)).append(";font-weight:")
                .append(face >= 2 ? "700" : "400").append(";font-style:")
                .append(face % 2 == 1 ? "italic" : "normal")
                .append(";src:url('https://bookworm.invalid").append(PREFIX).append(FILES[i]).append('-')
                .append(FACES[face]).append(".ttf') format('truetype');}");
        }
        return css.toString();
    }
    /** Only the selected family's four immutable assets may be served, never EPUB fonts. */
    public static String asset(String url, String selected) {
        int i = index(selected); if (i < 3) return null;
        try {
            URI uri = new URI(url);
            if (!"https".equals(uri.getScheme()) || !"bookworm.invalid".equals(uri.getRawAuthority())
                    || uri.getRawQuery() != null || uri.getRawFragment() != null) return null;
            for (String face : FACES) {
                String file = FILES[i] + '-' + face + ".ttf";
                if ((PREFIX + file).equals(uri.getRawPath())) return "fonts/" + file;
            }
        } catch (URISyntaxException | NullPointerException invalid) { /* Fail closed. */ }
        return null;
    }
    public static String license(int index) { return "font-licenses/" + FILES[index] + "-OFL.txt"; }
    private ReaderFonts() { }
}
