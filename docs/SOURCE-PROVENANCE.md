# Source provenance and preservation

Repository preparation was performed on 2026-10-26 without changing application
behavior, UI, reader engine, library schema or dependencies.

- `main` is reconstructed from the preserved v1.0 source archive, versionName 1.0,
  versionCode8. All 56 application/resource/asset/build-config files match its
  original SHA256 inventory. Source archive SHA256:
  E218BB496938222F7E67AAC595D612D5FEE7C6BF8B58065BCE0BD31F91955FBF.
- `develop` preserves the current v1.2 source, versionName1.2, versionCode10.
  All 67 application/resource/asset/build-config files match the separately
  verified pre-Git v1.2 backup. No exact complete v1.1 snapshot was available;
  no intermediate v1.1 history or tag was invented.
- The eight original v1.0 release files remain private and unchanged. A separate
  backup holds the original v1.2 source and exact working APK. Neither archive,
  APKs nor signing keys enter Git.
- Public documentation, licenses, ignore rules and test infrastructure were
  prepared separately from the preserved application source. The reader regression
  harness now uses checked-in Java text/hash fixtures rather than a private APK.
  Private release-file checks are optional when archives are absent. These are
  test/repository changes, not runtime application changes.
- Line-ending normalization is deliberately disabled for this preparation, to
  retain archived source bytes. The Gradle launcher has executable Git mode.

The new verification APKs use a newly generated local debug certificate, not the
original private signing key. They are not byte-identical to the hardware-tested
APK and must not be presented as original signed updates. No new APK was installed
or published. Original package identity/signing materials remain untouched outside Git.
