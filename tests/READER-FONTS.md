# Stage 3A production typography

Seven fonts are available in the existing Reading settings screen. System Serif
remains the new-user default; a legacy `serif=false` remains System Sans. Loading
old preferences does not write migration data. Apply saves an additive `font_id`
alongside the retained legacy key and other unchanged reading settings.

Only the selected bundled family's four static faces are declared and served.
The 16 Stage 1-approved hashes are still recorded in the archived Stage 2 manifest.
Four complete unmodified OFL notices are in main assets and accessible through
Reading settings → Font licenses. There is no runtime font download.

The trusted ES5 font barrier is separate from `reader.js`. It checks four actual
font-style measurements against three generic fallback baselines, then waits for
three stable rendering frames. System families need no bundled-font barrier.
Probe spans are outside chapter text and removed before the original `BW.init`.
The reader's universal image/content width constraint is explicitly overridden
only on invisible measurement spans, so narrow screens cannot clamp their widths.

Native polling observes the barrier; a polling interval never authorizes pagination.
A diagnostic timeout fails closed, keeps the saved anchor untouched and exposes
Reading settings so the user can choose a system font. Generation and WebView
identity guards reject stale callbacks. Existing text offsets, page/scroll engine,
touch listeners, selection guards and storage methods remain unchanged.

## Local validation

With JDK 17 in process-local JAVA_HOME:

```powershell
.\tests\check-reader-fonts.ps1
.\tests\run-reader-font-browser-tests.ps1
.\gradlew.bat --no-daemon --console=plain --offline :app:assembleDebug :app:lintDebug
.\tests\check-reader-fonts.ps1 -CheckApk
```

The browser test uses the actual production preference CSS, font barrier,
pagination engine and input guard. It substitutes data-font URLs for Android's
native intercepted route, testing all seven choices in both modes, delayed font
availability, text-anchor restoration, selection guards, turns, scrolling,
cancellation and fail-closed timeout. This is desktop evidence, not Android UI,
native selection-handle or API 22 execution. Historical milestone scripts that
freeze the entire old reader CSS/resource method are intentionally not rewritten;
the new guard compares unaffected reader methods against approved develop HEAD.

## Required Samsung validation before committing

1. Update manually without uninstalling. Confirm existing serif/sans appearance,
   books, progress, themes and bookshelf presentation are retained.
2. In a book, show controls → Reading settings → Reading font. Try each choice,
   Apply, and confirm the same approximate passage remains visible. Repeat in
   page and continuous-scroll modes, including near chapter boundaries.
3. Close/reopen the book and restart BOOKWORM. Confirm font and passage persist.
4. Check bold, italic, bold italic, accented/decomposed characters, fallback,
   internal links, TOC, chapter navigation and unchanged font-size/margin/spacing.
5. Recheck taps, page locking, long-press selection and native selection handles.
6. Test cold offline opening, repeated font switches and responsiveness. Confirm
   there is no Font Test launcher. Font licenses must remain readable in settings.

Stage 2 font rendering/selection evidence is retained in FONT-PROTOTYPE.md and
fixtures/font-prototype. Stage 3A integrated typography passed user-confirmed Samsung
API 22 hardware testing: all seven fonts render and paginate properly. Stage 3A
source/guard evidence is retained in fixtures/stage3a before Stage 3B changes.
