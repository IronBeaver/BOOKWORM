# Contributing to BOOKWORM

Small, focused contributions are welcome. Bug reports, testing on older tablets,
accessibility, EPUB rendering and performance improvements all help. You do not
need to be a programmer: try the [family testing checklist](docs/TESTING.md) or
describe a problem using the [report template](.github/ISSUE_TEMPLATE/bug_report.md).
Do not include personal books, private keys, passwords or sensitive screenshots.

## A small-project workflow

- `main` contains the stable v1.0 release. Annotated tag `v1.0.0` identifies it.
- `develop` contains integrated v1.2 work, including hardware-tested v1.1 shelf
  features and experimental catalog integration. No v1.1 historical commit/tag
  has been invented; its exact complete source snapshot was not preserved.
- Start a feature or fix branch from `develop`, for example:

      git switch develop
      git switch -c fix/short-description

- Explain what changed and why. Build, lint and run relevant tests before asking
  for review. Keep parser, position and signing changes especially narrow.
- Test integrated work before merging. Move reviewed stable releases to `main`
  and give them an annotated version tag; never move an existing release tag.

Nothing has been published yet. Repository owners will choose the public review
and contribution channel before publication. There is no required account or
contributor toolchain beyond ordinary Git and the Android build tools.

## Build and test

See [BUILDING](docs/BUILDING.md). Host tests model behavior and check source
contracts; they are not proof of Android UI, TLS or performance compatibility.
Mention the device and test scope when reporting results. Other devices remain
unvalidated until someone reports them.

For v1.2 catalog work, first read [known issues](docs/KNOWN_ISSUES.md). The SM-T280
catalog timeout is unconfirmed in cause. Do not disable TLS verification or
increase request volume as a workaround. Investigate in a focused future change.

## License and repository hygiene

Contribute original code under the project's MIT license, or identify another
applicable license before adding material. Preserve all bundled-content notices.
Do not add runtime dependencies without explaining their size, purpose and API22
impact. Do not commit APKs, keystores, Gradle caches, local.properties, personal
EPUBs or device logs. The Gradle wrapper and the explicitly licensed bundled
Frankenstein EPUB are intentional exceptions to the binary/download exclusions.
