$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
& (Join-Path $PSScriptRoot 'check-picker-compatibility.ps1')
& (Join-Path $PSScriptRoot 'check-bookshelf-regression.ps1')
& (Join-Path $PSScriptRoot 'check-bindery-contracts.ps1')
& (Join-Path $PSScriptRoot 'check-ui-polish.ps1')
& (Join-Path $PSScriptRoot 'check-immersive-reader.ps1')
$testOutput = Join-Path $projectRoot 'app\build\model-tests'
New-Item -ItemType Directory -Force $testOutput | Out-Null
$javaBin = Join-Path $env:JAVA_HOME 'bin'
$sourceRoot = Join-Path $projectRoot 'app\src\main\java\com\bookworm\reader'
& "$javaBin\javac.exe" -encoding UTF-8 -d $testOutput "$sourceRoot\ThemeSettings.java" "$sourceRoot\DemoBooks.java" "$sourceRoot\HtmlEntities.java" "$sourceRoot\EpubBook.java" "$sourceRoot\EpubParser.java" "$sourceRoot\EpubHtml.java" "$sourceRoot\ReadingPosition.java" "$sourceRoot\LibraryFiles.java" "$sourceRoot\LibraryCatalog.java" "$sourceRoot\LibrarySort.java" "$sourceRoot\LibraryRows.java" "$sourceRoot\LibrarySeeder.java" "$PSScriptRoot\ModelTests.java" "$PSScriptRoot\EpubTests.java" "$PSScriptRoot\RealEpubTests.java" "$PSScriptRoot\PrepareBundledBook.java" "$PSScriptRoot\LibraryTests.java"
if ($LASTEXITCODE -ne 0) { throw 'Model test compilation failed' }
& "$javaBin\javac.exe" -encoding UTF-8 -cp $testOutput -d $testOutput "$sourceRoot\CoverChoice.java" "$sourceRoot\CoverTypography.java" "$sourceRoot\LeatherArt.java" "$PSScriptRoot\BinderyPreview.java" "$PSScriptRoot\BinderyTests.java"
if ($LASTEXITCODE -ne 0) { throw 'Bindery test compilation failed' }
& "$javaBin\javac.exe" -encoding UTF-8 -cp $testOutput -d $testOutput "$sourceRoot\ShelfPresentation.java" "$PSScriptRoot\ShelfPresentationTests.java" "$PSScriptRoot\ShelfUiPreview.java"
if ($LASTEXITCODE -ne 0) { throw 'Shelf presentation compilation failed' }
& "$javaBin\javac.exe" -encoding UTF-8 -cp $testOutput -d $testOutput "$sourceRoot\ReaderTap.java" "$sourceRoot\ReaderChromeState.java" "$PSScriptRoot\ReaderInputTests.java"
if ($LASTEXITCODE -ne 0) { throw 'Reader input tests compilation failed' }
& "$javaBin\java.exe" -cp $testOutput com.bookworm.reader.ModelTests
if ($LASTEXITCODE -ne 0) { throw 'Model tests failed' }
Push-Location $projectRoot
try {
    & "$javaBin\java.exe" -cp $testOutput com.bookworm.reader.EpubTests
    if ($LASTEXITCODE -ne 0) { throw 'EPUB tests failed' }
    & "$javaBin\java.exe" -cp $testOutput com.bookworm.reader.RealEpubTests
    if ($LASTEXITCODE -ne 0) { throw 'Real EPUB tests failed' }
    & "$javaBin\java.exe" -cp $testOutput com.bookworm.reader.LibraryTests
    if ($LASTEXITCODE -ne 0) { throw 'Library tests failed' }
    & "$javaBin\java.exe" '-Djava.awt.headless=true' -cp $testOutput com.bookworm.reader.BinderyTests
    if ($LASTEXITCODE -ne 0) { throw 'Bindery tests failed' }
    & "$javaBin\java.exe" '-Djava.awt.headless=true' -cp $testOutput com.bookworm.reader.BinderyPreview
    if ($LASTEXITCODE -ne 0) { throw 'Bindery preview failed' }
    & "$javaBin\java.exe" -cp $testOutput com.bookworm.reader.ShelfPresentationTests
    if ($LASTEXITCODE -ne 0) { throw 'Shelf presentation tests failed' }
    & "$javaBin\java.exe" '-Djava.awt.headless=true' -cp $testOutput com.bookworm.reader.ShelfUiPreview
    if ($LASTEXITCODE -ne 0) { throw 'Shelf UI preview failed' }
    & "$javaBin\java.exe" -cp $testOutput com.bookworm.reader.ReaderInputTests
    if ($LASTEXITCODE -ne 0) { throw 'Reader input tests failed' }
} finally { Pop-Location }
