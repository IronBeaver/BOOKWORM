# Building BOOKWORM without Android Studio

## Tools

- Eclipse Temurin or another JDK17.
- Android SDK Platform 35 and Build Tools 35.0.0.
- Included Gradle8.9 wrapper and Android Gradle Plugin8.7.3.
- PowerShell7 and a desktop JDK for the optional host test scripts.

The app is Java, minSdk22, target/compile35. No external runtime dependencies.
You need neither the owner's private signing key nor Android Studio. The wrapper
downloads its pinned distribution and build dependencies on the first online build.
The distribution SHA256 is pinned in gradle-wrapper.properties.

Create local.properties with your SDK path (for example `sdk.dir=C:/Android`),
or set ANDROID_HOME for the current process. Do not commit local.properties.

From the repository root in Windows PowerShell:

    $env:JAVA_HOME = 'C:\path\to\jdk-17'
    $env:ANDROID_HOME = 'C:\Android'
    $env:GRADLE_USER_HOME = "$PWD\.gradle-user-home"
    .\gradlew.bat --no-daemon --console=plain :app:assembleDebug :app:lintDebug

Before the first build, create your local debug key as shown below. Add `--offline` only when the pinned tools/dependencies are already cached.
On other systems use `sh ./gradlew`; the app builds without Windows-only code,
but the provided host validation scripts use PowerShell and Windows JDK executable names.

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.
Lint report: `app/build/reports/lint-results-debug.html`.

## Owner's v2.0.0 release build

The release variant uses the same existing workspace signing configuration and
certificate as the previously distributed BOOKWORM APKs, with debuggable disabled.
The historical certificate is named Android Debug; no replacement key is created.
Using a different key prevents an in-place upgrade of the installed app.

With the established private keystore already present, run:

    .\gradlew.bat --no-daemon --console=plain --offline :app:assembleRelease :app:lintRelease

Output: `app/build/outputs/apk/release/app-release.apk`. Verify with apksigner and
`--min-sdk-version 22`; compare the certificate to the original distributed APK.
VersionName is 2.0.0 and versionCode is 11, above stable v1.0's code 8 and the
validated development build's code 10. Never commit keystores, local.properties,
credentials or generated APKs. Contributors' own debug keys are for separate test
installs and cannot replace the owner's installed app.

## Your own debug signing key

The unchanged build configuration uses
`.gradle-user-home/bookworm-debug.keystore` under the project. This file is ignored.
The custom filename is not generated automatically: create your own **debug-only**
key first (the passwords below are Android's public debug defaults, not a secret):

    New-Item -ItemType Directory -Force .gradle-user-home | Out-Null
    & "$env:JAVA_HOME\bin\keytool.exe" -genkeypair -keystore .gradle-user-home/bookworm-debug.keystore -alias AndroidDebugKey -storepass android -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname 'CN=Android Debug,O=Android,C=US'

Only run that command when the file is absent. Never overwrite a signing key.
Locally generated certificates differ from the original distributed APK, so your
APK may not install as an update over that APK. Do not uninstall an existing app
to force the update: uninstalling can delete books and preferences. Test your own
key on a separate device/profile or explicitly planned test package instead.

To verify APK signing, using your SDK's apksigner:

    & "$env:ANDROID_HOME\build-tools\35.0.0\apksigner.bat" verify --verbose --print-certs --min-sdk-version 22 app/build/outputs/apk/debug/app-debug.apk

API22 needs v1/JAR signing. Verification and minSdk do not prove hardware operation.
Only APKs with matching package identity and signing certificate can update the
original app while preserving its data. Private release keys never belong in Git.

## Tests

    .\tests\run-model-tests.ps1

This runs host parsing, persistence, model and source-contract checks. On `develop`
it also exercises shelf/spine models and catalog transport with local HTTPS mocks.
The catalog tests reuse Gradle's installed Groovy JSON parser through a host-only
adapter: run a Gradle build first to populate the wrapper distribution. No test
adapter is included in the APK. Host tests are not Android UI/TLS execution.

The v7 reader regression baselines are small checked-in text/hash fixtures, not
APKs. Private archived-release checks run only when the owner supplies that local
archive. Real Alice EPUB samples and Edge browser checks are optional and may be
skipped when those local resources are absent; report skips rather than claiming
device or browser coverage. The bundled Frankenstein tests remain available offline.

Do not run ADB/install commands or change device security settings as part of
automated tests. See the approachable [manual checklist](TESTING.md).
