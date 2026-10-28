param([switch]$CheckApk)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$root = Join-Path $projectRoot 'tests\fixtures\font-prototype\assets\font-prototype'
$assets = Join-Path $projectRoot 'app\src\main\assets'
$m = Get-Content -Raw (Join-Path $root 'manifest.json') | ConvertFrom-Json
if ($m.fonts.Count -ne 16 -or $m.licenses.Count -ne 4) { throw 'Incomplete approved manifest' }
$total = 0
foreach ($f in $m.fonts) {
    $path = Join-Path $assets ('fonts\' + $f.file)
    if ((Get-Item -LiteralPath $path).Length -ne $f.bytes -or (Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash.ToLowerInvariant() -cne $f.sha256) {
        throw "STOP: approved hash/size mismatch: $($f.file)"
    }
    $total += $f.bytes
}
if ($total -ne 3013628) { throw 'Unexpected font payload' }
foreach ($l in $m.licenses) {
    $text = Get-Content -Raw (Join-Path $assets ('font-licenses\' + $l.file))
    foreach ($part in @('Copyright','SIL OPEN FONT LICENSE Version 1.1','TERMINATION','DISCLAIMER')) {
        if (!$text.Contains($part)) { throw "Incomplete OFL notice: $($l.file)" }
    }
}
$testOutput = Join-Path $projectRoot 'app\build\font-prototype-tests'
New-Item -ItemType Directory -Force $testOutput | Out-Null
$javaBin = Join-Path $env:JAVA_HOME 'bin'
& "$javaBin\javac.exe" -encoding UTF-8 -d $testOutput (Join-Path $projectRoot 'tests\fixtures\font-prototype\java\com\bookworm\reader\FontPrototypeRoutes.java') (Join-Path $PSScriptRoot 'FontPrototypeRoutesTests.java')
if ($LASTEXITCODE -ne 0) { throw 'Prototype route test compilation failed' }
& "$javaBin\java.exe" -cp $testOutput com.bookworm.reader.FontPrototypeRoutesTests
if ($LASTEXITCODE -ne 0) { throw 'Prototype route tests failed' }
if ($CheckApk) {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $apk = [IO.Compression.ZipFile]::OpenRead((Join-Path $projectRoot 'app\build\outputs\apk\debug\app-debug.apk'))
    try {
        foreach ($f in $m.fonts) {
            $entry = $apk.GetEntry('assets/fonts/' + $f.file)
            if (!$entry -or $entry.Length -ne $f.bytes) { throw "Missing/incorrect packaged font: $($f.file)" }
            $stream = $entry.Open()
            try { $hash = [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData($stream)).ToLowerInvariant() } finally { $stream.Dispose() }
            if ($hash -cne $f.sha256) { throw "Packaged font hash mismatch: $($f.file)" }
        }
        foreach ($l in $m.licenses) {
            $entry = $apk.GetEntry('assets/font-licenses/' + $l.file)
            if (!$entry) { throw "Missing packaged license: $($l.file)" }
            $stream = $entry.Open()
            try { $hash = [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData($stream)) } finally { $stream.Dispose() }
            if ($hash -cne (Get-FileHash -Algorithm SHA256 (Join-Path $assets ('font-licenses\'+$l.file))).Hash) { throw "Packaged license changed: $($l.file)" }
        }
    } finally { $apk.Dispose() }
    Write-Output 'PASS: all 16 packaged font hashes and four exact packaged notices'
}
Write-Output "PASS: 16/16 approved source font hashes and sizes; $total bytes; four complete OFL notices"
