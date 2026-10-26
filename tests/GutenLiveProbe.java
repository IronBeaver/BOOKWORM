package com.bookworm.reader;

import java.io.*;
import java.nio.file.*;

/** Optional explicitly invoked single-book smoke test on desktop JDK TLS, not part of offline tests. */
public final class GutenLiveProbe {
    public static void main(String[] args)throws Exception {
        GutenCatalog page=GutenCatalog.parse(new String(Files.readAllBytes(Paths.get("app/build/gutendex-84-live.json")),"UTF-8"));
        if(page.books.size()!=1 || page.books.get(0).id!=84)throw new IOException("Expected the explicitly requested catalog item 84");
        File base=new File("app/build/gutenberg-live-"+System.currentTimeMillis());base.mkdirs();
        GutenLibrary.Result result=GutenLibrary.acquire(page.books.get(0),new File(base,"library"),new File(base,"cache"),new GutenHttp(),new GutenHttp.Operation(),(n,total)->{},()->System.out.println("Download complete; validating with existing EPUB importer"));
        int count=0;for(String chapter:result.book.chapters){EpubHtml.chapter(result.book,chapter,"body{}");count++;}
        System.out.println("Desktop live HTTPS/import/sanitize PASS: "+result.book.title+"; "+count+" chapters; Gutenberg ID "+GutenLibrary.identity(result.book)+"; source "+page.books.get(0).epub+"; library "+base.getPath());
    }
}
