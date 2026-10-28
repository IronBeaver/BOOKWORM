param([switch]$CheckApk)
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$source=Join-Path $root 'app/src/main/java/com/bookworm/reader'
$out=Join-Path $root 'app/build/reader-comfort-tests'
New-Item -ItemType Directory -Force $out | Out-Null
$files=@('ReaderFonts','ReaderPalettes','ReaderPreferences','ThemeSettings','ShelfPresentation','LaunchAttempt','ContinueReadingStore','LibraryCatalog','LibraryFiles','LibrarySort','ReadingPosition','EpubBook','EpubParser','HtmlEntities')|ForEach-Object {"$source/$_.java"}
& "$env:JAVA_HOME/bin/javac.exe" -encoding UTF-8 -d $out "$PSScriptRoot/stubs/android/content/Context.java" "$PSScriptRoot/typography-stubs/android/content/SharedPreferences.java" @files "$PSScriptRoot/ReaderComfortTests.java"
if($LASTEXITCODE -ne 0){throw 'Comfort test compilation failed'}
& "$env:JAVA_HOME/bin/java.exe" -cp $out com.bookworm.reader.ReaderComfortTests (Join-Path $out ('library-'+[Guid]::NewGuid().ToString('N')))
if($LASTEXITCODE -ne 0){throw 'Comfort/continuation tests failed'}
foreach($entry in (Get-Content -Raw "$PSScriptRoot/fixtures/stage3a/protected.json" | ConvertFrom-Json)) {
    if((Get-FileHash -LiteralPath (Join-Path $root $entry.Path)).Hash.ToLowerInvariant() -cne $entry.SHA256){throw "Approved typography/engine changed: $($entry.Path)"}
}
[xml]$themes=Get-Content "$root/app/src/main/res/values/reader-palettes.xml"
$expected=@{ReaderWarmPaper='#795322';ReaderSoftBlue='#245478';ReaderBlueNight='#FFE0A0';ReaderSoftDark='#B9D8FF'}
foreach($style in $themes.resources.style){if($style.parent -ne 'AppTheme' -or $style.item.name -ne 'android:colorAccent' -or $style.item.InnerText -cne $expected[$style.name]){throw 'Native selection-handle accent mismatch'}}
$main=Get-Content -Raw "$source/MainActivity.java"
$reader=Get-Content -Raw "$source/ReaderActivity.java"
if($main -notmatch 'state == null && isTaskRoot\(\)' -or $main -notmatch 'Intent.ACTION_MAIN' -or $main -notmatch 'Intent.CATEGORY_LAUNCHER' -or $main -notmatch 'launchAttempt.pause\(\)' -or $main -notmatch 'launchAttempt.allowed\(launchTicket\)'){throw 'Fresh-task/background lifecycle guards missing'}
if($reader -notmatch 'launchRecorded \|\|' -or $reader -notmatch 'if \(ready\) ContinueReadingStore.opened' -or $reader -notmatch 'if \(!ready && automaticFallback\(\)\) return'){throw 'Ready-only identity/fallback flow missing'}
& "$PSScriptRoot/check-reader-fonts.ps1" -CheckApk:$CheckApk
Write-Output 'PASS: hardware-approved Stage 3A font gate, font map, input, pagination, parser and positions preserved'
