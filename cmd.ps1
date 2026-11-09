# 1794207940.779846
@'

$ProgressPreference='SilentlyContinue'
$d='C:\rvc'; Set-Location "$d\app"
Start-Transcript -Path "$d\tts_boot.log" | Out-Null
"STEP pull"; git pull -q 2>&1 | Out-String
"STEP pip"; python -m pip install -q edge-tts 2>&1 | Out-String
"STEP start"
$env:PORCAY_BRANCH='porcay-outputs'
"GH_REPO=$($env:GH_REPO) token_set=$([bool]$env:GH_TOKEN)"
Start-Process python -ArgumentList 'porcay_tts.py' -WorkingDirectory "$d\app" -RedirectStandardOutput "$d\tts.out" -RedirectStandardError "$d\tts.err" -WindowStyle Hidden
for($i=0;$i -lt 80;$i++){ Start-Sleep 5; try{ if((Invoke-WebRequest http://127.0.0.1:7870/ -UseBasicParsing -TimeoutSec 5).StatusCode -eq 200){ "UP"; break } }catch{} }
"STEP tunnel"
Remove-Item "$d\TTS_URL.txt" -ErrorAction SilentlyContinue
Start-Process "$d\cf.exe" -ArgumentList 'tunnel --url http://127.0.0.1:7870 --no-autoupdate --protocol http2' -RedirectStandardError "$d\cf2.err" -RedirectStandardOutput "$d\cf2.out" -WindowStyle Hidden
for($i=0;$i -lt 30;$i++){ Start-Sleep 3; $m=Select-String -Path "$d\cf2.err" -Pattern 'https://[a-z0-9-]+\.trycloudflare\.com' -ErrorAction SilentlyContinue | Select-Object -First 1; if($m){ $m.Matches[0].Value | Set-Content "$d\TTS_URL.txt"; break } }
"DONE"
'@ | Set-Content C:\tts_boot.ps1 -Encoding utf8
Start-Process pwsh -ArgumentList '-NoProfile -ExecutionPolicy Bypass -File C:\tts_boot.ps1' -WindowStyle Hidden
'tts bootstrap started'
