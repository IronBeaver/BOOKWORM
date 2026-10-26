package com.bookworm.reader;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.cert.Certificate;
import java.util.*;
import javax.net.ssl.*;

/** Runs production parsing, transport and acquisition against queued HTTPS connection fixtures. */
public final class GutenbergTests {
    static int checks;
    static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    interface Work {void run()throws Exception;}
    static void reject(Work work,String message)throws Exception {try{work.run();throw new AssertionError(message);}catch(IOException expected){checks++;}}
    static String json(int id,String formats){return "{\"count\":1,\"next\":null,\"previous\":null,\"results\":[{\"id\":"+id+",\"title\":\"A Book\",\"authors\":[{\"name\":\"An Author\"}],\"languages\":[\"en\"],\"copyright\":false,\"media_type\":\"Text\",\"formats\":"+formats+"}]}";}
    static final String EPUB="https://www.gutenberg.org/ebooks/84.epub3.images", REDIRECT="https://www.gutenberg.org/cache/epub/84/pg84-images-3.epub";
    static class Response extends HttpsURLConnection {
        int code=200;byte[] data;Map<String,String> headers=new HashMap<>();InputStream stream;boolean disconnected;
        Response(byte[] data,String type)throws Exception{super(new URL(EPUB));this.data=data;headers.put("Content-Type",type);headers.put("Content-Length",""+data.length);}
        public void connect(){}public void disconnect(){disconnected=true;}public boolean usingProxy(){return false;}
        public String getCipherSuite(){return "TLS_TEST";}public Certificate[] getLocalCertificates(){return null;}public Certificate[] getServerCertificates(){return null;}
        public int getResponseCode(){return code;}public String getHeaderField(String key){return headers.get(key);}public String getContentType(){return headers.get("Content-Type");}
        public InputStream getInputStream()throws IOException{return stream==null?new ByteArrayInputStream(data):stream;}
    }
    static class Fixture implements GutenHttp.Connections {
        final Queue<Response> queue=new ArrayDeque<>();int opened;List<String> urls=new ArrayList<>();
        public HttpsURLConnection open(URL url)throws IOException {opened++;urls.add(url.toString());if(queue.isEmpty())throw new IOException("No fixture");return queue.remove();}
        GutenHttp client(){return new GutenHttp(this,false);}
    }
    public static void main(String[] args)throws Exception {
        String query=GutenUrls.query("Mary Shelley & moon","EN,fr");
        check(query.contains("Mary+Shelley+%26+moon")&&query.contains("languages=en%2Cfr"),"Search encoded safely; language normalized");
        check(GutenUrls.query("","").contains("sort=popular"),"Popular catalog default");
        reject(()->GutenUrls.query("x","english"),"Invalid language rejected");reject(()->GutenUrls.query("x".repeat(161),""),"Long search rejected");
        for(String url:new String[]{"http://www.gutenberg.org/ebooks/84.epub3.images","https://evil.example/84.epub","https://www.gutenberg.org.evil/ebooks/84.epub3.images","https://user@www.gutenberg.org/ebooks/84.epub3.images","https://www.gutenberg.org:444/ebooks/84.epub3.images","https://127.0.0.1/84.epub","https://www.gutenberg.org/cache/epub/84/%2e%2e/x.epub","https://www.gutenberg.org/ebooks/85.epub3.images","https://www.gutenberg.org/ebooks/84.kf8.images",EPUB+"?redirect=x",EPUB+"#x"})reject(()->GutenUrls.checked(url,GutenUrls.Kind.EPUB,84),"Unsafe download URL");
        GutenUrls.checked(EPUB,GutenUrls.Kind.EPUB,84);GutenUrls.checked(REDIRECT,GutenUrls.Kind.EPUB,84);checks+=2;
        reject(()->GutenUrls.checked("https://gutendex.com.evil/books/?page=2",GutenUrls.Kind.CATALOG,0),"Unsafe pagination host");
        GutenCatalog catalog=GutenCatalog.parse(json(84,"{\"application/epub+zip\":\""+EPUB+"\",\"image/jpeg\":\"https://www.gutenberg.org/cache/epub/84/pg84.cover.medium.jpg\"}"));
        GutenCatalog.Book selected=catalog.books.get(0);
        check(selected.epub.equals(EPUB)&&!selected.cover.isEmpty(),"Exact catalog-supplied EPUB/cover selection");
        check(selected.author.equals("An Author")&&selected.languages.equals("en"),"Authors and language parsed");
        check(catalog.next.isEmpty()&&catalog.previous.isEmpty(),"Null pagination terminates");
        String paged=json(84,"{}").replace("\"next\":null","\"next\":\"https://gutendex.com/books/?page=2\"");
        check(GutenCatalog.parse(paged).next.endsWith("page=2"),"Next page supplied by API");
        reject(()->GutenCatalog.parse(paged.replace("gutendex.com","evil.example")),"Unsafe next rejected");
        check(GutenCatalog.parse(json(84,"{\"application/octet-stream\":\""+EPUB+"\"}")).books.get(0).epub.isEmpty(),"Generic binary is not selected as EPUB");
        check(GutenCatalog.parse(json(84,"{\"application/epub+zip\":\"https://evil.example/book.epub\"}")).books.get(0).epub.isEmpty(),"Untrusted catalog file suppressed");
        check(GutenCatalog.parse(json(84,"{}").replace("\"copyright\":false","\"copyright\":true")).books.isEmpty(),"Copyrighted catalog entries excluded");
        reject(()->GutenCatalog.parse("{"),"Malformed JSON");reject(()->GutenCatalog.parse("[".repeat(30)+"]".repeat(30)),"JSON depth bounded");
        reject(()->GutenCatalog.parse(json(84,"{}").replace("\"id\":84","\"id\":4294967380")),"Overflowing catalog identity rejected");
        reject(()->GutenCatalog.parse(json(84,"{}").replace("\"id\":84","\"id\":84.5")),"Fractional catalog identity rejected");
        reject(()->GutenCatalog.parse(json(84,"{}").replace("\"id\":84","\"id\":0")),"Zero catalog identity rejected");
        reject(()->GutenCatalog.parse(" ".repeat(GutenCatalog.MAX_JSON+1)),"Metadata response bounded before parsing");
        String entry=json(84,"{}");String book=entry.substring(entry.indexOf("[{" )+1,entry.lastIndexOf(']'));
        reject(()->GutenCatalog.parse("{\"results\":["+String.join(",",Collections.nCopies(33,book))+"]}"),"Page result count bounded");
        reject(()->GutenCatalog.parse("{\"results\":["+book+","+book+"]}"),"Duplicate catalog IDs rejected");
        Fixture f=new Fixture();Response redirect=new Response(new byte[0],"text/html");redirect.code=302;redirect.headers.put("Location",REDIRECT);f.queue.add(redirect);f.queue.add(new Response("data".getBytes("UTF-8"),"application/epub+zip"));
        check(new String(f.client().bytes(EPUB,GutenUrls.Kind.EPUB,84,10,new GutenHttp.Operation()),"UTF-8").equals("data")&&f.opened==2,"Safe redirect followed and bytes bounded");
        check(redirect.disconnected&&!redirect.getInstanceFollowRedirects()&&redirect.getConnectTimeout()==15000&&redirect.getReadTimeout()==20000,"Connections close; automatic redirects disabled; timeouts set");
        for(String location:new String[]{"http://www.gutenberg.org/cache/epub/84/x.epub","https://evil.example/x.epub","https://www.gutenberg.org/ebooks/84.html.images"}) {
            Fixture bad=new Fixture();Response r=new Response(new byte[0],"text/html");r.code=302;r.headers.put("Location",location);bad.queue.add(r);
            reject(()->bad.client().bytes(EPUB,GutenUrls.Kind.EPUB,84,10,new GutenHttp.Operation()),"Unsafe redirect blocked");check(bad.opened==1&&r.disconnected,"No request sent to unsafe redirect");
        }
        Fixture loop=new Fixture();for(int i=0;i<4;i++){Response r=new Response(new byte[0],"text/html");r.code=302;r.headers.put("Location",EPUB);loop.queue.add(r);}
        reject(()->loop.client().bytes(EPUB,GutenUrls.Kind.EPUB,84,10,new GutenHttp.Operation()),"Redirect loop bounded");check(loop.opened==4,"At most three redirects");
        for(int status:new int[]{403,404,429,500,503}) {Fixture bad=new Fixture();Response r=new Response(new byte[0],"text/html");r.code=status;bad.queue.add(r);reject(()->bad.client().bytes(EPUB,GutenUrls.Kind.EPUB,84,10,new GutenHttp.Operation()),"Failure status surfaced without retries");check(bad.opened==1,"No retry loop");}
        for(String type:new String[]{"text/html","application/pdf","application/x-mobipocket-ebook"}){Fixture bad=new Fixture();bad.queue.add(new Response(new byte[2],type));reject(()->bad.client().bytes(EPUB,GutenUrls.Kind.EPUB,84,10,new GutenHttp.Operation()),"Unexpected file type rejected");}
        Fixture oversized=new Fixture();oversized.queue.add(new Response(new byte[11],"application/epub+zip"));reject(()->oversized.client().bytes(EPUB,GutenUrls.Kind.EPUB,84,10,new GutenHttp.Operation()),"Declared oversized body rejected");
        Fixture chunked=new Fixture();Response chunk=new Response(new byte[11],"application/epub+zip");chunk.headers.remove("Content-Length");chunked.queue.add(chunk);reject(()->chunked.client().bytes(EPUB,GutenUrls.Kind.EPUB,84,10,new GutenHttp.Operation()),"Unknown-length oversized body rejected");
        Fixture partial=new Fixture();Response shortBody=new Response(new byte[1],"application/epub+zip");shortBody.headers.put("Content-Length","10");partial.queue.add(shortBody);reject(()->partial.client().bytes(EPUB,GutenUrls.Kind.EPUB,84,10,new GutenHttp.Operation()),"Truncated body rejected");
        File base=new File("app/build/gutenberg-tests-"+UUID.randomUUID());base.mkdirs();File root=new File(base,"library"),cache=new File(base,"cache");cache.mkdirs();
        try {
            GutenHttp.Operation cancelled=new GutenHttp.Operation();cancelled.cancel();Fixture empty=new Fixture();File temp=new File(cache,"cancel.part");
            reject(()->empty.client().download(EPUB,84,temp,cancelled,(n,t)->{}),"Cancel before network");check(!temp.exists()&&empty.opened==0,"Cancelled partial removed, no request");
            GutenHttp.Operation during=new GutenHttp.Operation();Fixture slow=new Fixture();Response body=new Response(new byte[10],"application/epub+zip");body.stream=new ByteArrayInputStream(new byte[10]){@Override public synchronized int read(byte[] b,int o,int l){int n=super.read(b,o,l);during.cancel();return n;}};slow.queue.add(body);
            reject(()->slow.client().download(EPUB,84,temp,during,(n,t)->{}),"Cancel during body copy");check(!temp.exists(),"Mid-download cleanup");
            Fixture invalid=new Fixture();invalid.queue.add(new Response("not an EPUB".getBytes("UTF-8"),"application/epub+zip"));
            reject(()->GutenLibrary.acquire(selected,root,cache,invalid.client(),new GutenHttp.Operation(),(n,t)->{},()->{}),"Completed bytes structurally validated by existing importer");
            check(LibraryCatalog.scan(root).books.isEmpty()&&cache.list().length==0,"Failed import leaves no partial book/download");
            byte[] epub=Files.readAllBytes(Paths.get("app/src/main/assets/books/frankenstein.epub"));Fixture valid=new Fixture();valid.queue.add(new Response(epub,"application/epub+zip"));
            Files.write(new File(cache,"guten-stale.part").toPath(),new byte[]{1});
            GutenHttp.Operation commit=new GutenHttp.Operation();GutenLibrary.Result imported=GutenLibrary.acquire(selected,root,cache,valid.client(),commit,(n,t)->{},()->{
                check(!commit.cancel(),"Atomic import phase cannot be interrupted");
                try {GutenLibrary.acquire(selected,root,cache,new Fixture().client(),new GutenHttp.Operation(),(n,t)->{},()->{});throw new AssertionError("Concurrent acquisition allowed");}
                catch(IOException expected){checks++;}
            });
            check(imported.book.title.contains("Frankenstein")&&!imported.book.cover.isEmpty(),"Real EPUB import retains original metadata/artwork");
            check(cache.list().length==0&&LibraryCatalog.scan(root).books.size()==1,"Successful import atomic and temporary removed");
            check(GutenLibrary.identity(imported.book)==84,"Embedded Gutenberg OPF ID found");
            File position=new File(imported.book.directory,"position.properties");Files.write(position.toPath(),"keep-progress".getBytes("UTF-8"));
            Fixture noFetch=new Fixture();GutenLibrary.Result duplicate=GutenLibrary.acquire(selected,root,cache,noFetch.client(),new GutenHttp.Operation(),(n,t)->{},()->{});
            check(duplicate.existing&&duplicate.book.id.equals(imported.book.id)&&noFetch.opened==0,"Known ID prevents second download");
            check(new String(Files.readAllBytes(position.toPath()),"UTF-8").equals("keep-progress"),"Duplicate preserves reading position");
            new File(imported.book.directory,"gutenberg.properties").delete();new File(imported.book.directory,"gutenberg.properties.previous").delete();
            check(GutenLibrary.find(root,84)!=null,"Previously imported/bundled EPUB recognized without catalog sidecar");
            check(GutenLibrary.find(root,999999)==null,"Different edition not silently matched by title");
            Fixture failedAgain=new Fixture();Response forbidden=new Response(new byte[0],"text/html");forbidden.code=403;failedAgain.queue.add(forbidden);
            GutenCatalog.Book other=new GutenCatalog.Book(85,"Other","Author","en","","https://www.gutenberg.org/ebooks/85.epub3.images","");
            reject(()->GutenLibrary.acquire(other,root,cache,failedAgain.client(),new GutenHttp.Operation(),(n,t)->{},()->{}),"Failure after prior success recovers global single-download permit");
            check(LibraryCatalog.scan(root).books.size()==1,"Failure preserves previous library");
        } finally {EpubParser.deleteTree(base);}
        check(GutenHttp.message(new SSLHandshakeException("untrusted")).contains("not bypassed"),"TLS errors explain secure fallback");
        check(GutenHttp.message(new UnknownHostException()).contains("offline"),"Offline message preserves library availability");
        Fixture paused=new Fixture();reject(()->new GutenHttp(paused,true).bytes(EPUB,GutenUrls.Kind.EPUB,84,10,new GutenHttp.Operation()),"Server cooldown respected across operations");check(paused.opened==0,"No network during cooldown");
        System.out.println("Passed "+checks+" catalog/HTTPS-mock/download/cancellation/duplicate/atomic-import checks (host JSON adapter; no Android TLS claim).");
    }
}
