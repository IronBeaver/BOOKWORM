package com.bookworm.reader;

import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.regex.*;
import org.w3c.dom.*;

/** Rebuilds XHTML from an allowlist. Book scripts, handlers, SVG, forms and embeds never reach WebView. */
public final class EpubHtml {
    public static final String ORIGIN = "https://bookworm.invalid/";
    private static final Set<String> TAGS = new HashSet<>(Arrays.asList("p", "div", "span", "h1", "h2", "h3", "h4", "h5", "h6", "a", "img", "br", "hr", "em", "strong", "b", "i", "u", "s", "small", "sub", "sup", "blockquote", "pre", "code", "ul", "ol", "li", "dl", "dt", "dd", "table", "thead", "tbody", "tfoot", "tr", "td", "th", "caption", "section", "article", "header", "footer", "figure", "figcaption", "aside"));
    private static final Set<String> DROP = new HashSet<>(Arrays.asList("script", "iframe", "object", "embed", "form", "input", "button", "textarea", "select", "svg", "math", "audio", "video", "source", "canvas", "style", "link", "head"));
    private EpubHtml() { }
    public static String escape(String s) { return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;"); }
    public static String url(String id, String reference) throws IOException {
        String path = EpubParser.withoutFragment(reference); String fragment = reference.contains("#") ? reference.substring(reference.indexOf('#') + 1) : null;
        try { return new URI("https", "bookworm.invalid", "/" + id + "/" + path, null).toASCIIString() + (fragment == null ? "" : "#" + fragment); }
        catch (Exception e) { throw new IOException("Invalid local URL", e); }
    }
    public static String declarations(String input) {
        StringBuilder out = new StringBuilder();
        for (String rule : input.split(";")) {
            int colon = rule.indexOf(':'); if (colon < 0) continue;
            String key = rule.substring(0, colon).trim().toLowerCase(Locale.US), value = rule.substring(colon + 1).trim().toLowerCase(Locale.US);
            if (!key.matches("text-align|text-indent|font-style|font-weight|text-decoration|margin(-top|-bottom|-left|-right)?|padding(-top|-bottom|-left|-right)?|border(-width|-style)?")) continue;
            if (value.length() > 80 || !value.matches("[a-z0-9 .%+-]+")) continue;
            if (!key.equals("font-weight")) {
                Matcher numbers = Pattern.compile("[0-9]+(?:\\.[0-9]+)?").matcher(value); boolean huge = false;
                while (numbers.find()) if (Double.parseDouble(numbers.group()) > 100) huge = true;
                if (huge) continue;
            }
            out.append(key).append(':').append(value).append(';');
        }
        return out.toString();
    }
    public static String css(String input) {
        input = input.replaceAll("(?s)/\\*.*?\\*/", "");
        if (input.length() > 128 * 1024) return "";
        StringBuilder out = new StringBuilder(); int cursor = 0;
        while (cursor < input.length()) {
            int open = input.indexOf('{', cursor); if (open < 0) break;
            int close = input.indexOf('}', open); if (close < 0) break;
            String selector = input.substring(cursor, open).trim();
            if (selector.length() < 200 && selector.matches("[a-zA-Z0-9_.*# >:+~,\\-]+")) out.append(selector.replaceAll("#([a-zA-Z_][a-zA-Z0-9_-]*)", "#pub-$1")).append('{').append(declarations(input.substring(open + 1, close))).append('}');
            cursor = close + 1;
        }
        return out.toString();
    }
    public static String chapter(EpubBook book, String path, String readerCss) throws IOException {
        Document doc = EpubParser.xml(EpubParser.read(book.content(path), EpubParser.MAX_XML));
        StringBuilder styles = new StringBuilder();
        for (Element e : EpubParser.elements(doc, "style")) if (styles.length() < 128 * 1024) styles.append(css(e.getTextContent()));
        int sheets = 0;
        for (Element link : EpubParser.elements(doc, "link")) {
            if (++sheets > 16 || styles.length() > 128 * 1024) break;
            if (!link.getAttribute("rel").equalsIgnoreCase("stylesheet")) continue;
            try {
                String local = EpubParser.withoutFragment(EpubParser.resolve(path, link.getAttribute("href")));
                styles.append(css(new String(EpubParser.read(book.content(local), 128 * 1024), "UTF-8")));
            } catch (IOException ignored) { /* Missing/external optional CSS is omitted. */ }
        }
        StringBuilder body = new StringBuilder(); int[] count = {0};
        java.util.List<Element> bodies = EpubParser.elements(doc, "body");
        if (bodies.isEmpty()) throw new IOException("Chapter has no XHTML body");
        children(bodies.get(0), body, book, path, 0, count);
        return "<!doctype html><html><head><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1'>"
                + "<meta http-equiv='Content-Security-Policy' content=\"default-src 'none'; img-src https://bookworm.invalid; style-src 'unsafe-inline'; script-src 'none'; base-uri 'none'; form-action 'none'\">"
                + "<style>" + styles + readerCss + "</style></head><body><main id='bw-content'>" + body + "</main></body></html>";
    }
    private static void children(Node parent, StringBuilder out, EpubBook b, String path, int depth, int[] count) throws IOException {
        if (depth > 64) throw new IOException("Chapter nesting exceeds safety limit");
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (++count[0] > 50000) throw new IOException("Chapter has too many nodes");
            if (n.getNodeType() == Node.TEXT_NODE || n.getNodeType() == Node.CDATA_SECTION_NODE) { out.append(escape(n.getNodeValue())); continue; }
            if (!(n instanceof Element)) continue;
            Element e = (Element) n; String tag = EpubParser.name(e).toLowerCase(Locale.US);
            if (tag.equals("svg")) {
                // Older-reader Gutenberg cover pages wrap a raster cover in SVG.
                // Preserve only safe raster references as HTML images; never emit SVG or its scripts.
                NodeList images = e.getElementsByTagNameNS("*", "image");
                for (int i = 0; i < images.getLength(); i++) {
                    Element image = (Element) images.item(i);
                    String href = image.getAttributeNS("http://www.w3.org/1999/xlink", "href");
                    if (href.isEmpty()) href = image.getAttribute("href");
                    try {
                        String local = EpubParser.resolve(path, href);
                        if (EpubParser.isRaster(EpubParser.withoutFragment(local))) out.append("<img alt=\"Book illustration\" src=\"").append(escape(url(b.id, local))).append("\">");
                    } catch (IOException ignored) { /* Unsafe/external SVG references remain excluded. */ }
                }
                continue;
            }
            if (DROP.contains(tag)) continue;
            if (!TAGS.contains(tag)) { children(e, out, b, path, depth + 1, count); continue; }
            out.append('<').append(tag);
            // Prefix publisher IDs so they cannot collide with the reader's own container.
            if (e.hasAttribute("id")) out.append(" id=\"pub-").append(escape(e.getAttribute("id"))).append('"');
            if (e.hasAttribute("class") && e.getAttribute("class").matches("[a-zA-Z0-9_ \\-]{0,200}")) out.append(" class=\"").append(escape(e.getAttribute("class"))).append('"');
            String inline = declarations(e.getAttribute("style"));
            if (!inline.isEmpty()) out.append(" style=\"").append(inline).append('"');
            for (String attr : new String[]{"title", "alt"}) if (e.hasAttribute(attr)) out.append(' ').append(attr).append("=\"").append(escape(e.getAttribute(attr))).append('"');
            String attribute = tag.equals("a") ? "href" : tag.equals("img") ? "src" : "";
            if (!attribute.isEmpty() && e.hasAttribute(attribute)) {
                try {
                    String ref = EpubParser.resolve(path, e.getAttribute(attribute));
                    if (!tag.equals("img") || EpubParser.isRaster(EpubParser.withoutFragment(ref))) out.append(' ').append(attribute).append("=\"").append(escape(url(b.id, ref))).append('"');
                } catch (IOException ignored) { /* Untrusted external or unsafe reference is removed. */ }
            }
            out.append('>');
            if (!tag.equals("img") && !tag.equals("br") && !tag.equals("hr")) { children(e, out, b, path, depth + 1, count); out.append("</").append(tag).append('>'); }
        }
    }
}
