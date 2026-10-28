# BOOKWORM Case Study: Evidence and Provenance

This document supports [BOOKWORM: A Case Study in Human-Directed AI Software Development](BOOKWORM-CASE-STUDY.md). It distinguishes public, independently inspectable artifacts from references to a **private** development archive.

## Evidence classification

- **Verified:** supported by inspected source, tool output, or release artifacts.
- **Reported:** firsthand creator testimony or a participant's implementation report.
- **Inferred:** a supported but not independently demonstrated explanation.
- **Unknown:** not established by available evidence.

A successful build does not prove hardware usability. A human hardware report is firsthand evidence, but not an instrumented device log.

## Original Codex rollout index

`R:Lnnn` identifies a **physical line number in the preserved original Codex JSONL rollout snapshot**. These are archival locators, **not public hyperlinks**, verbatim quotations, or line numbers in this Markdown file. The private archive is not published.

| ID | Documented event or claim | Original record locator |
|---|---|---|
| E01 | API 22 requirements, build failures, successful build | R:L15, L106, L116, L131, L158 |
| E02 | Bookshelf requirements and implementation | R:L168, L283 |
| E03 | EPUB development, disabled picker and correction | R:L290, L655, L662, L730 |
| E04 | Imported-book visibility problem and correction | R:L737, L952 |
| E05 | v1.0 stable milestone | R:L1684, L1749 |
| E06 | Gutenberg experiment | R:L1923, L2158 |
| E07 | Spine investigation, implementation, visual rejection | R:L2685, L2749, L2756, L2819, L2826, L2860, L2913, L2952, L2973 |
| E08 | Touch-handling investigation, fix, feedback | R:L2980, L3020, L3027, L3078, L3085 |
| E09 | Typography and font-asset research | R:L3109, L3251, L3258, L3361 |
| E10 | Font prototype and reported Samsung test | R:L3368, L3536, L3543 |
| E11 | Typography integration, palettes, hardware acceptance, push | R:L3543, L3792, L3799, L3999, L4006, L4138, L4145, L4183 |
| E12 | Gutenberg feature hidden for v2 | R:L4190, L4246, L4253, L4291; Git `b74086a` |
| E13 | v2 authorization, validation and publication | R:L4298, L4348, L4390, L4399, L4432, L4452; Git `b626d30` |
| E14 | Published v2 package and release metadata | Git tag `v2.0.0`; public release artifacts |

These entries identify where the original evidence can be checked by an authorized archive holder. They are **not a claim that private records are available to the public**.

## Publicly inspectable artifacts

- [Source repository](https://github.com/IronBeaver/BOOKWORM)
- [BOOKWORM v2.0.0 release](https://github.com/IronBeaver/BOOKWORM/releases/tag/v2.0.0)
- Release source commit: `b626d305f02284c405e9bf41f4bc76011fbf1bbf`
- Release APK filename: `BOOKWORM-v2.0.0.apk`
- Release APK size: `2,012,347` bytes
- Release APK SHA-256: `1884aac15b407c77a01e18649dca8ab1ac2a380e204ce8c09152ff54f93c60c7`
- Minimum Android API: `22`
- Recorded release: signed, non-debuggable

The hash identifies the recorded release binary. Readers can independently compute SHA-256 on the downloaded APK and compare it.

## Creator testimony

**Assignment provenance:** The project creator confirms that every development assignment submitted to Codex was drafted by ChatGPT after creator–ChatGPT discussions. Selected original planning discussions support the early requirements, but a complete one-to-one map to all Codex events has not been published.

**Hardware validation:** The creator confirms that the **exact final signed v2.0.0 APK** was subsequently installed and tested on the Samsung Galaxy Tab A and Google Pixel 6. The release-preparation record had noted that this installation was still outstanding at that earlier point. The two statements describe sequential stages.

## Limitations and privacy

This is a retrospective, single-project analysis. It is not a controlled productivity experiment, independent security audit, or proof of broad Android compatibility.

The historical rollout includes truncated/compacted content; early Git history includes reconstructed snapshots. The evidence archive is private and may contain personal paths, conversations, and sensitive metadata. **Do not upload raw session logs, signing keys, or the private evidence collection to this repository.**

The application code is MIT-licensed. Bundled fonts and third-party materials retain their own licensing and attribution requirements.
