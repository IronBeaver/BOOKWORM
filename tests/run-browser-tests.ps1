$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$testFolder = Join-Path $projectRoot 'app\build\browser-tests'
New-Item -ItemType Directory -Force $testFolder | Out-Null
$engine = Get-Content -Raw (Join-Path $projectRoot 'app\src\main\assets\reader.js')
$guard = Get-Content -Raw (Join-Path $projectRoot 'app\src\main\assets\reader-input.js')
$paragraphs = (1..220 | ForEach-Object { "<p>Paragraph $_. The quiet library holds stories of distant gardens, lanterns and rivers. Each line gives the reader a stable text anchor across changing page geometry.</p>" }) -join ''
$harness = @'
<!doctype html><html><body><pre id="results">RUNNING</pre><iframe id="reader" style="width:600px;height:700px;border:0"></iframe><script>
var engine=ENGINE, guard=GUARD, content=CONTENT, frame=document.getElementById('reader'), results=[], saved=0, stage=0;
function check(value,label){if(!value){document.getElementById('results').textContent='FAIL: '+label+'; '+JSON.stringify(results);throw new Error(label);}results.push(label);}
function targetVisible(w,offset){var walker=w.document.createTreeWalker(w.document.getElementById('bw-content'),4,null,false),n,total=0;while((n=walker.nextNode())){if(total+n.length>offset){var r=w.document.createRange();r.setStart(n,offset-total);r.setEnd(n,Math.min(n.length,offset-total+1));var b=r.getBoundingClientRect();return b.left>=-2&&b.left<w.innerWidth&&b.top>=-2&&b.top<w.innerHeight;}total+=n.length;}return false;}
function load(font){frame.srcdoc='<html><head><style>html,body{margin:0;padding:0}body{font:'+font+'px serif;line-height:1.6}#bw-content{box-sizing:border-box;padding:20px;}p{orphans:1;widows:1}</style></head><body><main id="bw-content"><p><a id="left-link" href="two.xhtml#note"><span>Local note</span></a></p><a id="right-link" style="position:absolute;right:24px;top:70px" href="two.xhtml#note">Right note</a><p id="selection-text">Selectable ordinary chapter text.</p>'+content+'</main><script>'+engine+'\n'+guard+'<\/script></body></html>';}
function hitTests(w){
function at(id){var r=w.document.getElementById(id).getBoundingClientRect();return w.BWInput.hit((r.left+r.width/2)/w.innerWidth,(r.top+r.height/2)/w.innerHeight);}
check(at('left-link').interactive,'Nested left-edge internal link protected');check(at('right-link').interactive,'Right-edge internal link protected');
check(!at('selection-text').interactive,'Ordinary chapter text remains eligible');check(w.BWInput.hit(-1,0.5).interactive,'Missing DOM target fails closed');
var selected=w.getSelection(),range=w.document.createRange();range.selectNodeContents(w.document.getElementById('selection-text'));selected.removeAllRanges();selected.addRange(range);
var before=selected.toString();check(at('selection-text').selection,'Active text selection suppresses tap zones');check(selected.toString()===before,'Hit testing does not alter selected text');selected.removeAllRanges();check(!at('selection-text').selection,'Cleared selection can return to reading');
var link=w.document.getElementById('left-link'),clicked=0;link.addEventListener('click',function(e){clicked++;e.preventDefault();});at('left-link');link.click();check(clicked===1&&link.getAttribute('href')==='two.xhtml#note','Input guard preserves native link behavior and target');
var h=w.innerHeight,width=w.innerWidth,pos=w.BW.position().offset,overlay=document.createElement('div');overlay.style.position='absolute';overlay.style.top='0';overlay.style.width='600px';overlay.style.height='100px';document.body.appendChild(overlay);check(w.innerHeight===h&&w.innerWidth===width,'Overlay siblings do not resize reading viewport');overlay.style.display='none';check(w.BW.position().offset===pos,'Showing/hiding overlay preserves content anchor');overlay.parentNode.removeChild(overlay);
}
frame.onload=function(){setTimeout(function(){var w=frame.contentWindow,p;
if(stage===0){p=w.BW.init(true,20,0,'');hitTests(w);check(p.pages>5,'Viewport pagination creates multiple pages');check(p.page===1,'Starts on first page');check(w.BW.turn(-1).boundary===-1,'Previous at start crosses chapter boundary');w.BW.turn(1);p=w.BW.position();check(p.page===2&&w.pageXOffset===w.innerWidth,'Next edge advances exactly one viewport (page '+p.page+', x '+w.pageXOffset+', width '+w.innerWidth+')');check(p.offset>0,'Page position uses positive text anchor');saved=p.offset;stage=1;load(20);}
else if(stage===1){p=w.BW.init(false,20,saved,'');check(w.pageYOffset>0,'Scroll restores from text anchor');check(targetVisible(w,saved),'Original page anchor visible after scroll mode switch');saved=p.offset;stage=2;frame.style.height='540px';load(28);}
else if(stage===2){p=w.BW.init(true,32,saved,'');check(targetVisible(w,saved),'Anchor remains visible after mode, typography and viewport changes');check(p.page>1,'Reflow restores a later page');stage=3;load(20);}
else if(stage===3){p=w.BW.init(true,20,-1,'');check(p.page===p.pages,'Previous-chapter entry restores final page');check(w.BW.turn(1).boundary===1,'Next at end crosses chapter boundary');check(w.BW.turn(-1).boundary===0,'Previous page works');document.getElementById('results').textContent='PASS '+results.length+' geometry checks: '+results.join('; ');}
},50);};load(20);
</script></body></html>
'@
# Prevent the embedded engine from closing the harness script element.
$engineJson = (ConvertTo-Json -InputObject $engine -Compress).Replace('</','<\/')
$contentJson = ConvertTo-Json -InputObject $paragraphs -Compress
$guardJson = ConvertTo-Json -InputObject $guard -Compress
$harness = $harness.Replace('ENGINE', $engineJson).Replace('GUARD', $guardJson).Replace('CONTENT', $contentJson)
$fixture = Join-Path $testFolder 'geometry.html'
[System.IO.File]::WriteAllText($fixture, $harness)
$browser = 'C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe'
if (!(Test-Path $browser)) { throw 'Installed Edge browser not found; no browser is downloaded.' }
$browserOutput = Join-Path $testFolder 'dom.html'
$browserError = Join-Path $testFolder 'browser.log'
$profile = Join-Path $testFolder 'profile'
$fixtureUri = ([System.Uri]$fixture).AbsoluteUri
$browserArguments = @('--headless', '--disable-gpu', '--no-first-run', '--no-default-browser-check', '--disable-background-networking', "--user-data-dir=`"$profile`"", '--dump-dom', '--virtual-time-budget=4000', "`"$fixtureUri`"")
$process = Start-Process -FilePath $browser -ArgumentList $browserArguments -WindowStyle Hidden -Wait -PassThru -RedirectStandardOutput $browserOutput -RedirectStandardError $browserError
$dom = Get-Content -Raw $browserOutput
$match = [regex]::Match($dom, '<pre id="results">([^<]*)</pre>')
if (!$match.Success -or !$match.Groups[1].Value.StartsWith('PASS')) { throw "Browser geometry tests failed: $($match.Groups[1].Value). See $testFolder" }
Write-Output $match.Groups[1].Value
