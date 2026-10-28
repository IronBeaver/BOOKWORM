# BOOKWORM: Give Old Tablets New Stories

A lightweight, free, offline-first EPUB reader, created to extend the useful life
of an aging Android tablet. Native Java and Android components keep it small and
personally modifiable.

> Anything that might encourage someone to read a single word more should be
> freely available and personally modifiable.

**Stable release: BOOKWORM v2.0.0, versionCode 11**, on `main` and annotated tag
`v2.0.0`. The approved functional source is
`b74086a1000e0de5a1e111ce7ef2cbfd11a2c3ce`; release preparation changes only
version/signing configuration and documentation. Stable v1.0 remains preserved at
`v1.0.0` and in its existing local archive. Gutenberg discovery is deferred and
not exposed in the v2 user interface.

[Download BOOKWORM v2.0.0](https://github.com/IronBeaver/BOOKWORM/releases/tag/v2.0.0)
and see the [release notes](docs/releases/BOOKWORM-v2.0.0.md).

Production typography integration passed Samsung API 22 hardware validation. **Reading
settings → Reading font** offers System Serif, System Sans, System Monospace,
EB Garamond, Literata, Atkinson Hyperlegible Next and Bitter. Select a font and
Apply; existing serif/sans preferences and all size, margin, spacing and theme
defaults are preserved. Only the selected bundled family's four faces are loaded
offline. Pagination/restoration wait for measured font readiness; a loading failure
leaves the saved position untouched and offers recovery through Reading settings.
See [Stage 2 evidence](tests/FONT-PROTOTYPE.md) and [typography notes](tests/READER-FONTS.md).

Stage 3B adds **Follow Appearance** (default), Warm Paper, Soft Blue, Blue Night and
Soft Dark reader-only palettes. Reading settings also includes **Continue reading
on launch**, default ON. A fresh launcher task opens the remembered book using its
existing saved text position; returning to a running task keeps the current screen.
If a remembered book is missing or an automatic open fails/is interrupted before
the first successful layout, the bookshelf remains available and repeated automatic
attempts are suppressed until a book is opened successfully by hand. Turn the option
OFF to start fresh tasks on the shelf. The latest build passed user-confirmed
Samsung Galaxy Tab A / Android 5.1.1 hardware testing, including typography,
pagination, reader palettes and Continue Reading on Launch. See
[Reading Comfort testing](tests/READING-COMFORT.md).

## How BOOKWORM was created

BOOKWORM was developed through a human-directed collaboration involving its project creator, ChatGPT, and Codex. The creator defined the objectives, tested the software on physical hardware, and retained control over design and release decisions.

Read the [BOOKWORM development case study](docs/BOOKWORM-CASE-STUDY.md) for an evidence-based examination of the process, including successes, failures, and lessons learned. A separate [evidence and provenance index](docs/CASE-STUDY-EVIDENCE.md) identifies supporting records and explains their limitations.

## Designed and Tested Hardware

BOOKWORM was specifically developed for the **Samsung Galaxy Tab A SM-T280**.
This is the project's reference hardware, not a claim of universal Android support.

| Specification | Reference hardware |
|---|---|
| Manufacturer | Samsung |
| Device | Galaxy Tab A 7.0 (2016) |
| Model | **SM-T280** |
| Android / API | **5.1.1 / 22** |
| CPU | **32-bit ARM / ARMv7** |
| RAM | **1.5 GB** |
| Display | **7-inch, 1280 × 800** |
| Connectivity | Wi-Fi |

Android API22 is the configured minimum. Other devices may work but have not been
validated. A minimum API declaration does not guarantee successful operation on
every device. Heavy frameworks and unnecessary dependencies are intentionally
avoided to accommodate older hardware.

## Screenshots

Actual screenshots have not been supplied for the public repository. Placeholders:

| View | Status |
|---|---|
| Cover-facing bookshelf and leather Bindery | Screenshot to add |
| Immersive reader | Screenshot to add |
| Development spine shelves and book preview | Screenshot to add |

No generated mockup is presented as a hardware screenshot.

## Features and versions

| Version | Status on the SM-T280 | Features |
|---|---|---|
| **v1.0 / code 8** | Hardware-tested stable | EPUB import/reading, persistent library and positions, reading preferences/navigation, leather Bindery covers, grid/classic bookshelf |
| **v1.1 development features** | Hardware-tested by the owner | Dimensional covers, physical shelves, leather spines, selection and book preview; touchscreen selection reported intuitive and reliable |
| **v1.2 / code 10** | Experimental networking | Gutendex discovery, title/author search, language filter, EPUB acquisition/import, duplicate detection and Open Book; catalog access currently times out on the reference tablet |

v1.0 requests no network permission. v1.2 adds INTERNET for the catalog; downloaded books still read offline.

Both page turning and continuous chapter scrolling are available offline. Native
centre-tap reader controls, Contents, reading appearance, themes, cover overrides
and sorting are retained. The software supports standard unencrypted reflowable
EPUB2/EPUB3; it does not provide PDF, DRM bypass, cloud sync or subscriptions.

`main` preserves the actual archived v1.0 application source. `develop` is one
honest snapshot of the preserved v1.2 work, including v1.1 features; no unavailable
intermediate historical commits or v1.1 tag have been invented.

## Installation and updates

The public v2.0.0 APK is available from the [GitHub release page](https://github.com/IronBeaver/BOOKWORM/releases/tag/v2.0.0). Obtain a reviewed APK from the project
owner or build one locally; do not download random repackaged copies.

1. Check Android/API requirements and retain your original EPUB files safely.
2. Copy the APK to the device and open it with the system package installer.
3. Android5.1.1 may require its **Unknown sources** sideloading setting. Allow
   only an APK you trust and restore the setting afterward; menu names vary.
4. Open BOOKWORM, import an EPUB through the system picker, and read offline.

A matching package identity and signing certificate lets an APK update the
existing app while retaining its library/preferences. A different debug key cannot
replace the original signed APK. **Do not routinely uninstall to update:** doing
so can delete books, covers, positions and settings. Keep originals outside the app.
No root, launcher replacement, Google Play or account is required.

## Building from source

JDK17, Android SDK Platform35 and Build Tools35.0.0 are required. The included
Gradle8.9 wrapper uses AGP8.7.3; Android Studio is optional. First create your own local debug key using docs/BUILDING.md, then run from the repository root:

    $env:JAVA_HOME = 'C:\path\to\jdk-17'
    $env:ANDROID_HOME = 'C:\Android'
    $env:GRADLE_USER_HOME = "$PWD\.gradle-user-home"
    .\gradlew.bat --no-daemon --console=plain :app:assembleDebug :app:lintDebug

APK: `app/build/outputs/apk/debug/app-debug.apk`.
[Full build, debug-signing and test instructions](docs/BUILDING.md) explain how to
build without the owner's private key. Your own debug certificate may not install
as an update over the original APK. Keys, local settings and generated APKs are
excluded from Git; the essential wrapper files are included.

## Tested Devices and compatibility reports

| Manufacturer/model | Android / CPU | Confirmed results | Known limits |
|---|---|---|---|
| Samsung Galaxy Tab A **SM-T280** | **5.1.1 / API22 / ARMv7** | v1.0 stable offline reader; v1.1 physical bookshelf and intuitive/reliable touch selection | v1.2 Gutendex catalog timeout; cause unknown |
| Other hardware | Not yet validated | Reports welcome | No compatibility claim yet |

Please report manufacturer/model number, Android version, CPU architecture if
known, installation result, EPUB import/reading, bookshelf performance, network
discovery, crashes and unusual behavior. Use the [simple report template](.github/ISSUE_TEMPLATE/bug_report.md);
logs are not required and screenshots are optional.

## Known limitations

**Gutendex catalog access times out on the Samsung SM-T280 running Android5.1.1.**
The cause is not established. Desktop HTTPS download/import succeeded, which does
not prove tablet network compatibility. No networking diagnosis or fix is part
of repository preparation. See [known issues and future investigation areas](docs/KNOWN_ISSUES.md).

Downloads do not resume after interruption. Final atomic import cannot be
cancelled. Public catalog/download servers may be unavailable. Copyright status
can differ outside the US. These development-network limitations do not replace
or diminish the tested stable offline-reading scope.

## Contributing

Bug reports, older-device tests, accessibility, rendering and performance
improvements are welcome. Start small: [Contributing](CONTRIBUTING.md) and the
[family testing checklist](docs/TESTING.md) explain the lightweight workflow.
`main` is stable; features/fixes start from `develop`, are tested before merging,
and stable releases receive annotated tags.

## Licensing and attribution

BOOKWORM's original application code is **MIT**, copyright 2026 **Norm R Langevin**.
See [LICENSE](LICENSE), [licensing audit](docs/LICENSING.md) and
[third-party notices](THIRD_PARTY_NOTICES.md). The Gradle wrapper and bundled book
are separately licensed; the MIT grant does not relicense them.

The complete bundled *Frankenstein* EPUB by Mary Shelley retains its Project
Gutenberg notices and license. [Original book page](https://www.gutenberg.org/ebooks/84).
Books are generally free of US copyright restrictions; users elsewhere must check
local law. [Gutendex](https://gutendex.com/) is an independent catalog API.
BOOKWORM is not affiliated with or endorsed by either organization.
