# BOOKWORM: A Case Study in Human-Directed AI Software Development

*An evidence-based examination of human–AI collaboration in creating an Android EPUB reader*  
*October 2026*

## Executive summary

BOOKWORM is a free, open-source EPUB reader developed to extend the useful life of a Samsung Galaxy Tab A 7.0 (SM-T280) running Android 5.1.1. Its project creator had extensive experience in instructional design and interactive learning, but was not a professional Android developer.

The creator collaborated with ChatGPT on requirements, planning, and development assignments. Those assignments were submitted to Codex, which investigated problems, wrote code, built the application, and prepared releases. The creator tested the results, evaluated design choices, and authorized consequential changes.

The creator confirms that **every development assignment submitted to Codex was drafted by ChatGPT following their discussions**. The preserved Codex session independently documents the assignments received and implementation work, but a complete one-to-one mapping to original ChatGPT discussions has not been established.

During October 26–28, 2026, the project progressed from a minimal API 22 Android application to a public v2.0.0 release. The creator subsequently confirmed installation and testing of the **exact signed release APK** on both the original Samsung tablet and a Google Pixel 6.

The evidence supports five conclusions:

1. AI assistance expanded the creator's practical implementation capacity.
2. Human oversight materially changed design, scope, testing, and release decisions.
3. Separating AI-assisted planning from AI-assisted implementation was workable in this project.
4. Technical failures, rejected designs, and procedural overengineering still required correction.
5. Build records, source history, AI reports, and firsthand device observations establish different kinds of facts.

This is a retrospective case study of one project, not a controlled productivity experiment or proof that AI can replace professional software developers.

## 1. Project context and objectives

The original problem was practical: a functioning Android tablet had become less useful as its operating system aged. The creator wanted to keep the hardware in service without replacing its firmware or depending on a cloud-based reading platform.

The initial requirements were compatibility with Android 5.1.1 (API 22), local EPUB import, offline reading, persistent library and reading position, and a lightweight implementation. Planning discussions also favored EPUB 2/3 support, pagination and scrolling, and avoiding decorative page-curl effects. DRM circumvention was outside scope.

The product evolved to include customizable bookshelves, personalized covers through the Bindery, multiple fonts, reading palettes, and native text selection. These features were not required merely to decode EPUB files; they reflected the creator's standards for a reader worth using.

**Evidence:** E01–E04; creator's planning account.

## 2. Method and evidence standards

The study draws on the preserved Codex rollout, historical source snapshots and Git references, milestone and validation records, original ChatGPT planning context, and the creator's firsthand testimony. A reconstruction identified **24 significant development events** across October 26–28. The private evidence inventory contains **139 verified files** and a snapshot of **4,834 complete Codex JSONL records**.

The record is not perfectly complete. Some tool outputs are truncated or compacted, and portions of early Git history were reconstructed from snapshots. The original ChatGPT discussions have not been individually matched to all 24 events.

We distinguish:

- **Verified:** directly supported by an inspected artifact or recorded tool result.
- **Reported:** supported by a participant's firsthand account or an AI implementation report.
- **Inferred:** a plausible interpretation not independently established.
- **Unknown:** not established by the available material.

For example, a successful build verifies compilation, not practical usability. A creator's device report establishes a firsthand observation, not an independently captured device trace.

## 3. Collaboration model and decision authority

The project creator defined objectives, supplied design judgment, performed physical-device testing, and decided what to accept or publish. ChatGPT helped structure requirements, explore options, and draft assignments. Codex investigated, implemented, built, and validated software within the authorization provided.

The recurring workflow was:

**Creator–ChatGPT discussion → ChatGPT-authored assignment → creator submission → Codex investigation or implementation → validation → creator decision.**

The preserved Codex rollout shows specific instances of investigation being separated from implementation approval. Spine-layout and touch-handling changes on October 28 followed this pattern. Repository updates and v2 publication also involved explicit authorization.

**Finding:** Human authority was not confined to the initial idea. It was exercised at points where changes could affect the application and its release. **Evidence:** E07–E08, E11–E13.

## 4. Development and technical outcomes

### 4.1 Incremental implementation

On October 26, Codex began with a minimal Java application targeting API 22. Signing, Gradle-daemon, and lint failures preceded a successful build. The project then added a bookshelf, EPUB import and reading, library management, personalized covers, and progressively less intrusive controls. A stable v1.0 milestone was preserved before further experimentation.

The source history includes reconstructed early snapshots. Those snapshots establish useful historical code states, but they should not be mistaken for a continuous original commit-by-commit record.

**Evidence:** E01–E05.

### 4.2 Hardware findings

Two early issues were exposed by use rather than compilation. EPUBs appeared greyed out in the Android picker; Codex revised file selection while retaining EPUB structural checks. Later, books appeared in the library count but not in the accessible bookshelf; Codex investigated a likely layout-measurement issue and changed the implementation. The precise runtime cause of the latter was not independently established by a device trace.

**Finding:** A successful build did not establish that the application worked correctly on the target tablet. **Evidence:** E03–E04.

### 4.3 Compatibility prototype

Before integrating a larger typography system, the collaborators investigated font assets and licensing and commissioned an isolated compatibility prototype. It included **16 bundled font faces**. The creator reported successful Samsung testing of font loading and native text selection. The production reader subsequently incorporated four bundled font families plus three system-font choices, along with palettes and reading-continuation behavior.

**Finding:** Testing uncertain features separately was a practical way to reduce integration risk on old hardware. The record does not quantify defects avoided. **Evidence:** E09–E11.

