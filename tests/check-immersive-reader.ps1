$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$sourceRoot = Join-Path $projectRoot 'app\src\main\java\com\bookworm\reader'
$reader = Get-Content -Raw "$sourceRoot\ReaderActivity.java"
$prefs = Get-Content -Raw "$sourceRoot\ReaderPreferences.java"
$settings = Get-Content -Raw "$sourceRoot\ReadingSettingsActivity.java"
$chrome = Get-Content -Raw "$sourceRoot\ReaderChrome.java"
$inputScript = Get-Content -Raw (Join-Path $projectRoot 'app\src\main\assets\reader-input.js')
function Get-Method([string]$source, [string]$name) {
    $matches = [regex]::Matches($source, '(?m)^[ \t]*(private|public|protected)[ \t]+(?:static[ \t]+)?[\w<>\[\].?,]+[ \t]+' + [regex]::Escape($name) + '\(')
    if ($matches.Count -eq 0) { throw "Method missing: $name" }
    $match = $matches[$matches.Count - 1] # Prefer the actual private navigation method after anonymous UI action delegates.
    $opening = $source.IndexOf('{', $match.Index)
    $depth = 0
    for ($i = $opening; $i -lt $source.Length; $i++) {
        if ($source[$i] -eq '{') { $depth++ }
        if ($source[$i] -eq '}') { $depth--; if ($depth -eq 0) { return $source.Substring($match.Index, $i - $match.Index + 1).Trim() } }
    }
    throw "Unterminated method: $name"
}
$checks = @(
    @{Name='WebView fills frame apart from compact safe fallback rail'; Pass=($reader -match 'pageArea = new FrameLayout.LayoutParams\(-1, -1\)') -and ($reader -match 'pageArea.bottomMargin = dp\(48\)') -and ($reader -notmatch 'root.addView\(web, new LinearLayout')},
    @{Name='Overlay is a sibling, not viewport padding'; Pass=($chrome -match 'root.addView\(overlay, new FrameLayout.LayoutParams\(-1, -1\)\)') -and ($chrome -notmatch 'web.setPadding|loadUrl|BW.init')},
    @{Name='Confirmed single taps only'; Pass=($reader -match 'onSingleTapConfirmed') -and ($reader -match 'onDoubleTap') -and ($reader -match 'onLongPress')},
    @{Name='Native listener never consumes WebView events'; Pass=$reader -match '(?s)detector.onTouchEvent\(event\);\s*return false;'},
    @{Name='Input guards both DOM and native link targets'; Pass=($reader -match 'getHitTestResult') -and ($reader -match 'BWInput.hit') -and ($inputScript -match "tag === 'a'")},
    @{Name='Selection checked at down and confirmation'; Pass=($reader -match 'current.block\(\)') -and ($reader -match 'selected \|\| web.selecting\(\)') -and ($inputScript -match '!selected.isCollapsed')},
    @{Name='Pointer cancellation and native touch slop retained'; Pass=($reader -match 'ACTION_POINTER_DOWN') -and ($reader -match 'ACTION_CANCEL') -and ($reader -match 'getScaledTouchSlop')},
    @{Name='Overlay/default controls and progress are separate saved preferences'; Pass=($prefs -match 'getBoolean\("controls_on_open", false\)') -and ($prefs -match 'getBoolean\("show_progress", true\)') -and ($prefs -match 'putBoolean\("controls_on_open", controlsOnOpen\)') -and ($prefs -match 'putBoolean\("show_progress", showProgress\)')},
    @{Name='Reading preference transaction does not clear existing settings'; Pass=$prefs -notmatch '\.clear\('},
    @{Name='Preference controls preserve drafts'; Pass=($settings -match 'putBoolean\("controls"') -and ($settings -match 'getBoolean\("controls"') -and ($settings -match 'draft.showProgress = progress.isChecked')},
    @{Name='Transient overlay state survives recreation'; Pass=($reader -match 'getBoolean\("chromeShown"') -and ($reader -match 'putBoolean\("chromeShown"')},
    @{Name='Accessibility enables foreground controls and listener is released'; Pass=($reader -match 'isTouchExplorationEnabled') -and ($reader -match 'enabled && foreground') -and ($reader -match 'removeTouchExplorationStateChangeListener')},
    @{Name='Visible Controls fallback and native library path'; Pass=($chrome -match 'control\("Controls"') -and ($chrome -match 'Back to library') -and ($chrome -match 'owner.dp\(48\)')},
    @{Name='Page and chapter alternatives remain native'; Pass=($chrome -match 'Previous page') -and ($chrome -match 'Next chapter')},
    @{Name='Hardware Back preserves selection, chrome, then existing history'; Pass=($reader -match 'web.finishSelection\(\)') -and ($reader -match 'chromeState.shown\(\)') -and ($reader -match 'history.pop\(\)')},
    @{Name='Input script only inspects DOM, with no publisher bridge or layout edits'; Pass=($inputScript -match 'elementFromPoint') -and ($inputScript -notmatch 'addEventListener|preventDefault|innerHTML\s*=|\.style\.|addJavascriptInterface')}
    @{Name='Native controls cancel delayed tap actions'; Pass=$reader -match 'private void showChrome\(bool[^\r\n]*tap.cancel\(\)'},
    @{Name='Overlay announces without forcing screen-reader focus'; Pass=($chrome -match 'announceForAccessibility') -and ($chrome -notmatch 'ACTION_ACCESSIBILITY_FOCUS')}
    @{Name='Control panels consume background taps without hiding accessible children'; Pass=($chrome -match 'top.setClickable\(true\)') -and ($chrome -match 'bottom.setClickable\(true\)') -and ($chrome -notmatch 'IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS')}
    @{Name='Fallback remains measured when hidden under overlay'; Pass=($chrome -match 'dock.setVisibility\(shown \? View.INVISIBLE : View.VISIBLE\)') -and ($reader -match 'chrome.reserveFallback\(web\)')}
)
foreach ($check in $checks) { if (!$check.Pass) { throw "Immersive regression: $($check.Name)" } }
$baseline = Get-Content -Raw (Join-Path $projectRoot 'tests\fixtures\v7\ReaderActivity.java.txt')
$methods = @('localUrl','resource','blocked','claimImage','load','update','persist','capture','turn','chapterAtBoundary','chapter','follow','contents','closeReader')
foreach ($name in $methods) {
    if ((Get-Method $reader $name) -cne (Get-Method $baseline $name)) { throw "Protected reader method changed: $name" }
}
$oldPrefs = Get-Content -Raw (Join-Path $projectRoot 'tests\fixtures\v7\ReaderPreferences.java.txt')
if ((Get-Method $prefs 'css') -cne (Get-Method $oldPrefs 'css')) { throw 'Reader typography CSS changed' }
$protected = Get-Content -Raw (Join-Path $PSScriptRoot 'immersive-protected-v7.json') | ConvertFrom-Json
foreach ($file in $protected) {
    if ($file.Name -in @('AppearanceActivity.java','BookshelfAdapter.java','MainActivity.java','SettingsActivity.java')) { continue } # v1.1 shelf UI only; full v1 reader hashes checked by physical shelf guards.
    if ((Get-FileHash -LiteralPath (Join-Path $sourceRoot $file.Name) -Algorithm SHA256).Hash -ne $file.Hash) { throw "Protected v7 subsystem changed: $($file.Name)" }
}
$expected = (Get-Content -Raw (Join-Path $PSScriptRoot 'fixtures/v7/reader-js.sha256')).Trim()
if ($expected -ne (Get-FileHash (Join-Path $projectRoot 'app/src/main/assets/reader.js')).Hash) { throw 'Pagination engine script changed' }
Write-Output "Passed $($checks.Count + $methods.Count + 2 + $protected.Count - 4) immersive UI/core-method/protected-source checks (not native UI execution)."
