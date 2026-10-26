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
