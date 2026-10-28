$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$out=Join-Path $root 'app/build/reader-font-tests'
$assets=Join-Path $root 'app/src/main/assets'
$manifest=Get-Content -Raw "$PSScriptRoot/fixtures/font-prototype/assets/font-prototype/manifest.json" | ConvertFrom-Json
$cases=@()
$fontData=@{}
foreach($f in $manifest.fonts){$fontData[$f.file]='data:font/ttf;base64,'+[Convert]::ToBase64String([IO.File]::ReadAllBytes((Join-Path $assets ('fonts/'+$f.file))))}
foreach($id in @('system_serif','system_sans','system_monospace','eb_garamond','literata','atkinson_next','bitter')) {
    foreach($palette in @('appearance','warm_paper','soft_blue','blue_night','soft_dark')) {
    $css=(& "$env:JAVA_HOME/bin/java.exe" -cp $out com.bookworm.reader.ReaderFontTests $id 0 $palette) -join "`n"
    if($LASTEXITCODE -ne 0){throw 'Production CSS fixture failed'}
    $alias=if($id.StartsWith('system_')){''}else{[regex]::Match($css,'@font-face\{font-family:([^;]+)').Groups[1].Value}
    $cases+=@{id=$id;palette=$palette;alias=$alias;css=$css;bg=[regex]::Match($css,'background:(#[A-F0-9]{6})').Groups[1].Value;text=[regex]::Match($css,'color:(#[A-F0-9]{6})').Groups[1].Value;accent=[regex]::Match($css,'a\{color:(#[A-F0-9]{6})').Groups[1].Value}
    }
}
$harness=@'
<!doctype html><html><head><meta charset="utf-8"></head><body><pre id="results">RUNNING</pre><iframe id="chapter" style="width:380px;height:640px;border:0"></iframe><script>
var cases=__BW_CASES_JSON__, gateScript=__BW_GATE_JSON__, engine=__BW_ENGINE_JSON__, input=__BW_INPUT_JSON__;
var fontData=__BW_FONT_DATA_JSON__;
for(var ci=0;ci<cases.length;ci++)cases[ci].css=cases[ci].css.replace(/https:\/\/bookworm.invalid\/__bookworm_fonts\/([A-Za-z-]+\.ttf)/g,function(url,file){return fontData[file];});
var frame=document.getElementById('chapter'), output=document.getElementById('results'), checks=[], stage=0, font=0, paged=true;
window.onerror=function(message,url,line){output.textContent='FAIL: '+message+' at '+line;};
var content='<p><a href="#note">Internal note</a> <b>Bold</b> <i>Italic</i> <b><i>Bold italic</i></b></p>';
for(var n=0;n<180;n++)content+='<p>Paragraph '+n+'. The quiet library holds stories of distant gardens. Café, naïve, Łódź, Șș, Țț; e\u0301, o\u030b. Affinity, office, first. “Quoted words” — and a stable text anchor.</p>';
function check(ok,label){if(!ok){output.textContent='FAIL: '+label;throw new Error(label);}checks.push(label);}
function load(css,callback){frame.onload=function(){frame.contentWindow.onerror=window.onerror;callback(frame.contentWindow);};frame.srcdoc='<html><head><meta charset="utf-8"><style>'+css+'</style></head><body><main id="bw-content">'+content+'</main></body></html>';}
function wait(w,state,callback){var deadline=Date.now()+10000;function poll(){var s=w.BWFonts.status();if(s.state===state){callback(s);return;}if(Date.now()>deadline){check(false,'Timeout '+JSON.stringify(s));return;}setTimeout(poll,20);}poll();}
function visible(w,offset){var walker=w.document.createTreeWalker(w.document.getElementById('bw-content'),4,null,false),n,total=0;while((n=walker.nextNode())){if(total+n.length>offset){var r=w.document.createRange();r.setStart(n,offset-total);r.setEnd(n,offset-total+1);var b=r.getBoundingClientRect();return b.left>=-2&&b.left<w.innerWidth&&b.top>=-2&&b.top<w.innerHeight;}total+=n.length;}return false;}
function init(w,c){
check(!w.BW,'No pagination before font barrier: '+c.id);w.eval(engine);w.eval(input);
function rgb(hex){return 'rgb('+parseInt(hex.substr(1,2),16)+', '+parseInt(hex.substr(3,2),16)+', '+parseInt(hex.substr(5,2),16)+')';}
check(w.getComputedStyle(w.document.body).backgroundColor===rgb(c.bg),'Reader background: '+c.palette);
check(w.getComputedStyle(w.document.querySelector('a')).color===rgb(c.accent),'Reader links: '+c.palette);
if(c.palette!=='appearance'){var highlight=w.getComputedStyle(w.document.querySelector('p'),'::selection');check(highlight.backgroundColor===rgb(c.accent)&&highlight.color===rgb(c.bg),'Selection contrast colours: '+c.palette);}
var p=w.BW.init(paged,20,5000,'');check(visible(w,5000),'Restored text anchor visible: '+c.id+' '+paged);
check(Math.abs(p.offset-5000)<2000,'Approximate position retained: '+c.id+' '+paged);
var selection=w.getSelection(),range=w.document.createRange();range.selectNodeContents(w.document.querySelector('#bw-content p'));selection.removeAllRanges();selection.addRange(range);
check(selection.toString().length>0&&w.BWInput.hit(0.5,0.5).selection,'DOM selection guard retained: '+c.id+' '+paged);selection.removeAllRanges();
if(paged){var x=w.pageXOffset;w.BW.turn(1);check(w.pageXOffset===x+w.innerWidth,'Page turn remains one viewport: '+c.id);w.BW.turn(-1);}
else{var y=w.pageYOffset;w.scrollBy(0,100);check(w.pageYOffset>=y+99,'Vertical scrolling works: '+c.id);}
}
function next(){
if(font===cases.length){if(paged){paged=false;font=0;}else{failureTests();return;}}
var c=cases[font++];load(c.css,function(w){w.eval(gateScript);w.BWFonts.begin(c.alias);wait(w,'ready',function(s){check(s.faces===(c.alias?4:0),'Only selected family measured: '+c.id);init(w,c);next();});});
}
function delayedTest(){var c=cases.filter(function(c){return !!c.alias;})[0];load(c.css.replace(/@font-face\{[^}]+\}/g,''),function(w){
w.eval(gateScript);w.BWFonts.begin(c.alias);setTimeout(function(){check(w.BWFonts.status().state==='pending'&&!w.BW,'Missing faces keep pagination blocked');var style=w.document.createElement('style');style.textContent=c.css;w.document.head.appendChild(style);wait(w,'ready',function(){check(!w.document.querySelector('[aria-hidden="true"]'),'Measurement probes removed before pagination');init(w,c);next();});},200);
});}
function failureTests(){load(cases[0].css,function(w){
w.eval(gateScript);w.BWFonts.begin('MissingFace');setTimeout(function(){
check(w.BWFonts.status().state==='pending'&&!w.BW,'Fallback is never mistaken for ready');
w.BWFonts.begin('');check(w.BWFonts.status().state==='ready','A new gate cancels old probes');
w.BWFonts.begin('MissingFace');var realNow=w.Date.now;w.Date.now=function(){return realNow()+31000;};
wait(w,'failed',function(){w.Date.now=realNow;check(!w.BW,'Diagnostic timeout never starts pagination');check(!w.document.querySelector('[aria-hidden="true"]'),'Failed gate cleans probes');output.textContent='PASS: '+checks.length+' checks; 7 fonts × 5 palettes × 2 modes; delayed loading, anchor restoration, selection colours/guard, page turns, scroll and failure recovery.';});
},100);
});}
delayedTest();
</script></body></html>
'@
$harness=$harness.Replace('__BW_CASES_JSON__',($cases|ConvertTo-Json -Depth 5 -Compress)).Replace('__BW_FONT_DATA_JSON__',($fontData|ConvertTo-Json -Depth 3 -Compress)).Replace('__BW_GATE_JSON__',(ConvertTo-Json -InputObject (Get-Content -Raw "$assets/reader-fonts.js") -Compress)).Replace('__BW_ENGINE_JSON__',(ConvertTo-Json -InputObject (Get-Content -Raw "$assets/reader.js") -Compress)).Replace('__BW_INPUT_JSON__',(ConvertTo-Json -InputObject (Get-Content -Raw "$assets/reader-input.js") -Compress))
$fixture=Join-Path $out 'font-reader.html'
[IO.File]::WriteAllText($fixture,$harness)
$browser='C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe'
if(!(Test-Path $browser)){throw 'Installed Edge required for this optional host test; no browser is downloaded'}
$profile=Join-Path $out ('profile-'+[Guid]::NewGuid().ToString('N'))
$arguments=@('--headless','--disable-gpu','--no-first-run','--no-default-browser-check','--disable-background-networking','--remote-debugging-port=0',('--user-data-dir="'+$profile+'"'),'about:blank')
$process=Start-Process -FilePath $browser -ArgumentList $arguments -WindowStyle Hidden -PassThru
$socket=$null
try {
    $deadline=[DateTime]::UtcNow.AddSeconds(15);$target=$null
    do {
        $portFile=Join-Path $profile 'DevToolsActivePort'
        if(Test-Path $portFile){$port=(Get-Content $portFile)[0];$targets=Invoke-RestMethod "http://127.0.0.1:$port/json/list" -TimeoutSec 3;$target=$targets|Where-Object url -eq 'about:blank'|Select-Object -First 1}
        if(!$target){Start-Sleep -Milliseconds 100}
    }while(!$target -and [DateTime]::UtcNow -lt $deadline)
    if(!$target){throw 'Temporary browser target unavailable'}
    $socket=[Net.WebSockets.ClientWebSocket]::new()
    $null=$socket.ConnectAsync([Uri]$target.webSocketDebuggerUrl,[Threading.CancellationToken]::None).GetAwaiter().GetResult()
    $script:readerFontCdpId=0
    $script:readerFontBrowserErrors=@()
    function Cdp([string]$method,$parameters){
        $script:readerFontCdpId++;$id=$script:readerFontCdpId
        $b=[Text.Encoding]::UTF8.GetBytes((@{id=$id;method=$method;params=$parameters}|ConvertTo-Json -Compress -Depth 5))
        $cancel=[Threading.CancellationTokenSource]::new(10000)
        try {
            $null=$socket.SendAsync([ArraySegment[byte]]::new($b),[Net.WebSockets.WebSocketMessageType]::Text,$true,$cancel.Token).GetAwaiter().GetResult()
            while($true){$ms=[IO.MemoryStream]::new();do{$buf=[byte[]]::new(32768);$r=$socket.ReceiveAsync([ArraySegment[byte]]::new($buf),$cancel.Token).GetAwaiter().GetResult();$ms.Write($buf,0,$r.Count)}while(!$r.EndOfMessage);$reply=[Text.Encoding]::UTF8.GetString($ms.ToArray())|ConvertFrom-Json;$ms.Dispose();if($reply.method -eq 'Runtime.exceptionThrown'){$script:readerFontBrowserErrors+=$reply.params.exceptionDetails|ConvertTo-Json -Depth 8 -Compress};if($reply.id -eq $id){return $reply}}
        } finally {$cancel.Dispose()}
    }
    $null=Cdp 'Runtime.enable' @{}
    $null=Cdp 'Page.navigate' @{url=([Uri]$fixture).AbsoluteUri}
    $null=Cdp 'Page.bringToFront' @{}
    $deadline=[DateTime]::UtcNow.AddSeconds(45);$result=''
    do{
        $reply=Cdp 'Runtime.evaluate' @{expression="document.getElementById('results') ? document.getElementById('results').textContent : 'loading'";returnByValue=$true}
        $result=$reply.result.result.value
        if($script:readerFontBrowserErrors.Count){throw ($script:readerFontBrowserErrors -join "`n")}
        if($result -match '^(PASS|FAIL):'){break}
        Start-Sleep -Milliseconds 100
    }while([DateTime]::UtcNow -lt $deadline)
    $result | Set-Content (Join-Path $out 'browser-result.txt')
    if($result -notmatch '^PASS:'){throw "Reader font browser test: $result"}
    Write-Output "$result Desktop only: uses data-font fixtures in place of Android's intercepted HTTPS route."
} finally {
    if($socket){try{$null=Cdp 'Browser.close' @{}}catch{};$socket.Dispose()}
}
