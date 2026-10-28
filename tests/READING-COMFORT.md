# Stage 3B: reader palettes and Continue reading on launch

**Hardware approved:** the user confirmed the latest debug build passed Samsung
Galaxy Tab A / Android 5.1.1 testing, including typography, pagination, reader
palettes and Continue Reading on Launch. The checklist below remains a regression
procedure; local automated checks do not themselves execute on the tablet.

Both controls are in Reading settings and use its existing Apply/discard flow.
Fonts, size, margins, spacing and application appearance are independent of the
reader palette. Follow Appearance remains the default and emits exactly the prior
reading CSS. No colour picker or new dependency is added.

## Stage 3B files

Production changes:

- `MainActivity.java`: fresh-launch lifecycle hooks only; shelf rendering unchanged.
- `ReaderActivity.java`: reader-local palette/native accents and successful-open identity/fallback hooks.
- `ReaderPreferences.java`, `ReadingSettingsActivity.java`, `res/values/ids.xml`: two independent persisted settings and controls.
- New `ReaderPalettes.java`, `ContinueReadingStore.java`, `LaunchAttempt.java`, `res/values/reader-palettes.xml`.

Validation/documentation changes: README.md; this file; READER-FONTS.md;
ReaderComfortTests.java; check-reader-comfort.ps1; ReaderFontTests.java;
check-reader-fonts.ps1; run-reader-font-browser-tests.ps1; fixtures/stage3a.
The pre-existing uncommitted Stage 3A changes/assets are retained, not replaced.

| Palette | Background | Text | Link, selection and native-handle accent |
|---|---|---|---|
| Warm Paper | #F5EBD4 | #30291F | #795322 |
| Soft Blue | #DCEAF2 | #213B54 | #245478 |
| Blue Night | #183F73 | #FFFFFF | #FFE0A0 |
| Soft Dark | #252A32 | #E8E8E3 | #B9D8FF |

Selection uses opaque accent background and palette-background text. Reader-only
native theme resources set colorAccent for selection handles; changing a palette
updates the Activity theme before creating the new WebView. API 22 has white system
bar icons, so the new light palettes use dark system bars. Follow Appearance retains
existing behaviour. Body, links, selected text and progress colour contrast are
checked automatically; native pixels/handles still require hardware inspection.

## Launch lifecycle and storage

The existing MainActivity is still the launcher and library underneath the reader.
Only onCreate with no saved state, task-root status and MAIN/LAUNCHER intent grants
a one-time launch opportunity. Its existing library worker supplies the snapshot;
no extra scan or EPUB parsing is added. Pausing MainActivity cancels the opportunity,
including callbacks from an in-flight scan. onResume never grants a new opportunity.
Android's existing-task restoration is left intact; see the official
[task/back-stack documentation](https://developer.android.com/guide/components/activities/tasks-and-back-stack).

`reading.continue_on_launch` defaults true. `reading.palette_id` defaults appearance.
The new `reading_launch` store contains only `last_book` and `pending_book` identity
metadata, never chapter/offset values. ReadingPosition and all library files keep
their existing formats and remain the sole source of saved reading positions.

For existing installations without an identity, the latest existing read timestamp
is used once (stable ID tie-break). Unread books are excluded. Ambiguous damaged
legacy libraries remain on the shelf. A known missing identity never falls back to
a different older book. Removed history from before this feature cannot be rebuilt.

Before automatic navigation, a durable pending marker is saved. The first successful
font-gated layout clears it and records the book; ready readers also update identity
when returning to the foreground. A failed/interrupted attempt is not retried on
the next fresh launch. Open a book manually to reset that guard. The same MainActivity
stays underneath, so Library/Back works and does not restart automatic navigation.
Changing fonts/palettes continues to use the original text-anchor reload flow.

## Automated checks

```powershell
.\tests\check-reader-comfort.ps1
.\tests\run-reader-font-browser-tests.ps1
.\gradlew.bat --no-daemon --console=plain --offline :app:assembleDebug :app:lintDebug
.\tests\check-reader-comfort.ps1 -CheckApk
```

Host tests cover all five palettes, seven fonts, both modes, persistence, defaults,
contrast, cold-task/restore/background opportunity state, no previous read, missing
records/chapters, failed commits, interrupted/repeated opens and untouched position
files. Browser tests use production CSS/font gate/pagination/input scripts, checking
all 70 combinations, selection colours/guards, delayed fonts and anchor restoration.
They substitute data fonts for the Android route and do not run Android lifecycle
or native selection handles. Snapshot guards protect approved Stage 3A engine,
font-gate and map files; bookshelf rendering methods remain byte-identical.

## Hardware regression checklist

1. Upgrade manually without uninstalling. Confirm themes, fonts, books and positions.
2. Try all five palettes in multiple fonts and both modes; Apply around a distinctive
   passage, close/reopen, and confirm palette/font/passage persist independently.
3. Inspect links, footnotes, selected text, selection handles, native controls and
   system-bar icons in each palette, including palette changes within one reader.
4. Test tap zones/page locking, vertical scrolling, TOC and chapter boundaries.
5. With Continue ON, read a book, leave the task completely (swipe it away), then
   launch BOOKWORM. Confirm that book and its saved passage open. Library/Back must
   return to the shelf and stay there.
6. With Continue OFF and Applied, repeat a fresh-task launch: it must stay on the shelf.
7. Use Home/Recents/launcher to resume a running task on the shelf/settings/reader:
   no automatic screen/book change. Background during initial library loading too.
8. Test an empty/unread library and a known unavailable remembered book in disposable
   test data. Do not delete the user's real library to create a failure test.
9. Interrupt an automatic open before readiness, relaunch, and confirm safe shelf
   fallback; manually open a book and confirm continuation is enabled again.

No installation or hardware execution is performed by these local checks. This
hardware-approved development build is not a tagged release.