### 4.4 Public release

BOOKWORM v2.0.0 was published after explicit authorization. The release was signed, non-debuggable, and compatible with Android API 22 and later. The APK was **2,012,347 bytes**.

During release preparation, the exact signed APK had **not yet** been installed on physical devices. The creator later confirmed installing and testing that same final release on both the Samsung Galaxy Tab A and Pixel 6. These statements concern different stages and are not contradictory.

The [public source repository](https://github.com/IronBeaver/BOOKWORM) and [v2.0.0 release](https://github.com/IronBeaver/BOOKWORM/releases/tag/v2.0.0) provide inspectable artifacts. **Evidence:** E12–E14; creator confirmation.

## 5. Human oversight and quality control

### 5.1 Design acceptance

Codex investigated and implemented changes to author-name placement on the bookshelf spines. The creator subsequently rejected the visual result and accepted a simpler title-only design.

**Finding:** A technically implemented change was not necessarily an acceptable product decision. Design judgment remained with the creator. **Evidence:** E07.

### 5.2 Feature readiness

The collaboration experimented with public-domain discovery using Gutendex and Project Gutenberg. Desktop-side results were encouraging, but the original Samsung experienced unresolved access problems. Rather than present the feature as reliable, the creator authorized hiding its entry point for v2 while retaining the underlying implementation for possible future work.

The cause of the tablet's failure was not established in the reviewed record.

**Finding:** Implemented, partially validated, and release-ready are different states. **Evidence:** E06, E12.

### 5.3 Consequential approvals

The rollout documents separate approvals for selected source changes, repository operations, and stable v2 publication.

**Finding:** The reviewed workflow included meaningful authorization checkpoints. This does not imply every possible operation was independently audited. **Evidence:** E11–E13.

## 6. Evaluating ChatGPT and Codex

The systems had different jobs, so a numerical head-to-head score would be misleading. They can nevertheless be evaluated against the same qualitative criteria.

| Criterion | ChatGPT | Codex |
|---|---|---|
| Instruction adherence | Drafted assignments from creator discussions, according to creator testimony | Followed submitted assignments with recorded approval checkpoints |
| Technical contribution | Planning, requirements, alternatives, coordination | Investigation, code, builds, validation, release preparation |
| Response to correction | Revised recommendations when challenged | Revised code and interface after feedback |
| Scope discipline | Supported staged work, but later overextended archival procedure | Preserved stable/development distinctions and implemented feature deferral |
| Transparency | Explained plans and limitations; full planning-message mapping remains incomplete | Produced inspectable tool activity and implementation reports |
| Contribution to outcome | Made the work more structured and accessible | Produced the working application |

### Correctability and proportionality

Both systems were correctable in practice. Codex revised software following testing and rejected design results. ChatGPT changed course after the creator challenged unnecessary evidence-preservation complexity.

Following release, the collaborators assembled a restricted local evidence collection with provenance and integrity checks. The process then expanded into encryption troubleshooting that was not necessary for the research objective. The creator questioned the added work, and ChatGPT acknowledged its optional nature.

**Finding:** AI can remain diligent and technically plausible while recommending disproportionate process. Oversight must include the authority to stop work that no longer serves the goal.

The case supports the effectiveness of the *combined workflow*, not a controlled estimate of either system's independent productivity contribution.

## 7. Findings and implications

**Expanded capacity, not transferred authority.** The creator directed a working Android application without personally writing its implementation code. This demonstrates a practical capability in this instance, not a universal replacement for specialized developers.

**Human judgment changed outcomes.** Hardware observations, spine-design rejection, Gutenberg deferral, and release approval had direct consequences for what shipped.

**Separation of planning and execution was workable.** ChatGPT's task preparation and Codex's implementation created reviewable stages. The study does not prove that two AI systems are always preferable to one.

**Verification required multiple evidence types.** Source changes, build logs, package hashes, and firsthand hardware testing answered different questions.

**Knowing when to stop mattered.** Deferring an unfinished feature and abandoning unnecessary encryption troubleshooting both protected the original objective.

**Engagement was relevant but not quantified.** The project was driven by curiosity and a desire to reuse hardware. The record supports sustained voluntary participation, but not a measured productivity effect from enjoyment.

## 8. Conclusion

BOOKWORM began with an aging but functioning tablet and a desire to keep it useful. Over approximately three days of concentrated implementation, a collaboration involving its project creator, ChatGPT, and Codex produced a publicly released EPUB reader.

The record documents successes, build failures, hardware defects, rejected designs, compatibility experiments, deliberate scope control, and unnecessary procedural work.

ChatGPT helped plan and specify. Codex investigated and implemented. The creator established objectives, tested results, and decided what was acceptable.

The evidence does not establish autonomous AI software development or the replacement of professional software engineering. It demonstrates something narrower and more practical: **human-directed AI collaboration can enable a project creator to complete technical work without personally performing every specialized implementation task.**

**The objective wasn't to remove the human from software development. It was to give the human more control over what could be created.**

---

## Evidence and reproducibility

See [Case Study Evidence](CASE-STUDY-EVIDENCE.md) for a claim-to-record index, evidence classification, limitations, and public artifact checks. The private evidence archive and raw conversations are **not** included in the public repository.

**Public artifacts:** [Repository](https://github.com/IronBeaver/BOOKWORM) · [BOOKWORM v2.0.0](https://github.com/IronBeaver/BOOKWORM/releases/tag/v2.0.0)

*This is a retrospective case study of one project, not a security audit, independent performance benchmark, or controlled productivity comparison.*
