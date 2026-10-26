package com.bookworm.reader;

import java.text.BreakIterator;
import java.util.*;

/** Bounded, grapheme-aware line wrapping and fitting, using the renderer's actual font metrics. */
public final class CoverTypography {
    public interface Metrics { float width(String text, float size); float lineHeight(float size); }
    public static final class Layout {
        public final List<String> lines;
        public final float size, lineHeight;
        public final boolean truncated;
        Layout(List<String> lines, float size, float lineHeight, boolean truncated) {
            this.lines = Collections.unmodifiableList(lines); this.size = size; this.lineHeight = lineHeight; this.truncated = truncated;
        }
    }
    private CoverTypography() { }
    public static String normalized(String text, String fallback) {
        if (text == null || text.trim().isEmpty()) return fallback;
        return text.replaceAll("[\\s\\p{Z}]+", " ").trim();
    }
    public static Layout fit(String text, String fallback, float width, float height, float min, float max, Metrics metrics) {
        text = normalized(text, fallback); boolean limited = false;
        if (text.length() > 2048) { text = text.substring(0, 2048); if (Character.isHighSurrogate(text.charAt(text.length() - 1))) text = text.substring(0, text.length() - 1); limited = true; }
        float low = min, high = max;
        for (int i = 0; i < 9; i++) {
            float size = (low + high) / 2;
            if (wrap(text, width, size, metrics).size() * metrics.lineHeight(size) <= height) low = size; else high = size;
        }
        float size = low; List<String> lines = wrap(text, width, size, metrics);
        int count = Math.max(1, (int) Math.floor(height / metrics.lineHeight(size)));
        boolean truncated = limited || lines.size() > count;
        if (lines.size() > count) lines = new ArrayList<>(lines.subList(0, count));
        if (truncated) {
            String last = lines.get(lines.size() - 1);
            BreakIterator clusters = BreakIterator.getCharacterInstance(Locale.ROOT);
            while (!last.isEmpty() && metrics.width(last + "…", size) > width) {
                clusters.setText(last); int previous = clusters.preceding(last.length()); last = previous < 0 ? "" : last.substring(0, previous);
            }
            lines.set(lines.size() - 1, last + "…");
        }
        return new Layout(lines, size, metrics.lineHeight(size), truncated);
    }
    private static List<String> wrap(String text, float width, float size, Metrics metrics) {
        List<String> lines = new ArrayList<>(); String line = "";
        BreakIterator words = BreakIterator.getLineInstance(Locale.ROOT); words.setText(text);
        int start = words.first();
        for (int end = words.next(); end != BreakIterator.DONE; start = end, end = words.next()) {
            String word = text.substring(start, end);
            if (metrics.width((line + word).trim(), size) <= width) { line += word; continue; }
            if (!line.trim().isEmpty()) { lines.add(line.trim()); line = ""; }
            String separator = word.endsWith(" ") ? " " : "";
            word = word.trim();
            if (metrics.width(word, size) <= width) { line = word + separator; continue; }
            BreakIterator clusters = BreakIterator.getCharacterInstance(Locale.ROOT); clusters.setText(word);
            int a = clusters.first();
            for (int b = clusters.next(); b != BreakIterator.DONE; a = b, b = clusters.next()) {
                String cluster = word.substring(a, b);
                if (!line.isEmpty() && metrics.width(line + cluster, size) > width) { lines.add(line); line = ""; }
                line += cluster;
            }
            line += separator;
        }
        if (!line.trim().isEmpty()) lines.add(line.trim());
        if (lines.isEmpty()) lines.add("");
        return lines;
    }
}
