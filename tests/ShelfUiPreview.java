package com.bookworm.reader;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;

/** Illustrative desktop-font mockup of the UI geometry, not an Android screenshot. */
public final class ShelfUiPreview {
    private static final String[] TITLES = {"Frankenstein; or, The Modern Prometheus", "Pride and Prejudice", "Twenty Thousand Leagues Under the Seas", "The Picture of Dorian Gray", "A Journey to the Centre of the Earth", "The Strange Case of Dr Jekyll and Mr Hyde"};
    private static final String[] AUTHORS = {"Mary Wollstonecraft Shelley", "Jane Austen", "Jules Verne", "Oscar Wilde", "Jules Verne", "Robert Louis Stevenson"};
    private static String ellipsis(Graphics2D g, String value, int width) {
        if (g.getFontMetrics().stringWidth(value) <= width) return value;
        while (!value.isEmpty() && g.getFontMetrics().stringWidth(value + "…") > width) value = value.substring(0, value.length() - 1);
        return value + "…";
    }
    private static void panel(Graphics2D parent, int x, int preset) {
        Graphics2D g = (Graphics2D) parent.create(); g.translate(x, 0); g.clipRect(0, 0, 600, 960);
        ThemeSettings t = ThemeSettings.preset(preset, true, 18, preset == 1);
        g.setColor(new Color(t.background)); g.fillRect(0, 0, 600, 960);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(t.text)); g.setFont(new Font("Serif", Font.PLAIN, 18)); g.drawString("BOOKWORM", 20, 35); g.setStroke(new BasicStroke(1.7f));
        for (int i = 0; i < 3; i++) g.drawLine(460, 21 + i * 6, 476 - i * 4, 21 + i * 6);
        g.drawLine(516, 17, 516, 33); g.drawLine(512, 29, 516, 33); g.drawLine(516, 33, 520, 29); g.drawLine(508, 36, 524, 36); g.drawLine(508, 36, 508, 32); g.drawLine(524, 36, 524, 32);
        Path2D gear = new Path2D.Float();
        for (int i = 0; i < 32; i++) { double a = i * Math.PI / 16, r = i % 4 == 0 || i % 4 == 3 ? 9 : 7.3; double px = 564 + Math.cos(a) * r, py = 28 + Math.sin(a) * r; if (i == 0) gear.moveTo(px, py); else gear.lineTo(px, py); }
        gear.closePath(); g.draw(gear); g.draw(new Ellipse2D.Float(561, 25, 6, 6));
        g.setColor(new Color(ShelfPresentation.muted(t))); g.setFont(new Font("Serif", Font.PLAIN, 12)); g.drawString("6 books  ·  Title A–Z", 20, 73);
        String[] ordered = {"4", "0", "1", "5", "3", "2"};
        int top = 80;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 2; col++) {
                int i = Integer.parseInt(ordered[row * 2 + col]), base = 12 + col * 288, left = base + 74;
                BinderyPreview cover = new BinderyPreview(140, 210);
                LeatherArt.draw(cover, "preview-" + i, TITLES[i], AUTHORS[i], CoverChoice.COLORS[i]); cover.canvas.dispose();
                g.setColor(new Color(0, 0, 0, 30)); g.fillRect(left + 1, top + 18, 140, 210); g.drawImage(cover.image, left, top + 16, null);
                g.setColor(new Color(t.text)); g.setFont(new Font("Serif", Font.PLAIN, 15));
                String first = TITLES[i]; int split = first.length();
                while (split > 0 && g.getFontMetrics().stringWidth(first.substring(0, split)) > 140) split--;
                int space = first.lastIndexOf(' ', split); if (space > 0 && split < first.length()) split = space;
                g.drawString(first.substring(0, split), left, top + 249);
                if (split < first.length()) g.drawString(ellipsis(g, first.substring(split).trim(), 140), left, top + 267);
                g.setColor(new Color(ShelfPresentation.muted(t))); g.setFont(new Font("Serif", Font.PLAIN, 13)); g.drawString(ellipsis(g, AUTHORS[i], 140), left, top + 286);
                g.setColor(new Color(t.text)); for (int dy : new int[]{-7,0,7}) g.fill(new Ellipse2D.Float(base + 254.5f, top + 253 + dy, 3, 3));
            }
            top += 309;
            if (t.classic) {
                int wood = ShelfPresentation.blend(t.background, 0xff8d6847, .30f);
                g.setColor(new Color(wood)); g.fillRect(12, top, 576, 8);
                g.setColor(new Color(ShelfPresentation.blend(wood, t.accent, .20f))); g.fillRect(12, top, 576, 1); top += 8;
            }
        }
        g.dispose();
    }
    public static void main(String[] args) throws Exception {
        BufferedImage image = new BufferedImage(1800, 960, BufferedImage.TYPE_INT_RGB); Graphics2D g = image.createGraphics();
        for (int p = 0; p < 3; p++) panel(g, p * 600, p); g.dispose();
        File output = new File("app/build/bookshelf-ui-preview.png"); ImageIO.write(image, "png", output); System.out.println(output.getAbsolutePath());
    }
}
