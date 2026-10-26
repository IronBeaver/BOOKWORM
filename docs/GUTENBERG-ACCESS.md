# Catalog discovery and file acquisition — v1.2

Policy/API review: 2026-10-26. This is a personal-reader implementation, not
Project Gutenberg approval or an assurance that servers will accept every request.

## Current reference-tablet result

The owner reports a Gutendex catalog timeout on Samsung SM-T280 / Android 5.1.1. The cause is not established. The desktop success below is not evidence of tablet networking compatibility. No diagnosis or repair was attempted during repository preparation.

## Sources and route decision

[Gutendex documentation](https://gutendex.com/) describes JSON metadata, popularity,
title/author search, language filters, pagination and MIME-to-file URLs. BOOKWORM
uses that API exclusively for discovery. The public instance is used for this
personal/test build; its author recommends running your own server for long-term
use. No hosted catalog service, mirror, API SLA or endpoint customization is supplied.

[Gutenberg robot access](https://www.gutenberg.org/policy/robot_access.html) warns
against automated website crawling and gives dedicated harvesting/mirroring routes.
[Gutenberg offline catalogs](https://www.gutenberg.org/ebooks/offline_catalogs.html)
also describes app-oriented machine access and machine-readable metadata.
BOOKWORM does not crawl HTML, use the main search pages, harvest links or mirror a
catalog. The bulk harvesting route is inappropriate for downloading a single book.

[Gutenberg terms](https://www.gutenberg.org/policy/terms_of_use.html) describe
browser-like request volumes for reading applications, identify bulk usage as a
different use case, and explain blocks. We interpret an explicit foreground tap
for one EPUB as user-directed acquisition, not a crawler: no automatic books,
retries, prefetches or background harvesting. This interpretation is not a special
permission from Gutenberg. Any HTTP 403 is reported without bypass. A public
release should recheck the policies and provide a reachable maintainer contact;
the OPDS-specific contact requirement is relevant if OPDS is added later. This
implementation does not use OPDS.

[Gutenberg linking policy](https://www.gutenberg.org/policy/linking.html) calls for
canonical book landing pages in public links and discourages image hotlinking.
The app opens https://www.gutenberg.org/ebooks/ID in the external browser. Binary
EPUB acquisition uses the exact application/epub+zip URL supplied by Gutendex,
not a guessed path. Redirects from that URL are validated at every hop. Cover
thumbnails are requested individually by the user and decoded into a bounded
local memory cache, never automatically fetched in result batches or inlined
from a remote URL in a WebView. No mirror URL substitution or HTML fallback is used.

On this date, the actual Gutendex record for #84 supplied
https://www.gutenberg.org/ebooks/84.epub3.images. One explicit desktop smoke test
downloaded through this route, imported it and sanitized all 32 chapters using
production transport/import code. This proves the route worked from this desktop
network on that date, not its permanent availability or approval for bulk access.

## Security and resource limits

- INTERNET is the only permission. HTTPS only; default platform trust and hostname
  validation. No custom CA, trust manager, browser UA impersonation or certificate
  bypass. Downloads are not performed by WebView; existing EPUB WebView networking
  remains blocked, and publisher content is handled by the unchanged sanitizer.
- Discovery/pagination: exact gutendex.com/books path, maximum 2MiB per response,
  32 entries per page, bounded JSON depth/nodes/field lengths. Next/previous URLs
  come from the response. Every page is manually requested; a session is bounded
  to 100 distinct pages and retains only the current result set.
- Acquisition: only HTTPS www.gutenberg.org or gutenberg.org, with an EPUB/cover
  path scoped to the selected ID, no credentials, query, fragment or nonstandard
  port. Other catalog hosts/formats are deliberately unavailable. No arbitrary
  URL entry or downloader intent is exported.
- At most three redirects, 15s connect and 20s read timeouts, 90s total metadata/
  cover transfer budget and 5min EPUB transfer budget. A currently blocked read
  can take up to the read timeout beyond that overall budget. Metadata is 2MiB,
  thumbnails 512KiB with dimensions at most 4096 square, EPUB 64MiB. All bodies
  are bounded even without Content-Length; truncated or encoded responses fail.
- Requests are spaced at least two seconds apart. 429/503 responses establish
  a process-wide cooldown from Retry-After (minimum one minute, capped at 24h).
  There are no automatic retries. Restart clears that in-memory cooldown, so users
  should still honor the server's requested wait; long Retry-After periods should
  be honored manually beyond the local cap.
- Only one EPUB acquisition/import may run in the process. Downloads go to a
  unique private cache .part file. Failed/cancelled jobs remove it; stale matching
  private partials from process interruption are cleared before a new acquisition.
  The existing atomic, bounded ZIP/XML importer validates and commits the book.
  Final validation/commit is visibly noncancelable to avoid interrupting its
  existing transaction. Android may terminate a process; existing importer staging
  recovery remains unchanged. No original EPUB contents are rewritten.
- Gutenberg identity is detected from existing OPF identifier URLs (including
  older /84 form). A small atomic provenance sidecar supplements identifiers for
  catalog downloads; book hashes, metadata schema and saved positions do not change.
  Matching is checked before and after transfer. Unknown manually imported editions
  without identifiers cannot be reliably matched by Gutenberg ID; existing byte-hash
  import deduplication still applies. A conflicting embedded ID is warned about,
  with no incorrect selected-ID association saved.

## Android 5.1.1 and honest validation limits

[Android SSLSocket documentation](https://developer.android.com/reference/javax/net/ssl/SSLSocket)
lists TLS 1.2 support from API16 and client default enablement from API20. That
does not establish server compatibility on the Samsung's old trust store, cipher
suites, vendor TLS stack, clock or local network. No tablet/emulator networking
was tested, no roots were added and no trust checks were disabled. The desktop
live test used JDK17 trust/TLS. An old-device TLS failure produces an explicit
message and leaves the existing library readable; desktop download plus SAF Import
EPUB remains available. Some valid catalog books may be unavailable because of
host/path restrictions, server policy, TLS or existing parser limitations.

Offline tests use queued HttpsURLConnection mocks (no local HTTP trust exception)
and a host-only org.json adapter using already installed Gradle/Groovy libraries.
They execute production field parsing, transport and acquisition, but not Android's
actual org.json implementation, TLS or native UI. No test adapter/dependency is
packaged in the APK. No service, telemetry, account or background worker is added.
