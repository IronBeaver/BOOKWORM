package com.bookworm.reader;

import java.awt.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Host visual QA of the shared design commands. Android uses its own Canvas/fonts, not Java2D. */
public final class BinderyPreview implements LeatherArt.Surface {
    final BufferedImage image;
    final Graphics2D canvas;
    public BinderyPreview(int width, int height) {
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB); canvas = image.createGraphics();
        canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        canvas.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        canvas.scale(width / 200.0, height / 300.0);
    }
    private Font font(float size) { return new Font("Serif", Font.PLAIN, 12).deriveFont(size); }
    public float width(String value, float size) { return value.isEmpty() ? 0 : new TextLayout(value, font(size), canvas.getFontRenderContext()).getAdvance(); }
    public float lineHeight(float size) { return font(size).getLineMetrics("Ag", canvas.getFontRenderContext()).getHeight() * 1.05f; }
    public void background(int color) {
        canvas.setPaint(new GradientPaint(0, 0, new Color(LeatherArt.shade(color, 8)), 200, 300, new Color(LeatherArt.shade(color, -9)))); canvas.fill(new Rectangle2D.Float(0, 0, 200, 300));
    }
    public void fill(int color, float x, float y, float width, float height) { canvas.setColor(new Color(color, true)); canvas.fill(new Rectangle2D.Float(x, y, width, height)); }
    public void line(int color, float stroke, float x, float y, float ex, float ey) { canvas.setColor(new Color(color, true)); canvas.setStroke(new BasicStroke(stroke)); canvas.draw(new Line2D.Float(x, y, ex, ey)); }
    public void curve(int color, float stroke, float x, float y, float a, float b, float c, float d, float ex, float ey) {
        canvas.setColor(new Color(color, true)); canvas.setStroke(new BasicStroke(stroke)); canvas.draw(new CubicCurve2D.Float(x, y, a, b, c, d, ex, ey));
    }
    public void text(CoverTypography.Layout layout, float x, float y, float width, float height) {
        Shape clip = canvas.getClip(); canvas.clip(new Rectangle2D.Float(x - 1, y - 1, width + 2, height + 2));
        float top = y + (height - layout.lines.size() * layout.lineHeight) / 2;
        float ascent = font(layout.size).getLineMetrics("Ag", canvas.getFontRenderContext()).getAscent();
        for (int i = 0; i < layout.lines.size(); i++) {
            String line = layout.lines.get(i); if (line.isEmpty()) continue;
            TextLayout text = new TextLayout(line, font(layout.size), canvas.getFontRenderContext());
            float left = x + (width - text.getAdvance()) / 2, baseline = top + i * layout.lineHeight + ascent;
            canvas.setColor(new Color(0xff69491f)); text.draw(canvas, left + .5f, baseline + .75f);
            canvas.setColor(new Color(CoverChoice.GOLD)); text.draw(canvas, left, baseline);
        }
        canvas.setClip(clip);
    }
    public static void main(String[] args) throws Exception {
        BufferedImage sheet = new BufferedImage(720, 784, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics(); g.setColor(new Color(0x171a19)); g.fillRect(0, 0, 720, 784);
        String[] titles = {"Frankenstein; or, The Modern Prometheus", "Twenty Thousand Leagues Under the Seas", "The Strange Case of Dr Jekyll and Mr Hyde", "Pride and Prejudice", "A Journey to the Centre of the Earth", "The Picture of Dorian Gray"};
        String[] authors = {"Mary Wollstonecraft Shelley", "Jules Verne", "Robert Louis Stevenson", "Jane Austen", "Jules Verne", "Oscar Wilde"};
        for (int i = 0; i < 6; i++) {
            BinderyPreview preview = new BinderyPreview(200, 300);
            LeatherArt.draw(preview, "preview-" + i, titles[i], authors[i], CoverChoice.COLORS[i]);
            preview.canvas.dispose(); int x = 20 + i % 3 * 240, y = 22 + i / 3 * 388;
            g.drawImage(preview.image, x, y, null); g.setColor(new Color(CoverChoice.GOLD)); g.setFont(new Font("SansSerif", Font.PLAIN, 14));
            g.drawString(CoverChoice.COLOR_NAMES[i], x, y + 325);
        }
        g.dispose(); File out = new File("app/build/bindery-preview.png"); ImageIO.write(sheet, "png", out);
        System.out.println(out.getAbsolutePath());
    }
}
