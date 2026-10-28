param([switch]$CheckApk)
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$source=Join-Path $root 'app/src/main/java/com/bookworm/reader'
$out=Join-Path $root 'app/build/reader-font-tests'
New-Item -ItemType Directory -Force $out | Out-Null
& "$env:JAVA_HOME/bin/javac.exe" -encoding UTF-8 -d $out "$PSScriptRoot/stubs/android/content/Context.java" "$PSScriptRoot/typography-stubs/android/content/SharedPreferences.java" "$source/ReaderFonts.java" "$source/ReaderPalettes.java" "$source/ReaderPreferences.java" "$source/ThemeSettings.java" "$PSScriptRoot/ReaderFontTests.java"
if($LASTEXITCODE -ne 0){throw 'Font test compilation failed'}
& "$env:JAVA_HOME/bin/java.exe" -cp $out com.bookworm.reader.ReaderFontTests
if($LASTEXITCODE -ne 0){throw 'Font preference/route tests failed'}
& "$PSScriptRoot/check-font-prototype.ps1" -CheckApk:$CheckApk

# Compare untouched interaction/storage methods against the approved develop HEAD,
# without rewriting historical milestone baselines that intentionally freeze old CSS.
$env:GIT_OPTIONAL_LOCKS='0'
$git='C:\Program Files\Git\cmd\git.exe'
function Method([string]$text,[string]$name){
    $matches=[regex]::Matches($text,'(?m)^    (?:@Override )?(?:private|public|protected) (?:static )?[\w<>\[\].?,]+ '+[regex]::Escape($name)+'\(')
    if(!$matches.Count){throw "Missing method $name"}
    $m=$matches[$matches.Count-1];$start=$text.IndexOf('{',$m.Index);$depth=0
    for($i=$start;$i -lt $text.Length;$i++) {if($text[$i] -eq '{'){$depth++};if($text[$i] -eq '}'){$depth--;if($depth -eq 0){return $text.Substring($m.Index,$i-$m.Index+1).Replace("`r",'')}}}
    throw "Unterminated method $name"
}
$current=Get-Content -Raw "$source/ReaderActivity.java"
$baseline=(& $git -C $root show HEAD:app/src/main/java/com/bookworm/reader/ReaderActivity.java) -join "`n"
foreach($name in @('installInput','confirmedTap','hit','update','persist','capture','turn','chapterAtBoundary','chapter','follow','contents','onBackPressed')) {
    if((Method $current $name) -cne (Method $baseline $name)){throw "Protected reader method changed: $name"}
}
$main=Get-Content -Raw "$source/MainActivity.java"
$oldMain=(& $git -C $root show HEAD:app/src/main/java/com/bookworm/reader/MainActivity.java) -join "`n"
foreach($name in @('buildShelf','restoreSelection','selectBook','dismissSelection','chooseSort','openBook','bookOptions','bookDetails')) {
    if((Method $main $name) -cne (Method $oldMain $name)){throw "Protected bookshelf method changed: $name"}
}
$unchanged=@('SpineView.java','BookshelfAdapter.java','EpubParser.java','ReadingPosition.java','ReaderWebView.java','ReaderChrome.java','ThemeSettings.java','AppearanceActivity.java')
foreach($name in $unchanged){
    & $git -C $root diff --quiet HEAD -- "app/src/main/java/com/bookworm/reader/$name"
    if($LASTEXITCODE -ne 0){throw "Out-of-scope change: $name"}
}
& $git -C $root diff --quiet HEAD -- app/src/main/assets/reader.js app/src/main/assets/reader-input.js app/build.gradle
if($LASTEXITCODE -ne 0){throw 'Pagination/input scripts or build/dependency settings changed'}
if($CheckApk){
    $apk=[IO.Compression.ZipFile]::OpenRead((Join-Path $root 'app/build/outputs/apk/debug/app-debug.apk'))
    try{if(@($apk.Entries|Where-Object FullName -like 'assets/font-prototype/*').Count){throw 'Prototype UI still packaged'}}finally{$apk.Dispose()}
    $manifest=Get-Content -Raw (Join-Path $root 'app/build/intermediates/merged_manifests/debug/processDebugManifest/AndroidManifest.xml')
    if($manifest -match 'FontPrototypeActivity'){throw 'Prototype launcher still packaged'}
}
Write-Output 'PASS: protected reader methods, bookshelf, parser, themes, gestures and position storage unchanged'
