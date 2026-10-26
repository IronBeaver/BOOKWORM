# Repository validation — stable source

2026-10-26: the reconstructed v1.0 application/configuration matched all 56 original
source-inventory entries. All 814 available host checks passed. Optional downloaded
Alice fixtures were absent; no new native UI or hardware test was performed.

Offline assembleDebug and lintDebug passed after generating a fresh local debug
key at the unchanged custom filename. The first fresh build correctly reported a
missing key; explicit developer key creation is now documented. No original key
was needed or replaced. Lint: 0 errors, 21 existing warnings. API22 v1 and v2
signatures verify with the validation certificate. This rebuild is not the original
hardware-tested APK and is not claimed byte-identical to it.

Repository hygiene is checked before each commit, then across all branches/tags
and all Git blob objects. No keys, APKs, private archives, caches or local.properties
are intended for tracking. The Gradle wrapper and unchanged licensed Frankenstein
EPUB are the only reviewed binary exceptions. Security pattern checks are not a
universal detector; owner review is still required before public distribution.

The owner reports v1.1 physical shelves/spine selection working on the SM-T280.
v1.2 Gutendex catalog access times out on that tablet; cause unknown. This
preparation does not diagnose or repair that issue.

## Development checkout validation

The actual detached `develop` source worktree passed all 1,250 portable host
checks: eight private-archive checks were explicitly skipped because archives
are not distributed. Optional Alice samples and native/browser/hardware tests
were not supplied/executed in that clean checkout. Catalog transport tests used
local mocks only; no Gutenberg troubleshooting or new live catalog test occurred.

Offline assembleDebug/lintDebug succeeded with an independent generated debug
key. Lint: 0 errors, 44 warnings. API22 v1 and v2 signatures verify. Manifest
confirms versionName1.2/versionCode10; stable confirms 1.0/code8. The original
workspace APK and original signing key were not used/replaced by these rebuilds.
Existing SDK metadata/platform-tools and unsigned Gradle META-INF metadata
warnings remain. They did not prevent successful build/signature verification.

The clean test environment initially lacked its local Gradle/Groovy test-tool
cache. Reusing the already installed wrapper cache fixed the environment; no
runtime dependency, application source or global tool changed. Public instructions
explain running Gradle before host tests.

Staged-path/content audits ran before each commit. The final audit covers all
branches/tags and every Git blob, including unreferenced preparation blobs, with
private-key byte hashes supplied privately. The original release archive and
pre-Git source/APK backup are separately reverified. No remote is configured and
nothing has been pushed or published. A workspace-local, checksum-verified MinGit
was needed because Git was not installed; it is ignored and not distributed.

Before public publication: owner review of history/documentation, jurisdictional
book redistribution/licensing review, selection of trusted signed release APKs,
and clear experimental catalog labeling are still needed. Screenshots are explicit
placeholders. The known Samsung catalog timeout remains an open development issue.
