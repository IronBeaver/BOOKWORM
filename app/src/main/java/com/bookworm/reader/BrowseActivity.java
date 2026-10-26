package com.bookworm.reader;

import android.app.AlertDialog;
import android.content.*;
import android.graphics.*;
import android.net.Uri;
import android.os.Bundle;
import android.util.LruCache;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/** Explicit foreground browsing. No automatic searches, image batches, retries or background downloads. */
public final class BrowseActivity extends ThemedActivity {
    public static final String NOTICE="Project Gutenberg books are generally free of US copyright restrictions. Outside the US, check your local copyright laws before downloading. Embedded licenses and attribution are preserved. Gutendex is an independent catalog API. BOOKWORM is not affiliated with Project Gutenberg or Gutendex.";
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final GutenHttp http=new GutenHttp();
    private final LruCache<Integer,Bitmap> covers=new LruCache<Integer,Bitmap>(1024*1024) {
        @Override protected int sizeOf(Integer id,Bitmap bitmap) { return bitmap.getByteCount(); }
    };
    private EditText search,language;
    private TextView status;
    private ProgressBar progress;
    private Button searchButton,popular,next,previous,cancel;
    private ListView list;
    private GutenCatalog page;
    private GutenHttp.Operation operation;
    private boolean busy,committing;
    private String currentUrl="";
    private final LinkedHashSet<String> visited=new LinkedHashSet<>();
    private BaseAdapter adapter;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);loadAppearance();
        LinearLayout root=column();root.setPadding(dp(8),0,dp(8),dp(8));root.addView(ShelfUi.toolbar(this,"Browse Books",true));
        search=new EditText(this);style(search);search.setSingleLine(true);search.setHint("Title or author");search.setContentDescription("Search by title or author");search.setMinHeight(dp(48));
        search.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(160)});root.addView(search);
        LinearLayout filters=new LinearLayout(this);
        language=new EditText(this);style(language);language.setSingleLine(true);language.setHint("Language: en (optional)");language.setContentDescription("Optional two-letter language codes, for example en or en,fr");language.setMinHeight(dp(48));
        language.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(14)});filters.addView(language,new LinearLayout.LayoutParams(0,-2,1));
        searchButton=button("Search",v -> query(false));filters.addView(searchButton);popular=button("Popular",v -> query(true));filters.addView(popular);root.addView(filters);
        if(state!=null) {search.setText(state.getString("search",""));language.setText(state.getString("language",""));}
        TextView information=text("Gutendex catalog · Project Gutenberg books · Terms & sources",false);information.setTextSize(12);information.setMinHeight(dp(48));information.setFocusable(true);information.setClickable(true);information.setBackground(ShelfUi.feedback(this));information.setOnClickListener(v -> information());root.addView(information);
        status=text("Tap Popular or Search to connect. Covers load only when requested. Existing library books remain offline.",false);status.setTextSize(13);status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);root.addView(status);
        progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setVisibility(View.GONE);root.addView(progress,new LinearLayout.LayoutParams(-1,dp(6)));
        cancel=button("Cancel request",v -> { if(operation!=null && operation.cancel()) {status.setText("Cancelling…");cancel.setEnabled(false);} });cancel.setVisibility(View.GONE);root.addView(cancel);
        list=new ListView(this);list.setDivider(null);list.setCacheColorHint(appearance.background);list.setItemsCanFocus(true);
        adapter=new BaseAdapter() {
            public int getCount() {return page==null?0:page.books.size();}
            public GutenCatalog.Book getItem(int at) {return page.books.get(at);}
            public long getItemId(int at) {return getItem(at).id;}
            public View getView(int at,View recycled,ViewGroup parent) {return result(getItem(at));}
        };
        list.setAdapter(adapter);root.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout navigation=new LinearLayout(this);
        previous=button("Previous results",v -> requestPage(page.previous,false));next=button("Next results",v -> requestPage(page.next,false));
        navigation.addView(previous,new LinearLayout.LayoutParams(0,-2,1));navigation.addView(next,new LinearLayout.LayoutParams(0,-2,1));root.addView(navigation);
        setContentView(root);setBusy(false);
    }
    private void query(boolean popularOnly) {
        try { requestPage(GutenUrls.query(popularOnly?"":search.getText().toString(),language.getText().toString()),true); }
        catch(IOException bad) {status.setText(bad.getMessage());}
    }
    private void setBusy(boolean value) {
        busy=value;searchButton.setEnabled(!value);popular.setEnabled(!value);search.setEnabled(!value);language.setEnabled(!value);
        next.setEnabled(!value && page!=null && !page.next.isEmpty() && !page.next.equals(currentUrl));
        previous.setEnabled(!value && page!=null && !page.previous.isEmpty() && !page.previous.equals(currentUrl));
        cancel.setVisibility(value?View.VISIBLE:View.GONE);cancel.setEnabled(!committing);progress.setVisibility(value?View.VISIBLE:View.GONE);progress.setIndeterminate(true);
    }
    private boolean alive() {return !isFinishing() && !isDestroyed();}
    private void requestPage(String url,boolean fresh) {
        if(busy || url.isEmpty()) return;
        if(!fresh && url.equals(currentUrl)) {status.setText("Catalog returned the same page. Try a new search.");return;}
        if(fresh) visited.clear();
        if(!fresh && visited.size()>=100 && !visited.contains(url)) {status.setText("Browsing session limit reached. Start a new search.");return;}
        operation=new GutenHttp.Operation();GutenHttp.Operation op=operation;committing=false;setBusy(true);status.setText("Loading one catalog page…");
        worker.execute(() -> {
            try {
                byte[] json=http.bytes(url,GutenUrls.Kind.CATALOG,0,GutenCatalog.MAX_JSON,op);
                GutenCatalog ready=GutenCatalog.parse(new String(json,"UTF-8"));op.check();
                runOnUiThread(() -> {if(!alive())return;page=ready;currentUrl=url;visited.add(url);adapter.notifyDataSetChanged();list.setSelection(0);setBusy(false);
                    status.setText(ready.books.size()+" results shown · "+ready.count+" catalog matches. Tap a book for details; tap Cover to load its thumbnail.");});
            } catch(Exception failed) {failed(failed);}
        });
    }
    private View result(GutenCatalog.Book book) {
        LinearLayout row=new LinearLayout(this);row.setPadding(dp(6),dp(8),dp(6),dp(8));row.setGravity(Gravity.CENTER_VERTICAL);
        Bitmap image=covers.get(book.id);
        FrameLayout thumbnail=new FrameLayout(this);
        if(image==null) { Button load=button(book.cover.isEmpty()?"No cover":"Cover",v -> thumbnail(book));load.setTextSize(11);load.setEnabled(!book.cover.isEmpty()&&!busy);load.setContentDescription("Load cover thumbnail for "+book.title);thumbnail.addView(load,new FrameLayout.LayoutParams(-1,-1)); }
        else {ImageView cover=new ImageView(this);cover.setImageBitmap(image);cover.setScaleType(ImageView.ScaleType.FIT_CENTER);cover.setContentDescription("Cover of "+book.title);thumbnail.addView(cover,new FrameLayout.LayoutParams(-1,-1));}
        row.addView(thumbnail,new LinearLayout.LayoutParams(dp(64),dp(96)));
        LinearLayout metadata=new LinearLayout(this);metadata.setOrientation(LinearLayout.VERTICAL);metadata.setPadding(dp(12),dp(8),dp(4),dp(8));metadata.setMinimumHeight(dp(96));
        TextView title=text(book.title,false);title.setTextSize(16);title.setMaxLines(2);title.setEllipsize(android.text.TextUtils.TruncateAt.END);metadata.addView(title);
        TextView author=text(book.author,false);author.setTextSize(13);author.setMaxLines(2);metadata.addView(author);
        metadata.setFocusable(true);metadata.setClickable(true);metadata.setBackground(ShelfUi.feedback(this));metadata.setContentDescription(book.title+", by "+book.author+". Book details.");metadata.setOnClickListener(v -> {if(!busy) details(book);});
        row.addView(metadata,new LinearLayout.LayoutParams(0,-2,1));return row;
    }
    private void details(GutenCatalog.Book book) {
        LinearLayout body=column();body.addView(text(book.title+"\n"+book.author+"\nProject Gutenberg #"+book.id+" · "+book.languages+"\n\n"+book.summary+"\n\n"+NOTICE,false));
        body.addView(button("Original Gutenberg book page",v -> external(GutenUrls.landing(book.id))));
        ScrollView scroll=new ScrollView(this);scroll.addView(body);
        AlertDialog dialog=new AlertDialog.Builder(this).setCustomTitle(ShelfUi.dialogTitle(this,"Book details"))
                .setView(scroll).setPositiveButton(book.epub.isEmpty()?"EPUB unavailable":"Download EPUB",(d,w)-> download(book))
                .setNegativeButton("Close",null).create();ShelfUi.show(this,dialog);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(!book.epub.isEmpty());
    }
    private void download(GutenCatalog.Book book) {
        if(busy || book.epub.isEmpty())return;
        operation=new GutenHttp.Operation();GutenHttp.Operation op=operation;committing=false;setBusy(true);status.setText("Checking library, then downloading one EPUB: "+book.title);
        File root=LibraryStore.root(getApplicationContext()),cache=new File(getCacheDir(),"gutenberg");
        worker.execute(() -> {
            try {
                GutenLibrary.Result result=GutenLibrary.acquire(book,root,cache,http,op,(received,total) -> runOnUiThread(() -> {
                    if(!alive()||committing)return;progress.setIndeterminate(total<=0);if(total>0) {progress.setMax(1000);progress.setProgress((int)(received*1000/total));}
                    status.setText("Downloading: "+(received/1024)+" KiB"+(total>0?" / "+(total/1024)+" KiB":"")+" · Cancel is available before import.");
                }),() -> runOnUiThread(() -> {if(!alive())return;committing=true;cancel.setEnabled(false);progress.setIndeterminate(true);status.setText("Validating EPUB and committing to the library. Please wait; this final atomic import cannot be cancelled.");}));
                runOnUiThread(() -> {LibraryStore.notifyChanged();if(!alive())return;committing=false;setBusy(false);status.setText((result.existing?"Already in your library: ":"Imported: ")+result.book.title+" "+result.warning);
                    AlertDialog dialog=new AlertDialog.Builder(this).setCustomTitle(ShelfUi.dialogTitle(this,result.existing?"Already in library":"EPUB imported"))
                            .setView(text(result.book.title+"\n"+result.warning,false)).setPositiveButton("Open Book",(d,w)->startActivity(new Intent(this,ReaderActivity.class).putExtra("book",result.book.id)))
                            .setNegativeButton("Keep browsing",null).create();ShelfUi.show(this,dialog);});
            } catch(Exception error) {failed(error);}
        });
    }
    private void thumbnail(GutenCatalog.Book book) {
        if(busy || book.cover.isEmpty())return;
        operation=new GutenHttp.Operation();GutenHttp.Operation op=operation;committing=false;setBusy(true);status.setText("Loading the requested cover thumbnail…");
        worker.execute(() -> {
            try {
                byte[] bytes=http.bytes(book.cover,GutenUrls.Kind.COVER,book.id,512*1024,op);
                BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
                if(options.outWidth<1 || options.outHeight<1 || options.outWidth>4096 || options.outHeight>4096)throw new IOException("Unsupported cover dimensions");
                options.inSampleSize=1;while(options.outWidth/options.inSampleSize>160 || options.outHeight/options.inSampleSize>240)options.inSampleSize*=2;
                options.inJustDecodeBounds=false;options.inPreferredConfig=Bitmap.Config.RGB_565;Bitmap image=BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
                if(image==null) throw new IOException("Cover could not be decoded");op.check();
                runOnUiThread(() -> {if(!alive())return;covers.put(book.id,image);setBusy(false);adapter.notifyDataSetChanged();status.setText("Cover loaded. Tap the book details to download its EPUB.");});
            } catch(Exception error) {failed(error);}
        });
    }
    private void failed(Exception failure) {runOnUiThread(() -> {if(!alive())return;committing=false;setBusy(false);status.setText(GutenHttp.message(failure));adapter.notifyDataSetChanged();});}
    private void information() {
        LinearLayout body=column();body.addView(text(NOTICE+"\n\nOnly your requested catalog page, cover or EPUB is fetched. No accounts, telemetry, automatic bulk fetching or background downloads. Requests use HTTPS; old-device TLS failures are reported without weakening security. Downloads stop when this screen is closed; an import already committing may finish safely.",false));
        body.addView(button("Gutendex API documentation",v -> external("https://gutendex.com/")));
        body.addView(button("Project Gutenberg access policies",v -> external("https://www.gutenberg.org/policy/terms_of_use.html")));
        ScrollView scroll=new ScrollView(this);scroll.addView(body);ShelfUi.show(this,new AlertDialog.Builder(this).setCustomTitle(ShelfUi.dialogTitle(this,"Catalog & sources")).setView(scroll).setNegativeButton("Close",null).create());
    }
    private void external(String url) {try {startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(ActivityNotFoundException absent){Toast.makeText(this,"No browser is installed. "+url,Toast.LENGTH_LONG).show();}}
    @Override protected void onSaveInstanceState(Bundle state) {state.putString("search",search.getText().toString());state.putString("language",language.getText().toString());super.onSaveInstanceState(state);}
    @Override protected void onStop() {if(busy && !committing && operation!=null) operation.cancel();super.onStop();}
    @Override protected void onDestroy() {if(operation!=null) operation.cancel();worker.shutdown();covers.evictAll();super.onDestroy();}
}
