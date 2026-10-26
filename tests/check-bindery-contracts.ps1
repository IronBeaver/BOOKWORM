$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$sourceRoot = Join-Path $projectRoot 'app\src\main\java\com\bookworm\reader'
$repository = Get-Content -Raw "$sourceRoot\CoverRepository.java"
$view = Get-Content -Raw "$sourceRoot\CoverThumbnailView.java"
$theme = Get-Content -Raw "$sourceRoot\ThemeStore.java"
$appearance = Get-Content -Raw "$sourceRoot\AppearanceActivity.java"
$dialog = Get-Content -Raw "$sourceRoot\BinderyDialog.java"
$renderer = Get-Content -Raw "$sourceRoot\LeatherCoverRenderer.java"
$checks = @(
    @{Name='Global cover choice uses existing appearance preference file'; Pass=($repository -match 'getSharedPreferences\("appearance"') -and ($repository -match 'getString\("cover_policy", "AUTO"\)')},
    @{Name='Global cover policy is committed with appearance without clearing existing keys'; Pass=($theme -match 'putString\("cover_policy", coverPolicy\.name\(\)\)') -and ($theme -notmatch '\.clear\(')},
    @{Name='Appearance drafts retain policy across activity recreation'; Pass=($appearance -match 'getInt\("draftCover"') -and ($appearance -match 'putInt\("draftCover"')},
    @{Name='Global and per-book policies both exposed'; Pass=($appearance -match 'global_cover_leather') -and ($dialog -match 'cover_follow') -and ($dialog -match 'cover_original') -and ($dialog -match 'cover_leather')},
    @{Name='Thumbnail cache capped at three MiB'; Pass=$repository -match '3 \* 1024 \* 1024'},
    @{Name='Background work queue bounded and views weakly held'; Pass=($repository -match 'ArrayBlockingQueue<Runnable>\(64\)') -and ($repository -match 'WeakReference<CoverThumbnailView>')},
    @{Name='No rendering or decoding during onDraw'; Pass=($view.Substring($view.IndexOf('protected void onDraw')) -notmatch 'createBitmap|render\(|decodeFile|new RectF')},
    @{Name='Native text layout has final font-metric overflow check'; Pass=($renderer -match 'StaticLayout') -and ($renderer -match 'layout\.getHeight\(\) > h')},
    @{Name='Renderer only allocates requested bitmap resolution'; Pass=$renderer -match 'Bitmap\.createBitmap\(width, height, Bitmap\.Config\.RGB_565\)'},
    @{Name='Saving per-book options refreshes the bookshelf'; Pass=$dialog -match 'CoverRepository\.save\(book, value\); changed\.run\(\)'}
)
foreach ($check in $checks) { if (!$check.Pass) { throw "Bindery contract: $($check.Name)" } }
$protected = Get-Content -Raw (Join-Path $projectRoot 'tests\reader-source-v5-hashes.json') | ConvertFrom-Json
foreach ($file in $protected) {
    if ($file.Name -in @('ReaderActivity.java','ReaderPreferences.java')) { continue } # Milestone 003.6 presentation changes; new core-method and v7 guards cover these explicitly.
    $actual = (Get-FileHash -LiteralPath (Join-Path $sourceRoot $file.Name) -Algorithm SHA256).Hash
    if ($actual -ne $file.Hash) { throw "Protected reader source changed: $($file.Name)" }
}
Write-Output "Passed $($checks.Count + $protected.Count - 2) static bindery/unchanged-reader checks (not native UI execution)."
