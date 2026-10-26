param(
    [string]$Git = 'git',
    [switch]$Staged,
    [string[]]$ForbiddenFiles = @()
)
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$exe=(Get-Command $Git -ErrorAction Stop).Source
function GitText([string[]]$Arguments) {
    $result=& $exe -C $root @Arguments
    if($LASTEXITCODE -ne 0){throw "Git inspection failed: $($Arguments -join ' ')"}
    return $result
}
function BlobBytes([string]$Object) {
    $info=[Diagnostics.ProcessStartInfo]::new($exe)
    $info.WorkingDirectory=$root;$info.UseShellExecute=$false;$info.RedirectStandardOutput=$true;$info.RedirectStandardError=$true;$info.CreateNoWindow=$true
    foreach($a in @('cat-file','blob',$Object)){$info.ArgumentList.Add($a)}
    $process=[Diagnostics.Process]::Start($info);$stream=[IO.MemoryStream]::new()
    try {$process.StandardOutput.BaseStream.CopyTo($stream);$errorText=$process.StandardError.ReadToEnd();$process.WaitForExit();if($process.ExitCode -ne 0){throw $errorText};return ,$stream.ToArray()}
    finally {$process.Dispose();$stream.Dispose()}
}
if($Staged){$files=@(GitText @('ls-files','--cached'));$objects=@(GitText @('ls-files','--stage') | ForEach-Object {($_ -split '\s+',4)[1]} | Sort-Object -Unique)}
else {
    $refs=@(GitText @('for-each-ref','--format=%(refname)'))
    if(!$refs){throw 'No branches/tags to audit'}
    $files=@(foreach($ref in $refs){GitText @('ls-tree','-r','--name-only',$ref)}) | Sort-Object -Unique
    $objects=@(GitText @('cat-file','--batch-all-objects','--batch-check=%(objectname) %(objecttype)') | Where-Object {$_ -match ' blob$'} | ForEach-Object {($_ -split ' ')[0]})
}
$blocked=@($files | Where-Object {$_ -match '(^|/)(releases|local-backups|local-tools|local-validation|\.gradle|\.gradle-user-home|build|\.idea)(/|$)|\.(keystore|jks|p12|pfx|pem|key|apk|aab|zip|part|log)$|(^|/)local\.properties$|(^|/)\.env($|\.)'})
if($blocked){throw "Sensitive/generated paths in Git: $($blocked -join ', ')"}
$jars=@($files | Where-Object {$_ -like '*.jar' -and $_ -ne 'gradle/wrapper/gradle-wrapper.jar'})
$epubs=@($files | Where-Object {$_ -like '*.epub' -and $_ -ne 'app/src/main/assets/books/frankenstein.epub'})
if($jars -or $epubs){throw 'Unreviewed binary or personal EPUB in Git'}
$forbiddenHashes=@($ForbiddenFiles | ForEach-Object {(Get-FileHash -LiteralPath $_ -Algorithm SHA256).Hash})
foreach($object in $objects){
    $bytes=BlobBytes $object
    $hash=[BitConverter]::ToString([Security.Cryptography.SHA256]::Create().ComputeHash($bytes)).Replace('-','')
    if($forbiddenHashes -contains $hash){throw "Private file content found in Git object $object"}
    if($bytes.Length -ge 4 -and $bytes[0] -eq 254 -and $bytes[1] -eq 237 -and $bytes[2] -eq 254 -and $bytes[3] -eq 237){throw "Java keystore content found in object $object"}
    $text=[Text.Encoding]::UTF8.GetString($bytes)
    if($text -match '-----BEGIN (?:RSA |EC |OPENSSH |ENCRYPTED )?PRIVATE KEY-----|github_pat_[A-Za-z0-9_]{20,}|ghp_[A-Za-z0-9]{20,}|AKIA[0-9A-Z]{16}') {throw "Potential private key/credential in Git object $object"}
}
$remotes=@(GitText @('remote'))
if($remotes.Count){throw 'This preparation expects no remotes; inspect before any publication'}
Write-Output "Repository audit passed: $($files.Count) reviewed paths, $($objects.Count) blob objects, no forbidden files/recognized secrets, no remotes. Pattern checks are not a universal secret detector."
