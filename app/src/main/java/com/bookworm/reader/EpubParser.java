package com.bookworm.reader;

import java.io.*;
import java.net.URI;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

/** Bounded ZIP/XML importer. No external entity, network or archive executable is used. */
public final class EpubParser {
    public static final long MAX_ARCHIVE = 64L * 1024 * 1024, MAX_TOTAL = 128L * 1024 * 1024;
    public static final int MAX_ENTRY = 16 * 1024 * 1024, MAX_XML = 2 * 1024 * 1024;
    private EpubParser() { }
    public static File safeFile(File root, String path) throws IOException {
        if (path == null || path.isEmpty() || path.length() > 1024 || path.startsWith("/") || path.contains("//") || path.contains("\\")
                || path.contains(":") || path.indexOf('\0') >= 0) throw new IOException("Unsafe EPUB path");
        for (String part : path.split("/")) if (part.equals("..") || part.equals(".")) throw new IOException("Unsafe EPUB path");
        File f = new File(root, path).getCanonicalFile();
        if (!f.getPath().startsWith(root.getCanonicalPath() + File.separator)) throw new IOException("EPUB path escapes book");
        return f;
    }
    /** Resolve percent-encoded OPF/XHTML references, allowing .. only within the archive. */
    public static String resolve(String base, String href) throws IOException {
        try {
            if (href.contains("\\")) throw new IOException("Unsafe reference");
            URI ref = new URI(href.replace(" ", "%20"));
            if (ref.isAbsolute() || ref.getRawAuthority() != null || ref.getRawQuery() != null) throw new IOException("External reference");
            URI resolved = new URI("https", "local.invalid", "/" + base, null).resolve(ref);
            String path = resolved.getPath();
            if (path == null || path.contains("\\") || path.contains(":")) throw new IOException("Unsafe reference");
            // Decode before normalization so encoded traversal cannot bypass the root boundary.
            String parent = "";
            for (String part : path.split("/")) {
                if (part.isEmpty() || part.equals(".")) continue;
                if (part.equals("..")) {
                    int cut = parent.lastIndexOf('/');
                    if (parent.isEmpty()) throw new IOException("Reference escapes book");
                    parent = cut < 0 ? "" : parent.substring(0, cut);
                } else parent += (parent.isEmpty() ? "" : "/") + part;
            }
            if (parent.isEmpty()) throw new IOException("Empty reference");
            return parent + (resolved.getRawFragment() == null ? "" : "#" + resolved.getRawFragment());
        } catch (java.net.URISyntaxException e) { throw new IOException("Invalid EPUB reference", e); }
    }
    public static String withoutFragment(String path) { int p = path.indexOf('#'); return p < 0 ? path : path.substring(0, p); }
    public static byte[] read(File file, int limit) throws IOException {
        try (InputStream in = new FileInputStream(file); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192]; int n;
            while ((n = in.read(buffer)) != -1) { if (out.size() + n > limit) throw new IOException("EPUB resource exceeds safety limit"); out.write(buffer, 0, n); }
            return out.toByteArray();
        }
    }
    public static Document xml(byte[] data) throws IOException {
        try {
            // EPUB XHTML often has an external DTD; resolve it to nothing. Internal entities are never accepted.
            String encoding = "UTF-8";
            if (data.length >= 2 && ((data[0] & 255) == 255 && (data[1] & 255) == 254 || data[0] == '<' && data[1] == 0)) encoding = "UTF-16LE";
            else if (data.length >= 2 && ((data[0] & 255) == 254 && (data[1] & 255) == 255 || data[0] == 0 && data[1] == '<')) encoding = "UTF-16BE";
            else {
                String header = new String(data, 0, Math.min(200, data.length), "US-ASCII");
                java.util.regex.Matcher declaration = java.util.regex.Pattern.compile("^\\s*<\\?xml[^>]*encoding\\s*=\\s*['\"]([^'\"]+)['\"]").matcher(header);
                if (declaration.find()) encoding = declaration.group(1);
            }
            String probe = java.nio.charset.Charset.forName(encoding).newDecoder()
                    .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(data)).toString();
            if (probe.startsWith("\ufeff")) probe = probe.substring(1);
            if (probe.indexOf('\0') >= 0 || probe.toUpperCase(Locale.US).contains("<!ENTITY")
                    || probe.matches("(?s).*<!DOCTYPE[^>]*\\[.*")) throw new IOException("Internal XML entities are unsupported");
            String expanded = HtmlEntities.expand(probe);
            // Enforce depth before building a DOM, so deeply nested input cannot exhaust a parser's call stack.
            javax.xml.parsers.SAXParserFactory saxFactory = javax.xml.parsers.SAXParserFactory.newInstance();
            org.xml.sax.XMLReader preflight = saxFactory.newSAXParser().getXMLReader();
            preflight.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));
            org.xml.sax.helpers.DefaultHandler guard = new org.xml.sax.helpers.DefaultHandler() {
                int depth, count;
                @Override public void startElement(String uri, String local, String qualified, org.xml.sax.Attributes attributes) throws org.xml.sax.SAXException {
                    count += 1 + attributes.getLength();
                    if (++depth > 64 || count > 50000) throw new org.xml.sax.SAXException("XML node/depth safety limit exceeded");
                }
                @Override public void endElement(String uri, String local, String qualified) { depth--; }
                @Override public void fatalError(org.xml.sax.SAXParseException e) throws org.xml.sax.SAXException { throw e; }
                @Override public void error(org.xml.sax.SAXParseException e) throws org.xml.sax.SAXException { throw e; }
            };
            preflight.setContentHandler(guard); preflight.setErrorHandler(guard);
            preflight.parse(new InputSource(new StringReader(expanded)));
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true); factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));
            builder.setErrorHandler(new org.xml.sax.helpers.DefaultHandler() {
                @Override public void fatalError(org.xml.sax.SAXParseException e) throws org.xml.sax.SAXException { throw e; }
                @Override public void error(org.xml.sax.SAXParseException e) throws org.xml.sax.SAXException { throw e; }
            });
            Document doc = builder.parse(new InputSource(new StringReader(expanded)));
            int count = 0, depth = 0; Node cursor = doc;
            while (cursor != null) {
                if (++count > 50000 || depth > 64) throw new IOException("XML node/depth safety limit exceeded");
                if (cursor.getFirstChild() != null) { cursor = cursor.getFirstChild(); depth++; }
                else {
                    while (cursor != null && cursor.getNextSibling() == null) { cursor = cursor.getParentNode(); depth--; }
                    if (cursor != null) cursor = cursor.getNextSibling();
                }
            }
            return doc;
        } catch (Exception e) { throw new IOException("Invalid or unsupported EPUB XML: " + e.getMessage(), e); }
    }
    static String name(Node n) { return n.getLocalName() == null ? n.getNodeName() : n.getLocalName(); }
    static List<Element> elements(Document d, String name) {
        List<Element> result = new ArrayList<>(); NodeList nodes = d.getElementsByTagNameNS("*", name);
        for (int i = 0; i < nodes.getLength(); i++) result.add((Element) nodes.item(i)); return result;
    }
    public static synchronized EpubBook importBook(InputStream source, File library) throws IOException {
        if (!library.isDirectory() && !library.mkdirs()) throw new IOException("Cannot create private library");
        // Only one import runs at a time. Discard staging folders left by an interrupted process.
        File[] stale = library.listFiles((dir, name) -> name.matches("\\.import-[0-9a-f-]{36}"));
        if (stale != null) for (File abandoned : stale) deleteTree(abandoned);
        File stage = new File(library, ".import-" + UUID.randomUUID());
        if (!stage.mkdir()) throw new IOException("Cannot create import folder");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            File archive = new File(stage, "original.epub");
            try (InputStream in = source; OutputStream out = new FileOutputStream(archive)) {
                byte[] buffer = new byte[8192]; int n; long total = 0;
                while ((n = in.read(buffer)) != -1) {
                    total += n; if (total > MAX_ARCHIVE) throw new IOException("EPUB exceeds 64 MiB compressed limit");
                    digest.update(buffer, 0, n); out.write(buffer, 0, n);
                }
            }
            StringBuilder hash = new StringBuilder();
            for (byte b : digest.digest()) hash.append(String.format(Locale.US, "%02x", b & 255));
            File destination = new File(library, hash.toString());
            if (new File(destination, "book.properties").isFile()) return EpubBook.load(destination);
            EpubBook book = parse(archive, stage); book.id = hash.toString(); book.directory = stage; book.save();
            if (!stage.renameTo(destination)) throw new IOException("Cannot commit imported book");
            book.directory = destination; return book;
        } catch (java.security.NoSuchAlgorithmException impossible) { throw new IOException(impossible); }
        finally { if (stage.exists()) deleteTree(stage); }
    }
    static void deleteTree(File root) throws IOException {
        File[] children = root.listFiles(); if (children != null) for (File f : children) deleteTree(f);
        if (!root.delete()) throw new IOException("Could not remove incomplete import");
    }
    static EpubBook parse(File archive, File stage) throws IOException {
        File content = new File(stage, "content"); if (!content.mkdir()) throw new IOException("Cannot extract book");
        Set<String> names = new HashSet<>(); long total = 0; int count = 0;
        try (ZipFile zip = new ZipFile(archive)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (++count > 2048) throw new IOException("EPUB has too many ZIP entries");
                File target = safeFile(content, entry.getName());
                if (!names.add(entry.getName().toLowerCase(Locale.US))) throw new IOException("Duplicate ZIP path");
                if (entry.isDirectory()) { if (!target.isDirectory() && !target.mkdirs()) throw new IOException("Invalid ZIP directory"); continue; }
                if (entry.getSize() > MAX_ENTRY) throw new IOException("EPUB entry exceeds 16 MiB");
                if (!target.getParentFile().isDirectory() && !target.getParentFile().mkdirs()) throw new IOException("Invalid ZIP parent");
                CRC32 crc = new CRC32(); int size = 0;
                try (InputStream in = zip.getInputStream(entry); OutputStream out = new FileOutputStream(target)) {
                    byte[] buffer = new byte[8192]; int n;
                    while ((n = in.read(buffer)) != -1) {
                        size += n; total += n;
                        if (size > MAX_ENTRY || total > MAX_TOTAL) throw new IOException("EPUB decompression safety limit exceeded");
                        crc.update(buffer, 0, n); out.write(buffer, 0, n);
                    }
                }
                if (crc.getValue() != entry.getCrc()) throw new IOException("EPUB ZIP checksum mismatch");
                if (size > 1024 * 1024 && entry.getCompressedSize() > 0 && size / entry.getCompressedSize() > 200) throw new IOException("Suspicious ZIP compression ratio");
            }
        }
        if (!new String(read(safeFile(content, "mimetype"), 100), "US-ASCII").trim().equals("application/epub+zip")) throw new IOException("Not an EPUB archive");
        if (new File(content, "META-INF/encryption.xml").exists()) throw new IOException("Encrypted/DRM or obfuscated-font EPUBs are unsupported");
        Document container = xml(read(safeFile(content, "META-INF/container.xml"), MAX_XML));
        List<Element> roots = elements(container, "rootfile");
        if (roots.isEmpty()) throw new IOException("EPUB container has no package");
        String opf = roots.get(0).getAttribute("full-path");
        Document packageDoc = xml(read(safeFile(content, opf), MAX_XML));
        String version = packageDoc.getDocumentElement().getAttribute("version");
        if (!version.startsWith("2.") && !version.startsWith("3.")) throw new IOException("Only EPUB 2 and 3 are supported");
        for (Element meta : elements(packageDoc, "meta")) {
            if (meta.getAttribute("property").equals("rendition:layout") && meta.getTextContent().trim().equals("pre-paginated")) throw new IOException("Fixed-layout EPUB is unsupported");
        }
        EpubBook book = new EpubBook(); book.directory = stage;
        List<Element> titles = elements(packageDoc, "title"), authors = elements(packageDoc, "creator");
        if (!titles.isEmpty()) book.title = titles.get(0).getTextContent().trim();
        if (!authors.isEmpty()) { book.author = ""; for (Element a : authors) book.author += (book.author.isEmpty() ? "" : ", ") + a.getTextContent().trim(); }
        Map<String, Element> manifest = new HashMap<>(); String nav = "", ncx = "", coverId = "";
        for (Element meta : elements(packageDoc, "meta")) if (meta.getAttribute("name").equals("cover")) coverId = meta.getAttribute("content");
        for (Element item : elements(packageDoc, "item")) {
            String id = item.getAttribute("id"); if (manifest.put(id, item) != null) throw new IOException("Duplicate manifest ID");
            String path;
            try { path = withoutFragment(resolve(opf, item.getAttribute("href"))); } catch (IOException external) { continue; }
            String properties = " " + item.getAttribute("properties") + " ";
            if (properties.contains(" nav ")) nav = path;
            if (item.getAttribute("media-type").equals("application/x-dtbncx+xml")) ncx = path;
            if (properties.contains(" cover-image ") || id.equals(coverId)) {
                if (isRaster(path) && safeFile(content, path).isFile()) book.cover = path;
            }
        }
        for (Element spine : elements(packageDoc, "spine")) {
            if (spine.getAttribute("page-progression-direction").equals("rtl")) throw new IOException("Right-to-left page progression is not supported yet");
        }
        for (Element ref : elements(packageDoc, "itemref")) {
            if (ref.getAttribute("properties").contains("rendition:layout-pre-paginated")) throw new IOException("Fixed-layout EPUB is unsupported");
            if (ref.getAttribute("linear").equals("no")) continue;
            Element item = manifest.get(ref.getAttribute("idref"));
            if (item == null || !item.getAttribute("media-type").equals("application/xhtml+xml")) throw new IOException("Unsupported or missing spine content");
            String path = withoutFragment(resolve(opf, item.getAttribute("href")));
            // Validate every chapter now, so unreadable books never become library entries.
            Document chapter = xml(read(safeFile(content, path), MAX_XML));
            if (elements(chapter, "body").isEmpty()) throw new IOException("Spine chapter has no XHTML body");
            book.chapters.add(path);
        }
        if (book.chapters.isEmpty()) throw new IOException("EPUB has no reflowable reading order");
        try {
            if (!nav.isEmpty() && safeFile(content, nav).isFile()) {
                Document doc = xml(read(safeFile(content, nav), MAX_XML));
                for (Element section : elements(doc, "nav")) {
                    if (!section.getAttributeNS("http://www.idpf.org/2007/ops", "type").contains("toc")) continue;
                    NodeList links = section.getElementsByTagNameNS("*", "a");
                    for (int i = 0; i < links.getLength(); i++) { Element a = (Element) links.item(i); addToc(book, nav, a.getAttribute("href"), a.getTextContent()); }
                }
            }
            if (book.tocLinks.isEmpty() && !ncx.isEmpty()) {
                Document doc = xml(read(safeFile(content, ncx), MAX_XML));
                for (Element point : elements(doc, "navPoint")) {
                    NodeList links = point.getElementsByTagNameNS("*", "content"), labels = point.getElementsByTagNameNS("*", "text");
                    if (links.getLength() > 0 && labels.getLength() > 0) addToc(book, ncx, ((Element) links.item(0)).getAttribute("src"), labels.item(0).getTextContent());
                }
            }
        } catch (IOException optionalToc) { book.tocLinks.clear(); book.tocLabels.clear(); }
        return book;
    }
    private static void addToc(EpubBook b, String base, String href, String label) {
        try {
            String link = resolve(base, href);
            if (b.chapters.contains(withoutFragment(link)) && b.tocLinks.size() < 2048) { b.tocLinks.add(link); b.tocLabels.add(label.trim()); }
        } catch (IOException ignored) { /* External/invalid optional link is excluded. */ }
    }
    public static boolean isRaster(String path) {
        return path.toLowerCase(Locale.US).matches(".*\\.(png|jpe?g|gif|webp)");
    }
}
