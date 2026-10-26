package com.bookworm.reader;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.*;
import org.w3c.dom.*;

/** Acquisition and provenance sidecars; existing EPUB import and book identities are unchanged. */
public final class GutenLibrary {
    private static final AtomicBoolean ACTIVE=new AtomicBoolean();
    private GutenLibrary() { }
    public static int identity(EpubBook book) {
        try {
            File content=new File(book.directory,"content");
            Document container=EpubParser.xml(EpubParser.read(new File(content,"META-INF/container.xml"),EpubParser.MAX_XML));
            NodeList roots=container.getElementsByTagNameNS("*","rootfile");
            if(roots.getLength()<1) return 0;
            String path=((Element)roots.item(0)).getAttribute("full-path");
            Document opf=EpubParser.xml(EpubParser.read(EpubParser.safeFile(content,path),EpubParser.MAX_XML));
            NodeList identifiers=opf.getElementsByTagNameNS("*","identifier");
            for(int i=0;i<Math.min(32,identifiers.getLength());i++) {
                Matcher m=Pattern.compile("https?://(?:www\\.)?gutenberg\\.org/(?:(?:ebooks|etext)/)?(\\d{1,8})/?").matcher(identifiers.item(i).getTextContent().trim());
                if(m.matches()) return Integer.parseInt(m.group(1));
            }
        } catch(IOException | RuntimeException invalid) { }
        return 0;
    }
    public static EpubBook find(File root,int id) {
        for(EpubBook book:LibraryCatalog.scan(root).books) {
            try { if(Integer.toString(id).equals(LibraryFiles.read(new File(book.directory,"gutenberg.properties")).getProperty("id"))) return book; }
            catch(IOException ignored) { }
            if(identity(book)==id) return book;
        }
        return null;
    }
    public static final class Result {
        public final EpubBook book; public final boolean existing; public final String warning;
        Result(EpubBook book,boolean existing,String warning) {this.book=book;this.existing=existing;this.warning=warning;}
    }
    public static Result acquire(GutenCatalog.Book selected,File root,File cache,GutenHttp http,GutenHttp.Operation op,
                                 GutenHttp.Progress progress,Runnable importing) throws IOException {
        if(!ACTIVE.compareAndSet(false,true)) throw new IOException("Another EPUB download/import is already in progress.");
        File temporary=null;
        try {
            op.check();EpubBook existing=find(root,selected.id);op.check();
            if(existing!=null) return new Result(existing,true,"");
            GutenUrls.checked(selected.epub,GutenUrls.Kind.EPUB,selected.id);
            if(!cache.isDirectory() && !cache.mkdirs()) throw new IOException("Cannot create private download directory");
            File[] stale=cache.listFiles((dir,name)->name.startsWith("guten-") && name.endsWith(".part"));
            if(stale!=null) for(File file:stale) if(file.isFile() && !file.delete()) throw new IOException("Cannot clear an interrupted private download");
            temporary=File.createTempFile("guten-",".part",cache);
            http.download(selected.epub,selected.id,temporary,op,progress);
            op.check();existing=find(root,selected.id);op.check();
            if(existing!=null) return new Result(existing,true,""); // A SAF import may have finished during the transfer.
            op.beginImport();importing.run(); // Explicit cancellation boundary: leave the atomic importer intact.
            EpubBook book;
            try(InputStream in=new FileInputStream(temporary)) { book=EpubParser.importBook(in,root); }
            String warning="";int embedded=identity(book);
            if(embedded>0 && embedded!=selected.id) warning="The EPUB identifies a different Gutenberg edition; no selected-ID association was saved.";
            else {
                Properties provenance=new Properties();provenance.setProperty("id",Integer.toString(selected.id));
                provenance.setProperty("landing",GutenUrls.landing(selected.id));provenance.setProperty("catalog","https://gutendex.com/");
                try { LibraryFiles.write(new File(book.directory,"gutenberg.properties"),provenance); }
                catch(IOException failed) { warning="Book imported, but catalog association could not be saved. EPUB identity/content checks still prevent known duplicates."; }
            }
            return new Result(book,false,warning);
        } finally {
            if(temporary!=null && temporary.exists() && !temporary.delete()) temporary.deleteOnExit();
            ACTIVE.set(false);
        }
    }
}
