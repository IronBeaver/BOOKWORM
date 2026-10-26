$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$src=Join-Path $root 'app/src/main'
$java=Join-Path $src 'java/com/bookworm/reader'
$adapter=Get-Content -Raw "$java/BookshelfAdapter.java"
$main=Get-Content -Raw "$java/MainActivity.java"
$prefs=Get-Content -Raw "$java/BookshelfPreferences.java"
$appearance=Get-Content -Raw "$java/AppearanceActivity.java"
$spine=Get-Content -Raw "$java/SpineView.java"
$depth=Get-Content -Raw "$java/DimensionalCoverView.java"
$checks=@(
    @{Name='All presentations in canonical settings';Pass=($appearance -match 'layout_spines') -and ($appearance -match 'shelf.presentation') -and ($appearance -match 'Experimental|experimental')},
    @{Name='Separate additive persisted shelf keys';Pass=($prefs -match 'putString\("presentation"') -and ($prefs -match 'putBoolean\("depth"') -and ($prefs -notmatch '\.clear\(')},
    @{Name='Draft depth survives recreation';Pass=($appearance -match 'putBoolean\("draftDepth"') -and ($appearance -match 'getBoolean\("draftDepth"')},
    @{Name='Saved values restored on fresh load';Pass=($prefs -match 'getString\("presentation", null\)') -and ($prefs -match 'getBoolean\("depth", true\)')},
    @{Name='Cover taps still read';Pass=$adapter -match 'card.setOnClickListener\(v -> open.open\(book\)\)'},
    @{Name='Spine tap selects rather than opens reader';Pass=$adapter -match 'cell.setOnClickListener\(v -> \{ selected\(book.id\); select.open\(book\); \}\)'},
    @{Name='Spine long press updates panel and retains existing options';Pass=$adapter -match 'cell.setOnLongClickListener\(v -> \{ selected\(book.id\); select.open\(book\); options.open\(book\)'},
    @{Name='Selection updates visible cells without rebuilding/focus loss';Pass=($adapter -match 'updateSelection\(parent\)') -and ($adapter -match 'cell.setTag\(book.id\)')},
    @{Name='Selected/focused books identified natively';Pass=($adapter -match 'cell.setSelected') -and ($adapter -match 'cell.setFocusable\(true\)') -and ($adapter -match 'state_selected')},
    @{Name='Spine cells are separate nonoverlapping layout children';Pass=($adapter -match 'row.addView\(cell,new LinearLayout.LayoutParams\(0,owner.dp\(266\),1\)\)') -and ($adapter -match 'setMinimumWidth\(owner.dp\(48\)\)')},
    @{Name='Selection panel offers Read/menu/dismiss';Pass=($main -match 'button\("Read",v -> openBook\(book\)\)') -and ($main -match 'Dismiss selected book') -and ($main -match 'Book options for')},
    @{Name='Selection restored transiently by identity and checked against library';Pass=($main -match 'state.getString\("selectedBook"\)') -and ($main -match 'book.id.equals\(selectedBook\)') -and ($main -match 'state.putString\("selectedBook"')},
    @{Name='Back dismisses panel first';Pass=$main -match 'onBackPressed\(\) \{ if\(selectedBook!=null\) dismissSelection\(\)'},
    @{Name='Both views share persistent sorting source';Pass=($adapter -match 'rows = new LibraryRows\(books, sort\)') -and ($adapter -match 'spineBooks=sort.ordered\(books\)')},
    @{Name='Spines reuse Bindery colours and choices';Pass=$spine -match 'CoverChoice.COLORS\[CoverRepository.choice\(book\).colorFor\(book.id\)\]'},
    @{Name='Independent bounded spine cache';Pass=($spine -match '1536 \* 1024') -and ($spine -match 'sizeOf') -and ($spine -match 'Math.min\(640,getHeight\(\)\)')},
    @{Name='Spine drawing has no allocation or regeneration';Pass=$spine.Substring($spine.IndexOf('protected void onDraw')) -notmatch 'new |render\(|createBitmap|prepare\('},
    @{Name='No activities retained in process spine cache';Pass=($spine -match 'LruCache<String, Bitmap>') -and ($spine -notmatch 'static.*Context|static.*Activity|static.*View\s')},
    @{Name='Depth wraps existing cover without modifying renderer';Pass=($depth -match 'new CoverThumbnailView') -and ($depth -notmatch 'createBitmap|LeatherCoverRenderer|setScaleX|setRotation')},
    @{Name='Depth disabled bypasses physical drawing';Pass=$depth -match 'if \(!depth\) return'},
    @{Name='Classic cover support sits at the cover base above metadata';Pass=($adapter -match 'support.getLayoutParams\(\)\).topMargin=width\*3/2') -and ($adapter -match 'card.addView\(support')},
    @{Name='Version and minimum API';Pass=($version=Get-Content -Raw "$root/app/build.gradle") -match 'versionCode 10' -and $version -match "versionName '1.2'" -and $version -match 'minSdk 22'}
)
foreach($check in $checks) { if(!$check.Pass) {throw "Physical shelf regression: $($check.Name)"} }
$protected=Get-Content -Raw "$PSScriptRoot/physical-protected-v1.json" | ConvertFrom-Json
foreach($file in $protected) { if($file.Path -eq 'AndroidManifest.xml') {continue} # v1.2 permission/activity additions are checked separately.
    if((Get-FileHash -LiteralPath (Join-Path $src $file.Path)).Hash -ne $file.Hash) { throw "Protected v1 source changed: $($file.Path)" } }
$release=@(); if(Test-Path "$root/releases/BOOKWORM-v1.0") { $release=Get-Content -Raw "$PSScriptRoot/v1-release-hashes.json" | ConvertFrom-Json } else { Write-Output 'Private v1.0 archive checks skipped: archive is intentionally not distributed.' }
foreach($file in $release) { if((Get-FileHash -LiteralPath (Join-Path "$root/releases/BOOKWORM-v1.0" $file.Path)).Hash -ne $file.Hash) {throw "Protected v1 release changed: $($file.Path)"} }
Write-Output "Passed $($checks.Count+$protected.Count+$release.Count-1) physical shelf static/protected-source/release checks (not native UI execution)."
$output=Join-Path $root 'app/build/physical-tests'
New-Item -ItemType Directory -Force $output | Out-Null
$bin=Join-Path $env:JAVA_HOME 'bin'
& "$bin/javac.exe" -encoding UTF-8 -d $output "$PSScriptRoot/stubs/android/content/Context.java" "$PSScriptRoot/stubs/android/content/SharedPreferences.java" "$java/PhysicalShelf.java" "$java/BookshelfPreferences.java" "$java/CoverTypography.java" "$java/SpineTypography.java" "$PSScriptRoot/PhysicalShelfTests.java"
if($LASTEXITCODE -ne 0) {throw 'Physical shelf compilation failed'}
& "$bin/java.exe" '-Djava.awt.headless=true' -cp $output com.bookworm.reader.PhysicalShelfTests
if($LASTEXITCODE -ne 0) {throw 'Physical shelf tests failed'}
