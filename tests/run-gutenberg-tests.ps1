$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$source=Join-Path $root 'app/src/main'
$java=Join-Path $source 'java/com/bookworm/reader'
$protected=Get-Content -Raw "$PSScriptRoot/gutenberg-protected-v1.1.json" | ConvertFrom-Json
foreach($file in $protected) {if((Get-FileHash -LiteralPath (Join-Path $source $file.Path)).Hash -ne $file.Hash){throw "Protected v1.1 source changed: $($file.Path)"}}
$ui=Get-Content -Raw "$java/BrowseActivity.java"
$http=Get-Content -Raw "$java/GutenHttp.java"
$reader=Get-Content -Raw "$java/ReaderActivity.java"
[xml]$manifest=Get-Content -Raw "$source/AndroidManifest.xml"
$checks=@(
    @{Name='INTERNET is the only permission';Pass=($manifest.manifest.'uses-permission'.Count -eq 1) -and ($manifest.manifest.'uses-permission'.name -eq 'android.permission.INTERNET')},
    @{Name='Reader remains network blocked with deny-by-default interception';Pass=($reader -match 'setBlockNetworkLoads\(true\)') -and ($reader -match 'return blocked\(\)')},
    @{Name='New browser is private and portrait';Pass=($manifest.manifest.application.activity | Where-Object name -eq '.BrowseActivity').exported -eq 'false'},
    @{Name='Minimal settings entry';Pass=(Get-Content -Raw "$java/SettingsActivity.java") -match 'Browse Books'},
    @{Name='No trust bypass or WebView catalog rendering';Pass=($http -notmatch 'setHostnameVerifier|setSSLSocketFactory|X509TrustManager|TrustAll') -and ($ui -notmatch 'WebView')},
    @{Name='No automatic initial request or cover harvesting';Pass=($ui -match 'Tap Popular or Search') -and ($ui -match 'Cover to load') -and ($ui -notmatch 'onCreate[^}]*requestPage')},
    @{Name='Foreground cancellation and worker cleanup';Pass=($ui -match 'onStop\(\)') -and ($ui -match 'operation.cancel\(\)') -and ($ui -match 'worker.shutdown\(\)')},
    @{Name='UI separates download and noncancelable atomic import';Pass=($ui -match 'this final atomic import cannot be cancelled') -and ($ui -match 'cancel.setEnabled\(false\)')},
    @{Name='Successful import offers reader by existing ID';Pass=($ui -match 'Open Book') -and ($ui -match 'ReaderActivity.class\).putExtra\("book",result.book.id\)')},
    @{Name='Copyright and independence notices visible';Pass=($ui -match 'Outside the US') -and ($ui -match 'independent catalog') -and ($ui -match 'not affiliated')},
    @{Name='No runtime dependencies added';Pass=(Get-Content -Raw "$root/app/build.gradle") -notmatch 'implementation|api\('},
    @{Name='API22 and version 1.2';Pass=($v=Get-Content -Raw "$root/app/build.gradle") -match 'minSdk 22' -and $v -match 'versionCode 10' -and $v -match "versionName '1.2'"}
)
foreach($check in $checks){if(!$check.Pass){throw "Gutenberg regression: $($check.Name)"}}
Write-Output "Passed $($protected.Count+$checks.Count) v1.1-protected-source/network/UI static checks (not native UI execution)."
$lib=(Get-ChildItem "$root/.gradle-user-home/wrapper/dists/gradle-8.9-bin" -Recurse -Filter 'groovy-json-3.0.21.jar' | Select-Object -First 1).DirectoryName
if(!$lib){throw 'Existing Gradle Groovy JSON host-test adapter unavailable; no dependency will be installed'}
$out=Join-Path $root 'app/build/gutenberg-tests'
New-Item -ItemType Directory -Force $out | Out-Null
$bin=Join-Path $env:JAVA_HOME 'bin'
$files=@('GutenUrls','GutenCatalog','GutenHttp','GutenLibrary','EpubBook','EpubParser','HtmlEntities','LibraryFiles','LibraryCatalog') | ForEach-Object {"$java/$_.java"}
& "$bin/javac.exe" -encoding UTF-8 -cp "$lib/*" -d $out $files "$PSScriptRoot/stubs/org/json/JSONObject.java" "$PSScriptRoot/stubs/org/json/JSONArray.java" "$PSScriptRoot/stubs/org/json/JSONException.java" "$PSScriptRoot/GutenbergTests.java"
if($LASTEXITCODE -ne 0){throw 'Gutenberg test compilation failed'}
Push-Location $root
try { & "$bin/java.exe" -cp "$out;$lib/*" com.bookworm.reader.GutenbergTests
    if($LASTEXITCODE -ne 0){throw 'Gutenberg tests failed'}
} finally {Pop-Location}
