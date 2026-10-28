package com.bookworm.reader;

import android.content.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public final class ReaderComfortTests {
    private static int checks;
    private static void check(boolean ok, String message) { checks++; if (!ok) throw new AssertionError(message); }
    private static final class Memory extends Context {
        final Map<String, Prefs> stores = new HashMap<>();
        public SharedPreferences getSharedPreferences(String name, int mode) { return stores.computeIfAbsent(name, key -> new Prefs()); }
        Prefs reading() { return (Prefs) getSharedPreferences("reading", 0); }
        Prefs launch() { return (Prefs) getSharedPreferences("reading_launch", 0); }
    }
    private static final class Prefs implements SharedPreferences, SharedPreferences.Editor {
        final Map<String,Object> values = new HashMap<>(), pending = new HashMap<>(); boolean fail;
        public String getString(String k, String f) { return values.containsKey(k) ? (String) values.get(k) : f; }
        public boolean getBoolean(String k, boolean f) { return values.containsKey(k) ? (Boolean) values.get(k) : f; }
        public int getInt(String k, int f) { return values.containsKey(k) ? (Integer) values.get(k) : f; }
        public Editor edit() { pending.clear(); return this; }
        public Editor putString(String k, String v) { pending.put(k, v); return this; }
        public Editor putBoolean(String k, boolean v) { pending.put(k, v); return this; }
        public Editor putInt(String k, int v) { pending.put(k, v); return this; }
        public boolean commit() { if (fail) return false; values.putAll(pending); return true; }
    }
    public static void main(String[] args) throws Exception {
        Memory memory = new Memory(); ReaderPreferences prefs = ReaderPreferences.load(memory);
        check(prefs.continueOnLaunch && prefs.paletteId.equals(ReaderPalettes.FOLLOW), "Both migration defaults");
        check(memory.reading().values.isEmpty(), "Loading defaults never writes");
        for (int app = 0; app < 3; app++) for (int palette = 0; palette < ReaderPalettes.count(); palette++) {
            ThemeSettings original = ThemeSettings.preset(app, true, 18, true);
            ThemeSettings colours = ReaderPalettes.resolve(ReaderPalettes.id(palette), original);
            if (palette == 0) check(colours == original && ReaderPalettes.selectionCss(ReaderPalettes.FOLLOW, colours).isEmpty(), "Follow Appearance is unchanged");
            else {
                check(ThemeSettings.contrast(colours.background, colours.text) >= 4.5, "Body contrast");
                check(ThemeSettings.contrast(colours.background, colours.accent) >= 4.5, "Links, native handles and selection contrast");
                check(ThemeSettings.contrast(colours.background, ShelfPresentation.muted(colours)) >= 4.5, "Progress contrast");
                check(ThemeSettings.contrast(0xffffffff, ReaderPalettes.systemBar(ReaderPalettes.id(palette), colours)) >= 4.5, "API 22 white system-bar icons remain legible");
            }
            for (int font = 0; font < ReaderFonts.count(); font++) for (boolean paged : new boolean[]{true, false}) {
                prefs = ReaderPreferences.load(memory); prefs.paletteId = ReaderPalettes.id(palette); prefs.fontId = ReaderFonts.id(font);
                prefs.continueOnLaunch = false; prefs.paged = paged; prefs.size = 24; prefs.margin = 26; prefs.spacing = 18;
                check(prefs.save(memory), "Palette/font preferences save");
                ReaderPreferences loaded = ReaderPreferences.load(memory);
                check(loaded.paletteId.equals(prefs.paletteId) && loaded.fontId.equals(prefs.fontId) && loaded.paged == paged && !loaded.continueOnLaunch, "Independent persisted choices");
                check(loaded.size == 24 && loaded.margin == 26 && loaded.spacing == 18, "Comfort geometry unchanged");
                String css = loaded.css(original);
                check(css.contains("background:" + ThemeSettings.hex(colours.background)) && css.contains("color:" + ThemeSettings.hex(colours.text)), "Selected reader colours");
                check(original.background == ThemeSettings.preset(app, true, 18, true).background, "App palette never mutated");
            }
        }
        memory.reading().values.put("palette_id", "future_palette");
        check(ReaderPreferences.load(memory).paletteId.equals(ReaderPalettes.FOLLOW), "Unknown palette fallback");
        LaunchAttempt task = new LaunchAttempt(true); task.resume(); int ticket = task.claim();
        check(task.allowed(ticket) && task.claim() == -1, "Exactly one fresh launch opportunity");
        task.pause(); task.resume(); check(!task.allowed(ticket) && task.claim() == -1, "Background return cannot navigate");
        task = new LaunchAttempt(true); task.resume(); ticket = task.claim(); task.pause(); task.resume();
        check(!task.allowed(ticket), "In-flight scan cancelled across background return");
        task = new LaunchAttempt(false); task.resume(); check(task.claim() == -1, "Restored/duplicate task has no automatic navigation");

        File root = new File(args[0]); check(root.mkdirs() || root.isDirectory(), "Fixture directory");
        EpubBook first = book(root, 'a', 1000), last = book(root, 'b', 2000);
        LibraryCatalog.Snapshot library = LibraryCatalog.scan(root);
        Memory launch = new Memory(); byte[] position = Files.readAllBytes(new File(last.directory, "position.properties").toPath());
        check(ContinueReadingStore.begin(launch, library, false) == null && launch.launch().values.isEmpty(), "OFF never starts or mutates identity");
        check(ContinueReadingStore.begin(launch, library, true).equals(last.id), "ON migrates existing latest read book");
        check(ContinueReadingStore.begin(launch, library, true) == null, "Interrupted attempt does not loop on next fresh launch");
        check(ContinueReadingStore.opened(launch, last.id), "Successful ready reader records identity");
        Map<String,Object> savedLaunch = new HashMap<>(launch.launch().values);
        check(ContinueReadingStore.begin(launch, library, false) == null && launch.launch().values.equals(savedLaunch), "OFF preserves existing identity without navigating");
        check(ContinueReadingStore.begin(launch, library, true).equals(last.id), "Successful read permits next fresh launch");
        check(ContinueReadingStore.opened(launch, first.id), "Manual different book replaces last ID and clears pending attempt");
        check(ContinueReadingStore.begin(launch, library, true).equals(first.id), "Manual choice becomes continuation target");
        ContinueReadingStore.opened(launch, last.id);
        check(ContinueReadingStore.begin(launch, new LibraryCatalog.Snapshot(Collections.singletonList(first), 0), true) == null, "Removed record never selects a different older book");
        check(new File(last.directory, "content/chapter.xhtml").delete(), "Simulate missing/inaccessible last book");
        check(ContinueReadingStore.begin(launch, LibraryCatalog.scan(root), true) == null, "Missing last book falls back, never opens older book");
        check(ContinueReadingStore.begin(new Memory(), LibraryCatalog.scan(root), true) == null, "Ambiguous damaged legacy library stays on shelf");
        ContinueReadingStore.opened(launch, first.id);
        check(ContinueReadingStore.begin(launch, LibraryCatalog.scan(root), true).equals(first.id), "Manual success recovers after unavailable target");
        check(Arrays.equals(position, Files.readAllBytes(new File(last.directory, "position.properties").toPath())), "Identity/attempt logic never rewrites positions");
        Memory failedCommit = new Memory(); failedCommit.launch().fail = true;
        check(ContinueReadingStore.begin(failedCommit, library, true) == null, "Failed attempt-marker commit fails closed");
        check(!ContinueReadingStore.opened(failedCommit, first.id) && failedCommit.launch().values.isEmpty(), "Failed identity write preserves store");
        File empty = new File(root, "empty"); empty.mkdirs();
        check(ContinueReadingStore.begin(new Memory(), LibraryCatalog.scan(empty), true) == null, "No previous book stays on shelf");
        EpubBook unread = book(empty, 'c', 0);
        check(ContinueReadingStore.begin(new Memory(), LibraryCatalog.scan(empty), true) == null, "Unread seeded/imported book is never auto-opened");
        check(!ContinueReadingStore.opened(new Memory(), "../../unsafe"), "Invalid identities rejected");
        System.out.println("PASS: " + checks + " reader palette/persistence/contrast and launch-state/library/position checks");
        for (int i = 1; i < ReaderPalettes.count(); i++) {
            ThemeSettings c = ReaderPalettes.resolve(ReaderPalettes.id(i), ThemeSettings.preset(0, true, 18, false));
            System.out.printf(java.util.Locale.US, "%s body %.2f:1; links/selection/handles %.2f:1%n", ReaderPalettes.label(i), ThemeSettings.contrast(c.background,c.text), ThemeSettings.contrast(c.background,c.accent));
        }
    }
    private static EpubBook book(File root, char digit, long read) throws Exception {
        char[] chars = new char[64]; Arrays.fill(chars, digit);
        EpubBook book = new EpubBook(); book.id = new String(chars); book.directory = new File(root, book.id);
        File content = new File(book.directory, "content"); content.mkdirs();
        Files.write(new File(content, "chapter.xhtml").toPath(), "<html><body><p>Saved passage.</p></body></html>".getBytes("UTF-8"));
        book.chapters.add("chapter.xhtml"); book.save();
        if (read > 0) {
            new ReadingPosition("chapter.xhtml", 7).save(book.directory);
            check(new File(book.directory, "position.properties").setLastModified(read), "Legacy position timestamp fixture");
            LibraryFiles.markRead(book.directory, read);
        }
        return book;
    }
}
