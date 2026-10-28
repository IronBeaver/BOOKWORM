package com.bookworm.reader;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

/** Exact, debug-only asset allowlist. Every other request fails closed. */
final class FontPrototypeRoutes {
    static final String ORIGIN = "https://bookworm.invalid";
    static final String INDEX = "/font-prototype/index.html";
    private static final String FONTS = "/font-prototype/fonts/";

    static String asset(String url, Set<String> fonts) {
        try {
            URI uri = new URI(url);
            if (!"https".equals(uri.getScheme()) || !"bookworm.invalid".equals(uri.getRawAuthority())
                    || uri.getRawQuery() != null || uri.getRawFragment() != null) return null;
            String path = uri.getRawPath();
            if (INDEX.equals(path)) return path.substring(1);
            if (path != null && path.startsWith(FONTS) && fonts.contains(path.substring(FONTS.length())))
                return path.substring(1);
        } catch (URISyntaxException | NullPointerException invalid) { /* Deny malformed requests. */ }
        return null;
    }
}
