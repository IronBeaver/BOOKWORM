package com.bookworm.reader;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.net.ssl.*;

/** HTTPS-only bounded transport. Platform trust/hostname verification are never replaced. */
public final class GutenHttp {
    public interface Connections { HttpsURLConnection open(URL url) throws IOException; }
    public interface Progress { void update(long received,long total); }
    public static final class Operation {
        private volatile boolean cancelled,committing;
        private volatile HttpsURLConnection active;
        public synchronized boolean cancel() {
            if(committing) return false; if(cancelled) return true; cancelled=true;
            final HttpsURLConnection c=active;
            if(c!=null) { Thread closer=new Thread(c::disconnect,"BOOKWORM-cancel");closer.setDaemon(true);closer.start(); }
            return true;
        }
        public void check() throws InterruptedIOException { if(cancelled || Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Cancelled"); }
        public synchronized void beginImport() throws IOException { check(); committing=true; }
    }
    private static long nextRequest,blockedUntil;
    private final Connections connections;
    private final boolean spacing;
    public GutenHttp() { this(url -> (HttpsURLConnection)url.openConnection(),true); }
    // Package-private injection used by deterministic host tests, never a user-selectable endpoint.
    GutenHttp(Connections connections,boolean spacing) { this.connections=connections;this.spacing=spacing; }
    private void pace(Operation op) throws IOException {
        if(!spacing) return;
        long slot;
        synchronized(GutenHttp.class) {
            long now=System.currentTimeMillis();
            if(now<blockedUntil) throw new IOException("Server requested a pause. Try again later; no automatic retries are made.");
            slot=Math.max(now,nextRequest);nextRequest=slot+2000;
        }
        while(System.currentTimeMillis()<slot) {
            op.check(); try { Thread.sleep(Math.min(100,Math.max(1,slot-System.currentTimeMillis()))); }
            catch(InterruptedException interrupted) { Thread.currentThread().interrupt();op.check(); }
        }
    }
    public byte[] bytes(String url,GutenUrls.Kind kind,int id,int limit,Operation op) throws IOException {
        ByteArrayOutputStream out=new ByteArrayOutputStream(); transfer(url,kind,id,limit,out,op,(n,t)->{});return out.toByteArray();
    }
    public void download(String url,int id,File temporary,Operation op,Progress progress) throws IOException {
        boolean complete=false;
        try(FileOutputStream out=new FileOutputStream(temporary)) {
            transfer(url,GutenUrls.Kind.EPUB,id,EpubParser.MAX_ARCHIVE,out,op,progress);out.getFD().sync();complete=true;
        } finally { if(!complete && temporary.exists() && !temporary.delete()) temporary.deleteOnExit(); }
    }
    private void transfer(String value,GutenUrls.Kind kind,int id,long limit,OutputStream out,Operation op,Progress progress) throws IOException {
        long deadline=System.nanoTime()+(kind==GutenUrls.Kind.EPUB?300L:90L)*1000000000L;
        for(int redirects=0;redirects<=3;redirects++) {
            op.check(); URL url=GutenUrls.checked(value,kind,id);pace(op);op.check();
            HttpsURLConnection c=connections.open(url);op.active=c;
            try {
                op.check();c.setConnectTimeout(15000);c.setReadTimeout(20000);c.setInstanceFollowRedirects(false);c.setUseCaches(false);
                c.setRequestProperty("User-Agent","BOOKWORM/1.2 (personal, user-selected EPUB reader)");
                c.setRequestProperty("Accept-Encoding","identity");
                c.setRequestProperty("Accept",kind==GutenUrls.Kind.CATALOG?"application/json":kind==GutenUrls.Kind.EPUB?"application/epub+zip":"image/jpeg,image/png");
                int status=c.getResponseCode();op.check();
                if(status==429 || status==503) {
                    long wait=60000;String retry=c.getHeaderField("Retry-After");
                    try { wait=Math.max(60000,Long.parseLong(retry)*1000); }
                    catch(Exception notSeconds) {
                        try { java.text.SimpleDateFormat date=new java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz",Locale.US);date.setTimeZone(TimeZone.getTimeZone("GMT"));wait=Math.max(60000,date.parse(retry).getTime()-System.currentTimeMillis()); } catch(Exception ignored) { }
                    }
                    synchronized(GutenHttp.class) { blockedUntil=Math.max(blockedUntil,System.currentTimeMillis()+Math.min(wait,24*60*60*1000L)); }
                    throw new IOException("Server busy/rate limited (HTTP "+status+"). Please wait before retrying.");
                }
                if(status==301 || status==302 || status==303 || status==307 || status==308) {
                    String location=c.getHeaderField("Location");if(location==null || redirects==3) throw new IOException("Missing or excessive redirects");
                    value=new URL(url,location).toString();GutenUrls.checked(value,kind,id);continue;
                }
                if(status!=200) throw new IOException(status==403?"Server blocked this request (HTTP 403). Use the book landing page; BOOKWORM will not bypass the block.":"Server returned HTTP "+status);
                String encoding=c.getHeaderField("Content-Encoding");if(encoding!=null && !encoding.equalsIgnoreCase("identity")) throw new IOException("Unexpected compressed HTTP response");
                String type=c.getContentType();type=type==null?"":type.toLowerCase(Locale.US).split(";",2)[0].trim();
                if(kind==GutenUrls.Kind.CATALOG && !type.equals("application/json") || kind==GutenUrls.Kind.COVER && !(type.equals("image/jpeg")||type.equals("image/png"))
                        || kind==GutenUrls.Kind.EPUB && !(type.equals("application/epub+zip")||type.equals("application/octet-stream")||type.equals("application/zip")))
                    throw new IOException("Server returned an unsupported format instead of "+kind.name().toLowerCase(Locale.US));
                long total=-1;try { total=Long.parseLong(c.getHeaderField("Content-Length")); } catch(Exception ignored) { }
                if(total>limit) throw new IOException("Response exceeds download size limit");
                byte[] buffer=new byte[8192];long received=0,lastProgress=0;
                try(InputStream in=c.getInputStream()) {
                    int n;while((n=in.read(buffer))!=-1) {
                        op.check();if(System.nanoTime()>deadline) throw new SocketTimeoutException("Total transfer time limit reached");
                        if(received+n>limit) throw new IOException("Response exceeds download size limit");
                        out.write(buffer,0,n);received+=n;
                        long now=System.nanoTime();if(now-lastProgress>150000000L) { progress.update(received,total);lastProgress=now; }
                    }
                }
                op.check();if(total>=0 && total!=received) throw new IOException("Incomplete download; please retry");
                progress.update(received,total);return;
            } catch(IOException failed) { op.check();throw failed; }
            finally { op.active=null;c.disconnect(); }
        }
        throw new IOException("Too many redirects");
    }
    public static String message(Exception failure) {
        if(failure instanceof SSLException) return "Secure connection failed. This older Android device may not trust the server certificate or support its TLS settings. Check date/time; use desktop download + Import EPUB if needed. Certificate checks were not bypassed.";
        if(failure instanceof UnknownHostException || failure instanceof ConnectException) return "Cannot reach the server. Check your internet connection; existing books remain available offline.";
        if(failure instanceof SocketTimeoutException) return "Server connection timed out. Please retry later.";
        if(failure instanceof InterruptedIOException) return "Cancelled. No partial download was added to the library.";
        return failure.getMessage()==null?"Request failed. Please retry later.":failure.getMessage();
    }
}
