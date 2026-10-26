package com.bookworm.reader;

import java.io.IOException;
import java.net.*;

/** Discovery URLs and acquisition URLs are separate, tightly scoped trust boundaries. */
public final class GutenUrls {
    public enum Kind { CATALOG, EPUB, COVER }
    private GutenUrls() { }
    public static String query(String search,String language) throws IOException {
        search=search.trim(); language=language.trim().toLowerCase(java.util.Locale.US);
        if(search.length()>160 || !language.matches("|[a-z]{2}(,[a-z]{2}){0,4}")) throw new IOException("Use a short search and two-letter language codes, such as en or en,fr.");
        return "https://gutendex.com/books/?sort=popular&copyright=false&mime_type=application%2Fepub%2Bzip"
                +(search.isEmpty()?"":"&search="+URLEncoder.encode(search,"UTF-8"))
                +(language.isEmpty()?"":"&languages="+URLEncoder.encode(language,"UTF-8"));
    }
    public static String landing(int id) { return "https://www.gutenberg.org/ebooks/"+id; }
    public static URL checked(String value,Kind kind,int id) throws IOException {
        try {
            if(value==null || value.length()>2048) throw new IOException("Missing or oversized catalog URL");
            URI uri=new URI(value);
            if(!"https".equalsIgnoreCase(uri.getScheme()) || uri.getUserInfo()!=null || uri.getFragment()!=null
                    || (uri.getPort()!=-1 && uri.getPort()!=443)) throw new IOException("Only trusted HTTPS URLs are supported");
            String host=uri.getHost(), path=uri.getRawPath();
            if(host==null || path==null || path.contains("%") || path.contains("..") || path.contains("//") || path.contains("\\")) throw new IOException("Unsafe catalog path");
            if(kind==Kind.CATALOG) {
                if(!host.equalsIgnoreCase("gutendex.com") || !path.matches("/books/?")) throw new IOException("Untrusted catalog or pagination URL");
            } else {
                if(id<1 || !(host.equalsIgnoreCase("www.gutenberg.org") || host.equalsIgnoreCase("gutenberg.org")) || uri.getQuery()!=null)
                    throw new IOException("Unsupported book-file host");
                String prefix="/cache/epub/"+id+"/";
                boolean valid;
                if(kind==Kind.EPUB) valid=path.matches("/ebooks/"+id+"\\.epub(?:3)?(?:\\.(?:images|noimages))?")
                        || (path.startsWith(prefix) && path.substring(prefix.length()).matches("[A-Za-z0-9_.-]+\\.epub"));
                else valid=path.startsWith(prefix) && path.substring(prefix.length()).matches("[A-Za-z0-9_.-]+\\.(?:jpg|jpeg|png)");
                if(!valid) throw new IOException("Catalog file URL is not a supported EPUB/cover for this book");
            }
            return uri.toURL();
        } catch(URISyntaxException bad) { throw new IOException("Invalid catalog URL",bad); }
    }
}
