package com.bookworm.reader;

import java.util.Collections;
import java.util.Set;

public final class FontPrototypeRoutesTests {
    public static void main(String[] args) {
        Set<String> fonts = Collections.singleton("Literata-Regular.ttf");
        String origin = "https://bookworm.invalid";
        check("font-prototype/index.html".equals(FontPrototypeRoutes.asset(origin + FontPrototypeRoutes.INDEX, fonts)), "index");
        String path = "/font-prototype/fonts/Literata-Regular.ttf";
        check("font-prototype/fonts/Literata-Regular.ttf".equals(FontPrototypeRoutes.asset(origin + path, fonts)), "approved font");
        String[] blocked = {
            "http://bookworm.invalid" + path, "https://example.com" + path,
            "https://bookworm.invalid.example.com" + path, "https://bookworm.invalid:443" + path,
            "https://user@bookworm.invalid" + path, "file:///android_asset" + path,
            "content://bookworm.invalid" + path, origin + path + "?x=1", origin + path + "#fragment",
            origin + path + "?", origin + path + "#", origin + "/font-prototype/fonts/NotApproved.ttf",
            origin + "/font-prototype/fonts/../index.html", origin + "/font-prototype/fonts/%2e%2e/index.html",
            origin + "/font-prototype/fonts/%4citerata-Regular.ttf", origin + "/font-prototype/fonts/sub/Literata-Regular.ttf",
            origin + "/font-prototype/manifest.json", origin + "/font-prototype/licenses/Literata-OFL.txt",
            origin + "/other/Literata-Regular.ttf", origin + "/font-prototype/fonts/Literata-Regular.ttf/",
            "javascript:alert(1)", "data:text/html,hello", "not a URL", "", null
        };
        for (String url : blocked) check(FontPrototypeRoutes.asset(url, fonts) == null, "deny " + url);
        System.out.println("PASS: 2 allowed routes; " + blocked.length + " unsafe/unapproved routes denied");
    }
    private static void check(boolean value, String label) { if (!value) throw new AssertionError(label); }
}
