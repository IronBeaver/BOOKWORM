package com.bookworm.reader;

import java.io.*;
import java.util.Properties;

/** Independent cover preferences: no edits to EPUB, book metadata or reader state. */
public final class CoverChoice {
    public enum Policy { AUTO, ORIGINAL, LEATHER;
        public static Policy from(String key) {
            try { return valueOf(key); } catch (IllegalArgumentException | NullPointerException bad) { return AUTO; }
        }
    }
    public enum Override { GLOBAL, ORIGINAL, LEATHER }
    public static final String[] COLOR_NAMES = {"Burgundy", "Forest green", "Midnight navy", "Cognac", "Charcoal", "Plum"};
    public static final int[] COLORS = {0xff512632, 0xff1f3a30, 0xff202f49, 0xff65412b, 0xff303238, 0xff47314e};
    public static final int GOLD = 0xffe3c88d;
    public final Override override;
    /** -1 follows the deterministic identity colour; 0..5 chooses an explicit colour. */
    public final int color;
    public CoverChoice(Override override, int color) {
        this.override = override == null ? Override.GLOBAL : override;
        this.color = color >= 0 && color < COLORS.length ? color : -1;
    }
    public static CoverChoice defaults() { return new CoverChoice(Override.GLOBAL, -1); }
    public static int identityColor(String id) { return ((id == null ? "" : id).hashCode() & 0x7fffffff) % COLORS.length; }
    public int colorFor(String id) { return color < 0 ? identityColor(id) : color; }
    public Policy effective(Policy global) {
        if (override == Override.LEATHER) return Policy.LEATHER;
        if (override == Override.ORIGINAL) return Policy.ORIGINAL;
        return global == null ? Policy.AUTO : global;
    }
    public boolean useOriginal(Policy global, boolean usable) { return effective(global) != Policy.LEATHER && usable; }
    public static CoverChoice load(File directory) {
        try {
            Properties p = LibraryFiles.read(new File(directory, "cover.properties"));
            return new CoverChoice(Override.valueOf(p.getProperty("policy", "GLOBAL")), Integer.parseInt(p.getProperty("color", "-1")));
        } catch (IOException | IllegalArgumentException bad) { return defaults(); }
    }
    public void save(File directory) throws IOException {
        Properties p = new Properties(); p.setProperty("policy", override.name()); p.setProperty("color", Integer.toString(color));
        LibraryFiles.write(new File(directory, "cover.properties"), p);
    }
    public static String cacheKey(String id, String title, String author, String source, Policy global,
                                  CoverChoice choice, int width, int height) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            for (String part : new String[]{"bindery-1", id, title, author, source, global.name(), choice.override.name(),
                    Integer.toString(choice.colorFor(id)), width + "x" + height}) {
                byte[] bytes = (part == null ? "" : part).getBytes("UTF-8");
                digest.update(Integer.toString(bytes.length).getBytes("US-ASCII")); digest.update((byte) ':'); digest.update(bytes);
            }
            byte[] hash = digest.digest(); char[] out = new char[hash.length * 2];
            String digits = "0123456789abcdef";
            for (int i = 0; i < hash.length; i++) { out[i * 2] = digits.charAt((hash[i] & 255) >>> 4); out[i * 2 + 1] = digits.charAt(hash[i] & 15); }
            return new String(out);
        } catch (java.security.NoSuchAlgorithmException | UnsupportedEncodingException impossible) { throw new AssertionError(impossible); }
    }
}
