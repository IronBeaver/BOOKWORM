$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$sourceRoot = Join-Path $projectRoot 'app\src\main\java\com\bookworm\reader'
$adapter = Get-Content -Raw "$sourceRoot\BookshelfAdapter.java"
$main = Get-Content -Raw "$sourceRoot\MainActivity.java"
# These height equations reproduce the relevant API 22 LinearLayout branches;
# they are a host model, not execution of Android UI code.
function Get-Api22RowHeight([bool[]]$matchParent, [int[]]$heights) {
    $allFillParent = $true
    $maximum = 0
    $alternative = 0
    for ($i = 0; $i -lt $heights.Length; $i++) {
        $allFillParent = $allFillParent -and $matchParent[$i]
        $maximum = [Math]::Max($maximum, $heights[$i])
        $candidate = if ($matchParent[$i]) { 0 } else { $heights[$i] }
        $alternative = [Math]::Max($alternative, $candidate)
    }
    if (!$allFillParent) { $maximum = $alternative }
    return $maximum
}
$checks = @(
    @{Name='Original odd-row height collapse reproduced'; Pass=(Get-Api22RowHeight @($true,$false) @(280,1)) -eq 1},
    @{Name='Content-sized one-book row retains full height'; Pass=(Get-Api22RowHeight @($false,$false) @(280,0)) -eq 280},
    @{Name='Unequal two-book heights retain taller card'; Pass=(Get-Api22RowHeight @($false,$false) @(280,340)) -eq 340},
    @{Name='Production weighted cells use content height'; Pass=$adapter -match 'row\.addView\(card, new LinearLayout\.LayoutParams\(0, LinearLayout\.LayoutParams\.WRAP_CONTENT, 1\)\)'},
    @{Name='Visible shelf contains no demo catalogue'; Pass=($main+$adapter) -notmatch 'DemoBooks|demoCard|BookCoverView'},
    @{Name='Count and access use same snapshot'; Pass=($adapter -match 'return rows\.rowCount\(\)') -and ($adapter -match 'rows\.at\(index, column\)')},
    @{Name='Refresh submits scanned persistent records'; Pass=($main -match 'books = snapshot\.books; adapter\.submit\(books, sort\)')},
    @{Name='Book opening uses persistent identifier'; Pass=$main -match '\.putExtra\("book", book\.id\)'},
    @{Name='Native sort preference saved and restored'; Pass=($main -match 'getString\("sort"') -and ($main -match 'putString\("sort", next\.name\(\)\)')},
    @{Name='Bindery uses common portrait cover view without icon fallback'; Pass=($adapter -match 'CoverThumbnailView') -and ($adapter -notmatch 'setImageResource|ic_bookworm')}
)
foreach ($check in $checks) { if (!$check.Pass) { throw "Shelf regression: $($check.Name)" } }
$bundled = Get-Content -Raw "$sourceRoot\BundledBook.java"
$identity = [regex]::Match($bundled, 'ID = "([0-9a-f]{64})"').Groups[1].Value
$hash = (Get-FileHash (Join-Path $projectRoot 'app\src\main\assets\books\frankenstein.epub') -Algorithm SHA256).Hash.ToLowerInvariant()
if ($hash -ne $identity) { throw 'Bundled EPUB asset does not match its production identity' }
Write-Output "Passed $($checks.Count+1) shelf layout model/static/asset checks (not native UI execution)."
