$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$sourceRoot = Join-Path $projectRoot 'app\src\main\java\com\bookworm\reader'
$main = Get-Content -Raw "$sourceRoot\MainActivity.java"
$adapter = Get-Content -Raw "$sourceRoot\BookshelfAdapter.java"
$ui = Get-Content -Raw "$sourceRoot\ShelfUi.java"
$settings = Get-Content -Raw "$sourceRoot\SettingsActivity.java"
$appearance = Get-Content -Raw "$sourceRoot\AppearanceActivity.java"
$checks = @(
    @{Name='Toolbar exposes Sort/Import/Settings'; Pass=($main -match 'ShelfIcon.Kind.SORT') -and ($main -match 'ShelfIcon.Kind.IMPORT') -and ($main -match 'ShelfIcon.Kind.SETTINGS')},
    @{Name='Existing importer preserved as navigation target'; Pass=$main -match 'new Intent\(this, ImportActivity.class\)'},
    @{Name='No old appearance/attribution button rows on shelf'; Pass=($main -notmatch 'button\("Appearance"|button\("About Frankenstein"|sortButton')},
    @{Name='Toolbar icons have 48dp targets, labels and native focus'; Pass=($ui -match 'setMinimumWidth\(owner.dp\(48\)\)') -and ($ui -match 'setMinimumHeight\(owner.dp\(48\)\)') -and ($ui -match 'setContentDescription\(label\)')},
    @{Name='Title two lines and author one line use ellipsis'; Pass=($adapter -match 'title.setLines\(2\)') -and ($adapter -match 'author.setSingleLine\(true\)') -and ([regex]::Matches($adapter,'setEllipsize').Count -eq 2)},
    @{Name='Metadata sizes use sp TextView APIs and contrast-aware helper'; Pass=($adapter -match 'title.setTextSize\(ShelfPresentation.titleSize') -and ($adapter -match 'ShelfPresentation.muted\(owner.appearance\)')},
    @{Name='Long press and accessible overflow both open options'; Pass=($adapter -match 'setOnLongClickListener') -and ($adapter -match 'ShelfIcon.Kind.MORE') -and ($adapter -match 'Book options for')},
    @{Name='No persistent Cover options button'; Pass=$adapter -notmatch 'button\("Cover options"'},
    @{Name='Options retain Read/Bindery/details/cancel'; Pass=($main -match 'new String\[\]\{"Read", "Cover options", "Book details"\}') -and ($main -match 'BinderyDialog.show') -and ($main -match 'setNegativeButton\("Cancel"')},
    @{Name='Options preserve live cover refresh'; Pass=$main -match 'BinderyDialog.show\(this, book, adapter::notifyDataSetChanged\)'},
    @{Name='Settings groups present'; Pass=($settings -match 'section\(groups, "Appearance"') -and ($settings -match 'section\(groups, "Bookshelf"') -and ($settings -match 'section\(groups, "Reading"') -and ($settings -match 'section\(groups, "About"')},
    @{Name='Settings reuses existing canonical preference screens'; Pass=($settings -match 'AppearanceActivity.class') -and ($settings -match 'ReadingSettingsActivity.class') -and ($settings -notmatch '\.edit\(')},
    @{Name='Shelf-layout route scrolls existing control without a duplicate preference'; Pass=($settings -match 'putExtra\("focus_bookshelf", true\)') -and ($appearance -match 'getBooleanExtra\("focus_bookshelf", false\)')},
    @{Name='About retains offline bundled license'; Pass=$settings -match 'BundledBookActivity.class'},
    @{Name='Card opening uses persistent book identity'; Pass=$main -match 'putExtra\("book", book.id\)'},
    @{Name='Scroll restore waits for actual visible children'; Pass=$main -match 'list.getChildCount\(\) > 0'},
    @{Name='Summary reflects selected sort after state changes'; Pass=($main -match 'ShelfPresentation.summary\(books.size\(\), sort\)') -and ($main -match 'sort = next; updateSummary\(\)')}
)
foreach ($check in $checks) { if (!$check.Pass) { throw "UI polish: $($check.Name)" } }
$protected = Get-Content -Raw (Join-Path $PSScriptRoot 'ui-polish-protected-v6.json') | ConvertFrom-Json
foreach ($file in $protected) {
    if ($file.Name -in @('ReaderActivity.java','ReaderPreferences.java','ReadingSettingsActivity.java')) { continue } # Authorized reader presentation/preferences, now audited by immersive guards.
    if ((Get-FileHash -LiteralPath (Join-Path $sourceRoot $file.Name) -Algorithm SHA256).Hash -ne $file.Hash) { throw "Protected v6 subsystem changed: $($file.Name)" }
}
Write-Output "Passed $($checks.Count + $protected.Count - 3) static UI/protected-v6-source checks (not native UI execution)."
