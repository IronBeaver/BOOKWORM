package com.bookworm.reader;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.*;

public final class EpubTests {
    private static int checks;
    private static void check(boolean condition, String message) { checks++; if (!condition) throw new AssertionError(message); }
    static Map<String, byte[]> fixture(boolean epub3) throws Exception {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        put(entries, "mimetype", "application/epub+zip");
        put(entries, "META-INF/container.xml", "<container xmlns='urn:oasis:names:tc:opendocument:xmlns:container'><rootfiles><rootfile full-path='OPS/book.opf'/></rootfiles></container>");
        put(entries, "OPS/book.opf", "<package xmlns='http://www.idpf.org/2007/opf' version='" + (epub3 ? "3.0" : "2.0") + "'><metadata xmlns:dc='http://purl.org/dc/elements/1.1/'><dc:title>Local Test Book</dc:title><dc:creator>Ada Test</dc:creator><meta name='cover' content='cover'/></metadata><manifest><item id='b' href='two.xhtml' media-type='application/xhtml+xml'/><item id='a' href='one.xhtml' media-type='application/xhtml+xml'/><item id='cover' href='cover.png' media-type='image/png' properties='cover-image'/><item id='nav' href='nav.xhtml' media-type='application/xhtml+xml' properties='nav'/><item id='ncx' href='toc.ncx' media-type='application/x-dtbncx+xml'/></manifest><spine toc='ncx'><itemref idref='a'/><itemref idref='b'/></spine></package>");
        put(entries, "OPS/one.xhtml", "<!DOCTYPE html SYSTEM 'https://invalid.example/never.dtd'><html xmlns='http://www.w3.org/1999/xhtml'><body><h1 id='start'>First</h1><p>Hello reader.</p><a href='two.xhtml#next'>Next</a></body></html>");
        put(entries, "OPS/two.xhtml", "<html xmlns='http://www.w3.org/1999/xhtml'><body><h1 id='next'>Second</h1></body></html>");
        put(entries, "OPS/nav.xhtml", "<html xmlns='http://www.w3.org/1999/xhtml' xmlns:epub='http://www.idpf.org/2007/ops'><body><nav epub:type='toc'><a href='one.xhtml#start'>Opening</a><a href='two.xhtml'>Closing</a></nav></body></html>");
        put(entries, "OPS/toc.ncx", "<ncx xmlns='http://www.daisy.org/z3986/2005/ncx/'><navMap><navPoint><navLabel><text>NCX opening</text></navLabel><content src='one.xhtml#start'/></navPoint></navMap></ncx>");
        entries.put("OPS/cover.png", new byte[]{(byte)137,80,78,71});
        if (!epub3) entries.remove("OPS/nav.xhtml");
        return entries;
    }
    private static void put(Map<String, byte[]> e, String key, String value) throws Exception { e.put(key, value.getBytes("UTF-8")); }
    static byte[] archive(Map<String, byte[]> entries) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            for (Map.Entry<String, byte[]> e : entries.entrySet()) {
                ZipEntry entry = new ZipEntry(e.getKey());
                if (e.getKey().equals("mimetype")) { CRC32 crc = new CRC32(); crc.update(e.getValue()); entry.setMethod(ZipEntry.STORED); entry.setSize(e.getValue().length); entry.setCrc(crc.getValue()); }
                zip.putNextEntry(entry); zip.write(e.getValue()); zip.closeEntry();
            }
        }
        return out.toByteArray();
    }
    private static void reject(Map<String, byte[]> fixture, File root, String reason) throws Exception {
        boolean rejected = false;
        try { EpubParser.importBook(new ByteArrayInputStream(archive(fixture)), root); }
        catch (IOException expected) { rejected = true; }
        check(rejected, reason);
        File[] stages = root.listFiles((dir, name) -> name.startsWith(".import-"));
        check(stages != null && stages.length == 0, "Failed import cleaned up");
    }
    public static void main(String[] args) throws Exception {
        File root = new File("app/build/epub-tests-" + UUID.randomUUID());
        if (!root.mkdirs()) throw new IOException("Cannot create test directory");
        try {
            byte[] valid = archive(fixture(true));
            check(EpubParser.xml("<p>&nbsp;&eacute;&mdash;</p>".getBytes("UTF-8")).getDocumentElement().getTextContent().equals("\u00a0\u00e9\u2014"), "XHTML named entities without fetching a DTD");
            check(EpubParser.xml("<p><![CDATA[&nbsp;]]></p>".getBytes("UTF-8")).getDocumentElement().getTextContent().equals("&nbsp;"), "CDATA stays literal");
            check(EpubParser.xml("<?xml version='1.0' encoding='UTF-16'?><p>Unicode</p>".getBytes("UTF-16")).getDocumentElement().getTextContent().equals("Unicode"), "UTF-16 XML");
            EpubBook b = EpubParser.importBook(new ByteArrayInputStream(valid), root);
            check(b.title.equals("Local Test Book") && b.author.equals("Ada Test"), "Metadata");
            check(b.chapters.equals(Arrays.asList("OPS/one.xhtml", "OPS/two.xhtml")), "Spine order, not manifest order");
            check(b.cover.equals("OPS/cover.png"), "Raster cover metadata");
            check(b.tocLabels.equals(Arrays.asList("Opening", "Closing")), "EPUB 3 navigation");
            check(EpubBook.load(b.directory).chapters.equals(b.chapters), "Library survives reload");
            String html = EpubHtml.chapter(b, b.chapters.get(0), "body{color:white;}");
            check(html.contains("Hello reader.") && html.contains("pub-start"), "Sanitized body and publisher anchors");
            check(html.contains("https://bookworm.invalid/" + b.id + "/OPS/two.xhtml#next"), "Internal link resolves to private book origin");
            check(!EpubHtml.declarations("background:url(file:///secret);position:fixed;color:red;text-align:center").contains("url"), "CSS removes external resources");
            check(EpubHtml.declarations("text-align:center;font-style:italic").contains("font-style:italic"), "Basic CSS retained");
            File firstChapter = b.content(b.chapters.get(0));
            try (OutputStream out = new FileOutputStream(firstChapter)) {
                out.write(("<html xmlns='http://www.w3.org/1999/xhtml'><body onload='evil()'><script>evil()</script><iframe src='file:///private'/><svg xmlns='http://www.w3.org/2000/svg' xmlns:xlink='http://www.w3.org/1999/xlink'><script>evil()</script><image xlink:href='cover.png' onload='evil()'/><image xlink:href='https://evil.test/a.png'/></svg><p onclick='evil()'>Safe</p><img src='https://evil.test/a.png'/><a href='javascript:evil()'>link</a></body></html>").getBytes("UTF-8"));
            }
            html = EpubHtml.chapter(b, b.chapters.get(0), "");
            check(!html.contains("evil") && !html.contains("onclick") && !html.contains("onload"), "Scripts, handlers and external references removed");
            check(html.contains("<p>Safe</p>"), "Safe content survives sanitizing");
            check(html.contains("/OPS/cover.png") && !html.contains("<svg") && !html.contains("<image"), "SVG raster wrapper becomes a safe local HTML image only");
            ReadingPosition position = new ReadingPosition(b.chapters.get(0), 1234); position.save(b.directory);
            ReadingPosition restored = ReadingPosition.load(b.directory, "fallback");
            check(restored.offset == 1234 && restored.chapter.equals(position.chapter), "Durable text anchor persistence");
            new ReadingPosition(b.chapters.get(1), 92).save(b.directory);
            restored = ReadingPosition.load(b.directory, "fallback");
            check(restored.offset == 92 && restored.chapter.equals(b.chapters.get(1)), "Position update survives reload");
            try (OutputStream out = new FileOutputStream(new File(b.directory, "position.properties"))) { out.write("offset=bad".getBytes("UTF-8")); }
            check(ReadingPosition.load(b.directory, "fallback").offset == 1234, "Interrupted/corrupt position recovers previous anchor");
            check(new ReadingPosition("a", -99).offset == 0, "Negative text offset bounded");
            EpubBook duplicate = EpubParser.importBook(new ByteArrayInputStream(valid), root);
            check(duplicate.id.equals(b.id) && root.list().length == 1, "Duplicate is not copied");
            File arbitraryName = new File("app/build", "picker-source-" + UUID.randomUUID() + ".bin");
            try {
                try (OutputStream out = new FileOutputStream(arbitraryName)) { out.write(valid); }
                try (InputStream in = new FileInputStream(arbitraryName)) {
                    check(EpubParser.importBook(in, root).id.equals(b.id), "Valid EPUB accepted despite a non-EPUB source filename");
                }
            } finally { if (!arbitraryName.delete()) throw new IOException("Could not clean source fixture"); }
            File misleadingName = new File("app/build", "picker-source-" + UUID.randomUUID() + ".epub");
            try {
                try (OutputStream out = new FileOutputStream(misleadingName)) { out.write("This is not a ZIP or EPUB.".getBytes("UTF-8")); }
                boolean rejected = false;
                try (InputStream in = new FileInputStream(misleadingName)) { EpubParser.importBook(in, root); }
                catch (IOException expected) { rejected = true; }
                check(rejected, "Non-EPUB content rejected despite .epub filename");
                check(root.list().length == 1, "Invalid broad-picker selection leaves library unchanged and staging cleaned");
            } finally { if (!misleadingName.delete()) throw new IOException("Could not clean source fixture"); }
            EpubBook epub2 = EpubParser.importBook(new ByteArrayInputStream(archive(fixture(false))), root);
            check(epub2.tocLabels.get(0).equals("NCX opening"), "EPUB 2 NCX fallback");
            check(EpubParser.resolve("OPS/chapter.xhtml", "../images/a%20b.png").equals("images/a b.png"), "Relative decoded resource");
            for (String path : new String[]{"../outside", "/absolute", "C:/outside", "a\\b", "a/../b", "a/./b"}) {
                boolean bad = false; try { EpubParser.safeFile(root, path); } catch (IOException expected) { bad = true; }
                check(bad, "Unsafe ZIP path rejected: " + path);
            }
            for (String path : new String[]{"https://evil.test/a", "//evil.test/a", "../../outside", "%2e%2e/%2e%2e/outside", "file:///private"}) {
                boolean bad = false; try { EpubParser.resolve("OPS/book.opf", path); } catch (IOException expected) { bad = true; }
                check(bad, "Unsafe reference rejected: " + path);
            }
            Map<String, byte[]> bad = fixture(true); put(bad, "../outside", "bad"); reject(bad, root, "ZIP traversal");
            bad = fixture(true); bad.remove("META-INF/container.xml"); reject(bad, root, "Missing container");
            bad = fixture(true); put(bad, "mimetype", "application/zip"); reject(bad, root, "Wrong mimetype");
            bad = fixture(true); put(bad, "META-INF/encryption.xml", "<encryption/>"); reject(bad, root, "Encryption rejected");
            bad = fixture(true); put(bad, "OPS/one.xhtml", "<!DOCTYPE x [<!ENTITY e 'boom'>]><html>&e;</html>"); reject(bad, root, "Entity expansion rejected");
            bad = fixture(true); bad.put("huge.txt", new byte[2 * 1024 * 1024]); reject(bad, root, "Compression bomb");
            bad = fixture(true); bad.remove("OPS/two.xhtml"); reject(bad, root, "Missing spine resource");
            bad = fixture(true); put(bad, "OPS/one.xhtml", "<html xmlns='http://www.w3.org/1999/xhtml'><head/></html>"); reject(bad, root, "Missing chapter body rejected during import");
            bad = fixture(true); put(bad, "OPS/one.xhtml", "<html><body>" + String.join("", Collections.nCopies(100, "<div>")) + "deep" + String.join("", Collections.nCopies(100, "</div>")) + "</body></html>"); reject(bad, root, "Excessive XML nesting rejected before DOM");
            bad = fixture(true); put(bad, "OPS//one.xhtml", "<html/>"); reject(bad, root, "Canonical ZIP alias rejected");
            bad = fixture(true); put(bad, "OPS/book.opf", new String(bad.get("OPS/book.opf"), "UTF-8").replace("<metadata", "<metadata").replace("</metadata>", "<meta property='rendition:layout'>pre-paginated</meta></metadata>")); reject(bad, root, "Fixed layout rejected");
            // Leave a genuine EPUB ZIP fixture for independent inspection or later manual import.
            File fixtureOut = new File("app/build/test-fixtures"); fixtureOut.mkdirs();
            try (OutputStream out = new FileOutputStream(new File(fixtureOut, "local-test.epub"))) { out.write(valid); }
            System.out.println("Passed " + checks + " EPUB checks.");
        } finally { EpubParser.deleteTree(root); }
    }
}
