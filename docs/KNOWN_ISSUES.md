# Known issues

## Stable offline reader

v1.0 is hardware-tested on Samsung Galaxy Tab A SM-T280, Android 5.1.1/API22.
This does not establish compatibility with every Android device. Unencrypted,
reflowable EPUB2/EPUB3 are supported; PDF, DRM bypass, multimedia, cloud sync and
text-to-speech are not implemented. Font/Unicode behavior depends on Android fonts
and the device WebView. Preserve original book files when experimenting.

## Physical bookshelf development features

The owner has hardware-tested the v1.1 dimensional covers, physical shelves,
leather spines and selection/preview on the reference SM-T280. Touchscreen spine
selection was reported intuitive and reliable. Other hardware/accessibility
configurations and large libraries still need reports. Long spine titles can truncate.

## Experimental v1.2 catalog timeout — open issue

**Project Gutenberg / Gutendex catalog access times out on the Samsung SM-T280
running Android 5.1.1. The cause is not established.**

A desktop HTTPS download/import test succeeded. That does not establish tablet
network compatibility. Do not describe networking as working on the Samsung.
No diagnosis or repair was attempted during repository preparation.

Future investigation areas, not conclusions:

- Android 5.1 TLS and certificate trust.
- DNS resolution, connection/read timeouts and service availability.
- Network security configuration and local network differences.
- Desktop versus Android HTTP-client behavior.

Other v1.2 limits: downloads do not resume after interruption; final atomic import
cannot be cancelled; public catalog/download servers may be unavailable; covers
load individually on request; and manually imported editions without Gutenberg
identifiers cannot always be matched by ID. Copyright status may differ outside
the United States. These catalog limitations are separate from stable offline reading.
