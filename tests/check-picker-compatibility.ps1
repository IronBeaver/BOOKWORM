$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$source = Get-Content -Raw (Join-Path $projectRoot 'app\src\main\java\com\bookworm\reader\ImportActivity.java')
# Static contract checks only: no Android framework or hardware picker is executed.
$code = [regex]::Replace($source, '(?m)//[^\r\n]*', '')
$checks = @(
    @{ Name = 'Document picker uses the shared configuration'; Pass = $code -match 'pickerIntent\(Intent\.ACTION_OPEN_DOCUMENT\)' },
    @{ Name = 'Content fallback uses the shared configuration'; Pass = $code -match 'pickerIntent\(Intent\.ACTION_GET_CONTENT\)' },
    @{ Name = 'Shared configuration accepts all openable file MIME types'; Pass = $code -match 'return\s+new Intent\(action\)\.setType\("\*/\*"\)\.addCategory\(Intent\.CATEGORY_OPENABLE\)' },
    @{ Name = 'No competing restrictive MIME filter'; Pass = ([regex]::Matches($code, '\.setType\(').Count -eq 1) -and ($code -notmatch 'EXTRA_MIME_TYPES') },
    @{ Name = 'Provider type and filename are not used as EPUB gates'; Pass = $code -notmatch 'getType\(|DISPLAY_NAME|getLastPathSegment\(|\.endsWith\(' },
    @{ Name = 'Selected stream still reaches bounded EPUB validation'; Pass = $code -match 'EpubParser\.importBook\(input, LibraryStore\.root\(app\)\)' }
)
foreach ($check in $checks) { if (!$check.Pass) { throw "Picker regression: $($check.Name)" } }
Write-Output "Passed $($checks.Count) static picker compatibility contract checks (not a device test)."
