package com.bookworm.reader;

import java.io.*;
import java.util.*;
import org.json.*;

/** Gutendex metadata only. No HTML scraping and no acquisition URL construction. */
public final class GutenCatalog {
    public static final int MAX_JSON=2*1024*1024;
    public static final class Book {
        public final int id;
        public final String title,author,languages,summary,epub,cover;
        public Book(int id,String title,String author,String languages,String summary,String epub,String cover) {
            this.id=id;this.title=title;this.author=author;this.languages=languages;this.summary=summary;this.epub=epub;this.cover=cover;
        }
    }
    public final List<Book> books;
    public final String next,previous;
    public final int count;
    private GutenCatalog(List<Book> books,String next,String previous,int count) {
        this.books=Collections.unmodifiableList(books);this.next=next;this.previous=previous;this.count=count;
    }
    public static GutenCatalog parse(String json) throws IOException {
        if(json.length()>MAX_JSON) throw new IOException("Catalog response too large");
        // Bound structure before org.json can recurse; unknown fields are never rendered as markup.
        int depth=0,nodes=0; boolean quote=false,escape=false;
        for(int i=0;i<json.length();i++) {
            char ch=json.charAt(i);
            if(quote) { if(escape) escape=false; else if(ch=='\\') escape=true; else if(ch=='"') quote=false; }
            else if(ch=='"') quote=true;
            else if(ch=='{' || ch=='[') { if(++depth>16 || ++nodes>12000) throw new IOException("Catalog structure exceeds safety limits"); }
            else if(ch=='}' || ch==']') { if(--depth<0) throw new IOException("Invalid catalog JSON"); }
        }
        if(depth!=0 || quote) throw new IOException("Incomplete catalog JSON");
        try {
            JSONObject root=new JSONObject(json); JSONArray results=root.getJSONArray("results");
            if(results.length()>32) throw new IOException("Catalog page exceeds 32 results");
            List<Book> books=new ArrayList<>(); Set<Integer> seen=new HashSet<>();
            for(int i=0;i<results.length();i++) {
                JSONObject b=results.getJSONObject(i); String rawId=String.valueOf(b.opt("id"));
                if(!rawId.matches("[1-9][0-9]{0,7}")) throw new IOException("Invalid catalog identity");
                int id=Integer.parseInt(rawId);
                if(id<1 || !seen.add(id)) throw new IOException("Invalid or repeated catalog identity");
                if(!Boolean.FALSE.equals(b.opt("copyright")) || !"Text".equals(b.optString("media_type"))) continue;
                JSONArray authors=b.optJSONArray("authors"); StringBuilder by=new StringBuilder();
                if(authors!=null) for(int a=0;a<Math.min(16,authors.length());a++) {
                    if(by.length()>0) by.append("; "); by.append(text(authors.getJSONObject(a),"name",256,"Unknown author"));
                }
                JSONArray languages=b.optJSONArray("languages"); StringBuilder langs=new StringBuilder();
                if(languages!=null) for(int a=0;a<Math.min(16,languages.length());a++) { if(langs.length()>0) langs.append(", ");langs.append(languages.optString(a).substring(0,Math.min(16,languages.optString(a).length()))); }
                JSONObject formats=b.optJSONObject("formats"); String epub="",cover="";
                if(formats!=null) {
                    // Exact MIME entry; never accept Kindle/HTML/octet-stream as an EPUB.
                    epub=safe(formats.optString("application/epub+zip"),GutenUrls.Kind.EPUB,id);
                    cover=safe(formats.optString("image/jpeg"),GutenUrls.Kind.COVER,id);
                    if(cover.isEmpty()) cover=safe(formats.optString("image/png"),GutenUrls.Kind.COVER,id);
                }
                JSONArray summaries=b.optJSONArray("summaries"); String summary=summaries==null?"":summaries.optString(0);
                if(summary.length()>4000) summary=summary.substring(0,4000)+"…";
                books.add(new Book(id,text(b,"title",1024,"Untitled"),by.length()==0?"Unknown author":by.toString(),langs.toString(),summary,epub,cover));
            }
            return new GutenCatalog(books,page(root.optString("next")),page(root.optString("previous")),Math.max(0,root.optInt("count")));
        } catch(JSONException | RuntimeException bad) { throw new IOException("Invalid Gutendex response",bad); }
    }
    private static String text(JSONObject o,String key,int limit,String fallback) { String value=o.optString(key).trim(); return value.isEmpty()?fallback:value.substring(0,Math.min(limit,value.length())); }
    private static String safe(String value,GutenUrls.Kind kind,int id) { try { GutenUrls.checked(value,kind,id);return value; } catch(IOException bad) {return "";} }
    private static String page(String value) throws IOException {
        if(value==null || value.isEmpty() || value.equals("null")) return "";
        GutenUrls.checked(value,GutenUrls.Kind.CATALOG,0); return value;
    }
}
