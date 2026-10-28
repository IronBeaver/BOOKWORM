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

## v1.2 online discovery and user-selected acquisition

Gutendex (https://gutendex.com/) is an independent JSON catalog API derived from
Project Gutenberg metadata. It is not an official Project Gutenberg API.
BOOKWORM uses the API without bundling Gutendex server code; no Gutendex code or
new runtime library dependency is distributed. Its author recommends a separately
operated instance for long-term use; the public instance is used for this personal build.

Project Gutenberg books are generally free of US copyright restrictions. Users
outside the US must check their local copyright laws before downloading. The
catalog's copyright classification is not a warranty for another jurisdiction.
No affiliation, endorsement or ownership of Gutenberg books/artwork is claimed.
Each acquired EPUB remains unmodified, including embedded notices and license.
Public book references use https://www.gutenberg.org/ebooks/ID.

Access policy/API sources and implementation limits are documented in
[docs/GUTENBERG-ACCESS.md](docs/GUTENBERG-ACCESS.md). These should be reviewed before
public distribution or increased usage. No scraping, bulk acquisition or mirror
service is part of BOOKWORM. Local JSON test adapters use existing development-tool
libraries only; they are not included in the Android APK.

## Build tooling and original application code

The original BOOKWORM implementation is MIT, copyright 2026 Norm R Langevin.
This does not relicense the bundled EPUB, its artwork or third-party tooling.
Gradle8.9 wrapper/scripts retain their upstream notices and Apache2.0 terms;
full upstream LICENSE and NOTICE are under LICENSES/. System fonts are referenced,
not bundled. HTML named-entity mappings are generated standard character facts,
with their JDK-table provenance retained; no JDK implementation is redistributed.
See docs/LICENSING.md for the component audit and territorial book-rights review.

## Bundled reader fonts — SIL Open Font License 1.1

The following font software is redistributed unmodified under SIL OFL 1.1,
separately from BOOKWORM's MIT application code. Complete original license notices:

- **EB Garamond**: Copyright 2017 The EB Garamond Project Authors
  (https://github.com/octaviopardo/EBGaramond12). The upstream notice also preserves
  Copyright 2025 Deborah Khodanovich (RCS citation glyphs).
  [Complete notice](app/src/main/assets/font-licenses/EBGaramond-OFL.txt).
- **Literata**: Copyright 2017 The Literata Project Authors
  (https://github.com/googlefonts/literata).
  [Complete notice](app/src/main/assets/font-licenses/Literata-OFL.txt).
- **Atkinson Hyperlegible Next**: Copyright 2020–2024 The Atkinson Hyperlegible Next
  Project Authors (https://github.com/googlefonts/atkinson-hyperlegible-next).
  [Complete notice](app/src/main/assets/font-licenses/AtkinsonHyperlegibleNext-OFL.txt).
- **Bitter**: Copyright 2011 The Bitter Project Authors
  (https://github.com/solmatas/BitterPro), Reserved Font Name **Bitter Pro**.
  [Complete notice](app/src/main/assets/font-licenses/Bitter-OFL.txt).

Exact file URLs, sizes and SHA-256 hashes are retained in the
[approved manifest](tests/fixtures/font-prototype/assets/font-prototype/manifest.json).
No runtime font service is used. The four notices are also accessible in Reading
settings → Font licenses. System font choices redistribute no font files.
