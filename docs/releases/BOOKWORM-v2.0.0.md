# BOOKWORM v2.0.0

BOOKWORM is a free, lightweight, offline EPUB reader built to give older Android
devices a second life.

## What's new

- Beautiful physical bookshelf presentation, including optional spine shelves.
- Seven reading font choices, including EB Garamond, Literata, Atkinson Hyperlegible Next and Bitter.
- Four additional reading colour palettes, including Blue Night; Follow Appearance remains the default.
- Continue Reading on Launch, with safe fallback to the bookshelf.
- Improved paginated reading behaviour and reading-position restoration.
- Existing EPUB import, themes, covers and reading controls preserved.

## Compatibility

- Minimum Android version: Android 5.1 (API 22).
- Functionality hardware-tested on Samsung Galaxy Tab A (Android 5.1.1) and Pixel 6 (Android 17), as reported by the project owner.
- Additional device testing is ongoing.
- This version/signing build is verified locally; the exact release APK was not installed during release preparation.

## Project Gutenberg browsing

Deferred to a possible future v3 release. Unfinished browsing functionality is not
exposed in v2. Its implementation is retained for future development. Existing
downloaded/imported books and bundled Frankenstein remain readable offline.

## Upgrade and provenance

Package: `com.bookworm.reader`; versionName `2.0.0`; versionCode `11`.
The non-debuggable release APK retains the established BOOKWORM signing certificate
(historically named Android Debug). Update without uninstalling to preserve the
existing library, settings and reading positions.

Approved functional source: `b74086a1000e0de5a1e111ce7ef2cbfd11a2c3ce`.
The release commit adds only version/signing configuration and documentation.
The v1.0.0 tag and existing v1.0 artifacts are preserved unchanged.

**License:** MIT for BOOKWORM's original code; bundled fonts retain their SIL Open
Font Licenses. Bundled Frankenstein retains its Project Gutenberg notices and
license. Complete font notices are available in Reading settings and source assets.
