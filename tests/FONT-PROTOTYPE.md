# Stage 2: debug-only font compatibility fixture

**Archived after Stage 2 passed on the original Samsung Galaxy Tab A / Android
5.1.1 (user-confirmed): READY 16/16; missing-font control NOT detected; all faces
render correctly; native selection and handles work.** This is evidence for the
isolated fixture, not yet for Stage 3A production integration.

The historical fixture source is now under `tests/fixtures/font-prototype`, outside
all Android source sets. Its 16 unchanged fonts and four licenses moved into
`app/src/main/assets/fonts` and `app/src/main/assets/font-licenses`. Its approved
manifest remains under `tests/fixtures/font-prototype/assets/font-prototype`.
The Stage 2 APK (2,152,878 bytes; SHA-256
`cd4f132fc21da7f6d62bd0ba77ef1556e9a2c4a97d0705d55c566ad8ecce3a6f`)
is retained locally at `local-validation/font-prototype-stage2/BOOKWORM-stage2-font-test.apk`
(excluded from Git and outside Gradle clean outputs). Source, manifest and this
evidence are preserved separately. The current production APK has no Font Test launcher.

The instructions below describe the historical Stage 2 APK. For the current APK,
open a book, show reader controls, choose Reading settings, select a font and Apply.
`check-font-prototype.ps1` now checks the same approved bytes at their production
locations; `check-reader-fonts.ps1` adds production preference and routing tests.

This removable fixture lives in `app/src/debug`. Release builds exclude its Java,
manifest entries and assets. Production reader, preferences and bookshelf are unchanged.
No font selector or Reading Comfort settings are introduced.

## Open on the Samsung

After you install the development APK using your normal manual process, open the
Android app drawer and tap **BOOKWORM Font Test** (a second launcher entry using the
BOOKWORM icon). The ordinary **BOOKWORM** entry opens the unchanged application.
The test entry's Back button and Android Back close the fixture.

1. Turn off connectivity and open Font Test from a cold app start.
2. Wait for the measurement gate to say **READY: 16/16**. The missing-font negative
   control must say **NOT detected**. Route counts alone are not a pass.
3. Scroll through four styles of each of the four bundled families. Confirm genuine
   visual differences, regular/bold weight, italics, legible accents, punctuation,
   ligatures and no clipped lines. Compare system reference samples at the bottom.
4. Check decomposed accents and Greek, Cyrillic, Japanese, Arabic and emoji samples.
   Fallback or tofu is possible for unsupported characters; record actual results.
5. Long-press each family's text, drag native selection handles in both directions,
   and copy text. Confirm combining accents and ligatures do not break selection.
6. Tap **Diagnostics** and record Android/device details, the WebView user agent,
   opened font count and any decoding/sanitizer messages. One missing-font error is
   intentional. Tap **Reload**, leave/reopen the screen, and repeat after a cold start.
7. Open **Licenses** and verify that all four complete notices are readable.
8. Return to ordinary BOOKWORM and confirm existing reading/settings still behave as
   before. No install, hardware testing or selection validation was automated here.

## Loading and security

Only exact HTTPS `bookworm.invalid/font-prototype/index.html` and 16 allowlisted
font paths are intercepted. Other hosts, schemes, queries, fragments, encoded paths,
traversal and unapproved assets return 403. Network/file/content access is disabled.
The HTML CSP allows local fonts and inline fixture CSS, blocks document scripts and
other resources. As in the production reader, trusted packaged JavaScript is injected
by native code after `onPageFinished`; there is no JavaScript interface or external script.

The ES5 observer measures each style against serif, sans-serif and monospace fallback
probes. Matching candidate widths across the three fallbacks, with a difference from
at least one baseline, signals font application. All 16 must be detected and geometry
stable for three rendering frames before the hypothetical pagination gate opens.
There is no fixed font-load delay, `FontFace`, promise or `document.fonts` dependency.
A 30-second diagnostic limit fails closed as **INCONCLUSIVE**, never as ready.
System fonts with indistinguishable metrics can also make the result inconclusive.
This is evidence of application, not proof of every glyph, shaping correctness,
native selection or actual production pagination. The latter remains untouched.

## Provenance and local checks

`assets/font-prototype/manifest.json` records the approved exact download URLs,
sizes and SHA-256 hashes. Google CDN versions: EB Garamond v33/font 1.003,
Literata v40/font 3.103, Bitter v42/font 3.021. Atkinson Next 2.001 uses upstream
commit `7925f50f649b3813257faf2f4c0b381011f434f1`.

Complete unmodified OFL files come from pinned upstream commits:

- EB Garamond: `106a4a6d377987459ae5e68673a4570f13b957fb`
- Literata: `0c2761b727a1b3a7cffd313c37f0f5163dfc7a63`
- Atkinson Next: `7925f50f649b3813257faf2f4c0b381011f434f1`
- Bitter: `3238d7ae2cb0b564b81225d68b3c893a40b1d3ce`

With JDK 17 in the process-local JAVA_HOME, run:

```powershell
.\tests\check-font-prototype.ps1
.\gradlew.bat --no-daemon --console=plain --offline :app:assembleDebug :app:lintDebug
.\tests\check-font-prototype.ps1 -CheckApk
```

No application version, identity, signing configuration or dependency is changed.
Remove `app/src/debug` and the three prototype-specific test/documentation files to
remove the fixture. Do not remove font licenses separately while fonts remain packaged.
