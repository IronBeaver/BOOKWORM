# Third-party notice — bundled Frankenstein

BOOKWORM includes, at no charge, the unmodified older-reader EPUB of Frankenstein;
or, The modern Prometheus by Mary Wollstonecraft Shelley, Project Gutenberg eBook
#84. No Project Gutenberg affiliation or endorsement is claimed.

Official source: https://www.gutenberg.org/ebooks/84
Downloaded edition: https://www.gutenberg.org/ebooks/84.epub.images
Retrieved October 26, 2026; file metadata updated February 10, 2026.
Project Gutenberg's catalogue identifies the work as public domain in the USA.
The original notice addresses use outside the USA; it is retained in full.

Credits retained from the original:
Judith Boss, Christy Phillips, Lynn Hanninen and David Meltzer.
HTML version by Al Haines. Further corrections by Menno de Leeuw.

The complete original EPUB, including its attached full Project Gutenberg license,
is packaged without alteration at app/src/main/assets/books/frankenstein.epub.
SHA-256: eb696684c1314092bfe973be1241ab1f44cde101ac19bc36a574893de24016c2

Additional offline copies of the original notice and full license text are at:

- app/src/main/assets/books/gutenberg-notice.txt
- app/src/main/assets/books/gutenberg-license.txt

The app exposes the notice before reading the bundled book, and exposes the full
license through About Frankenstein and the reader's source/license button. The
original EPUB's license section also remains in its normal reading order.

Distribution is intended to remain free of charge with these notices and the
complete license attached. Do not detach the original license or attribution.
Before distributing this book as part of a paid product or on different terms,
review https://www.gutenberg.org/policy/license.html and obtain any required
permission. The included work is not covered by any separate license that may be
chosen for BOOKWORM application code.

## Build tooling and original application code

The original BOOKWORM implementation is MIT, copyright 2026 Norm R Langevin.
This does not relicense the bundled EPUB, its artwork or third-party tooling.
Gradle8.9 wrapper/scripts retain their upstream notices and Apache2.0 terms;
full upstream LICENSE and NOTICE are under LICENSES/. System fonts are referenced,
not bundled. HTML named-entity mappings are generated standard character facts,
with their JDK-table provenance retained; no JDK implementation is redistributed.
See docs/LICENSING.md for the component audit and territorial book-rights review.
