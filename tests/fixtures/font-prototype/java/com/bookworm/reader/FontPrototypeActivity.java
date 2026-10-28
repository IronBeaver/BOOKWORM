package com.bookworm.reader;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Removable debug fixture; never reads or writes library or reader preferences. */
public final class FontPrototypeActivity extends Activity {
    private final Set<String> fonts = new HashSet<>();
    private final Set<String> opened = Collections.synchronizedSet(new HashSet<String>());
    private final List<String> messages = Collections.synchronizedList(new ArrayList<String>());
    private JSONArray licenses;
    private byte[] document;
    private String script;
    private WebView web;
    private TextView routes;
    private boolean destroyed;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        try {
            JSONObject manifest = new JSONObject(read("font-prototype/manifest.json"));
            licenses = manifest.getJSONArray("licenses");
            JSONArray entries = manifest.getJSONArray("fonts");
            StringBuilder css = new StringBuilder(), samples = new StringBuilder();
            String previous = "";
            for (int i = 0; i < entries.length(); i++) {
                JSONObject f = entries.getJSONObject(i);
                String file = f.getString("file"), family = f.getString("family"), face = f.getString("face");
                if (!file.matches("[A-Za-z]+-(Regular|Italic|Bold|BoldItalic)\\.ttf") || !fonts.add(file))
                    throw new IOException("Invalid prototype manifest");
                String alias = "BWProbe" + i, weight = face.startsWith("Bold") ? "700" : "400";
                String style = face.contains("Italic") ? "italic" : "normal";
                css.append("@font-face{font-family:").append(alias).append(";font-weight:").append(weight)
                    .append(";font-style:").append(style).append(";src:url('fonts/").append(file)
                    .append("') format('truetype');}\n");
                if (!family.equals(previous)) samples.append("<h2>").append(escape(family)).append("</h2>");
                previous = family;
                samples.append("<section data-face='").append(alias).append("' data-weight='").append(weight)
                    .append("' data-style='").append(style).append("'><h3>").append(escape(face))
                    .append(" <span class='face-state'>Waiting for font measurement</span></h3>")
                    .append("<div class='specimen' style='font-family:").append(alias).append(",serif;font-weight:")
                    .append(weight).append(";font-style:").append(style).append("'>")
                    .append("<p>The quiet library holds stories of distant gardens. Affinity, office, first.</p>")
                    .append("<p>ÀÉÎÖÜ æ œ ß ẞ — café, naïve, façade; Łódź, český, ő ű, Șș Țț, İı.</p>")
                    .append("<p>Decomposed accents: e&#x301; a&#x308; c&#x327; o&#x30B;. “Quoted words” ‘apostrophes’ … € £ – — ! ?</p>")
                    .append("<p>Fallback / coverage: Ελληνικά · Русский · 日本語 · العربية · 🙂</p>")
                    .append("</div></section>");
            }
            if (fonts.size() != 16) throw new IOException("Expected 16 approved font faces");
            document = read("font-prototype/index.html").replace("#FONT_FACES#", css.toString())
                .replace("#SAMPLES#", samples.toString()).getBytes(StandardCharsets.UTF_8);
            script = read("font-prototype/probe.js");
            createScreen();
        } catch (Exception failure) {
            new AlertDialog.Builder(this).setTitle("Font prototype unavailable").setMessage(failure.toString())
                .setPositiveButton("Back", (d, w) -> finish()).setOnCancelListener(d -> finish()).show();
        }
    }

    @android.annotation.SuppressLint("SetJavaScriptEnabled") // Trusted fixture script injected as in ReaderActivity.
    private void createScreen() {
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(245, 235, 212));
        LinearLayout actions = new LinearLayout(this);
        action(actions, "Back", this::finish);
        action(actions, "Reload", () -> web.reload());
        action(actions, "Diagnostics", this::diagnostics);
        action(actions, "Licenses", this::showLicenses);
        root.addView(actions);
        routes = new TextView(this); routes.setTextColor(Color.rgb(48, 41, 31)); routes.setTextSize(12);
        int pad = (int) (8 * getResources().getDisplayMetrics().density + 0.5f);
        routes.setPadding(pad, 0, pad, pad); root.addView(routes); updateRoutes();
        web = new WebView(this); web.setBackgroundColor(Color.rgb(245, 235, 212));
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true); s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setAllowFileAccess(false); s.setAllowContentAccess(false);
        s.setAllowFileAccessFromFileURLs(false); s.setAllowUniversalAccessFromFileURLs(false);
        s.setBlockNetworkLoads(true); s.setBlockNetworkImage(true);
        s.setDomStorageEnabled(false); s.setDatabaseEnabled(false); s.setCacheMode(WebSettings.LOAD_NO_CACHE);
        s.setSupportMultipleWindows(false); s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setMediaPlaybackRequiresUserGesture(true); s.setTextZoom(100);
        web.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, String url) { return resource(url); }
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return "GET".equals(request.getMethod()) ? resource(request.getUrl().toString()) : blocked();
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) { return true; }
            @Override public void onPageFinished(WebView view, String url) {
                if (!destroyed && (FontPrototypeRoutes.ORIGIN + FontPrototypeRoutes.INDEX).equals(url))
                    view.evaluateJavascript(script, null);
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onConsoleMessage(ConsoleMessage message) {
                synchronized (messages) {
                    if (messages.size() < 24) messages.add(message.messageLevel() + ": " + message.message());
                }
                return true;
            }
        });
        root.addView(web, new LinearLayout.LayoutParams(-1, 0, 1)); setContentView(root);
        web.loadUrl(FontPrototypeRoutes.ORIGIN + FontPrototypeRoutes.INDEX);
    }

    private WebResourceResponse resource(String url) {
        String asset = FontPrototypeRoutes.asset(url, fonts);
        if (asset == null) return blocked();
        try {
            if (asset.endsWith("index.html")) return response("text/html", "UTF-8", new ByteArrayInputStream(document));
            InputStream stream = getAssets().open(asset);
            opened.add(Uri.parse(url).getLastPathSegment());
            runOnUiThread(() -> { if (!destroyed) updateRoutes(); });
            return response("application/x-font-ttf", null, stream);
        } catch (IOException missing) { return blocked(); }
    }
    private WebResourceResponse response(String mime, String encoding, InputStream stream) {
        return new WebResourceResponse(mime, encoding, 200, "OK",
            Collections.singletonMap("Cache-Control", "no-store"), stream);
    }
    private WebResourceResponse blocked() {
        return new WebResourceResponse("text/plain", "UTF-8", 403, "Blocked", Collections.emptyMap(),
            new ByteArrayInputStream(new byte[0]));
    }
    private void updateRoutes() {
        routes.setText("API " + Build.VERSION.SDK_INT + " · Font routes opened: " + opened.size()
            + "/16 since launch. Route access alone does not prove font rendering.");
    }
    private void action(LinearLayout row, String label, Runnable action) {
        Button b = new Button(this); b.setText(label); b.setTextSize(12); b.setContentDescription(label);
        b.setMinWidth(0); b.setMinimumWidth(0); b.setMinHeight((int) (48 * getResources().getDisplayMetrics().density));
        b.setOnClickListener(v -> action.run()); row.addView(b, new LinearLayout.LayoutParams(0, -2, 1));
    }
    private void diagnostics() {
        StringBuilder text = new StringBuilder("Android ").append(Build.VERSION.RELEASE).append(" / API ")
            .append(Build.VERSION.SDK_INT).append("\n").append(Build.MANUFACTURER).append(' ').append(Build.MODEL)
            .append("\n\n").append(web.getSettings().getUserAgentString())
            .append("\n\nOpened font routes: ").append(opened.size()).append("/16\n")
            .append("The missing-font negative control is intentionally blocked. Reload can reuse in-memory fonts.\n\n");
        synchronized (messages) { for (String message : messages) text.append(message).append("\n"); }
        new AlertDialog.Builder(this).setTitle("Font test diagnostics").setMessage(text.toString())
            .setPositiveButton("Close", null).show();
    }
    private void showLicenses() {
        String[] names = new String[licenses.length()];
        for (int i = 0; i < names.length; i++) names[i] = licenses.optJSONObject(i).optString("family");
        new AlertDialog.Builder(this).setTitle("Font licenses — OFL 1.1").setItems(names, (d, index) -> {
            try {
                String notice = read("font-prototype/licenses/" + licenses.getJSONObject(index).getString("file"));
                new AlertDialog.Builder(this).setTitle(names[index]).setMessage(notice).setPositiveButton("Close", null).show();
            } catch (Exception failure) {
                new AlertDialog.Builder(this).setMessage("Could not read license: " + failure).setPositiveButton("Close", null).show();
            }
        }).show();
    }
    private String read(String asset) throws IOException {
        try (InputStream in = getAssets().open(asset); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096]; int n;
            while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }
    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
    @Override protected void onPause() { if (web != null) web.onPause(); super.onPause(); }
    @Override protected void onResume() { super.onResume(); if (web != null) web.onResume(); }
    @Override protected void onDestroy() {
        destroyed = true;
        if (web != null) { ((LinearLayout) web.getParent()).removeView(web); web.stopLoading(); web.destroy(); web = null; }
        super.onDestroy();
    }
}
