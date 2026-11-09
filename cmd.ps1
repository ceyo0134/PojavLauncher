@'
$ProgressPreference='SilentlyContinue'
$d='C:\rvc'; New-Item -ItemType Directory -Force $d | Out-Null; Set-Location $d
Start-Transcript -Path "$d\boot.log" -Append | Out-Null
"STEP python"
$py='C:\Program Files\Python312\python.exe'
if(-not (Test-Path $py)){ curl.exe -sL -o py.exe https://www.python.org/ftp/python/3.12.7/python-3.12.7-amd64.exe; Start-Process .\py.exe -Wait -ArgumentList '/quiet InstallAllUsers=1 PrependPath=0 Include_test=0' }
"STEP clone"
if(-not (Test-Path "$d\app")){ git clone --depth 1 https://github.com/ceyo0134/porcay-voice-web.git app }
Set-Location "$d\app"
"STEP venv"
& $py -m venv "$d\venv"
$vp="$d\venv\Scripts\python.exe"
& $vp -m pip install -q --upgrade pip setuptools wheel
"STEP pip"
(Get-Content requirments_codespaces.txt | Where-Object { $_ -notmatch 'directml' }) | Set-Content req_win.txt
& $vp -m pip install -q -r req_win.txt
"STEP models"
$rel='https://github.com/ceyo0134/porcay-voice-web/releases/download/models-v1'
New-Item -ItemType Directory -Force assets\rmvpe | Out-Null
if(-not (Test-Path assets\rmvpe\rmvpe.pt)){ curl.exe -sL -o assets\rmvpe\rmvpe.pt "$rel/rmvpe.pt" }
if(-not (Test-Path assets\hubert_base\pytorch_model.bin)){ curl.exe -sL -o "$d\h.zip" "$rel/hubert_base.zip"; Expand-Archive "$d\h.zip" -DestinationPath assets -Force }
"STEP webui"
Start-Process $vp -ArgumentList 'webui.py --noautoopen --port 7865' -WorkingDirectory "$d\app" -RedirectStandardOutput "$d\web.out" -RedirectStandardError "$d\web.err" -WindowStyle Hidden
for($i=0;$i -lt 60;$i++){ Start-Sleep 5; try{ if((Invoke-WebRequest http://127.0.0.1:7865/ -UseBasicParsing -TimeoutSec 5).StatusCode -eq 200){break} }catch{} }
"STEP tunnel"
curl.exe -sL -o "$d\cf.exe" https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-windows-amd64.exe
Start-Process "$d\cf.exe" -ArgumentList 'tunnel --url http://127.0.0.1:7865 --no-autoupdate --protocol http2' -RedirectStandardError "$d\cf.err" -RedirectStandardOutput "$d\cf.out" -WindowStyle Hidden
for($i=0;$i -lt 30;$i++){ Start-Sleep 3; $m=Select-String -Path "$d\cf.err" -Pattern 'https://[a-z0-9-]+\.trycloudflare\.com' -ErrorAction SilentlyContinue | Select-Object -First 1; if($m){ $m.Matches[0].Value | Set-Content "$d\SITE_URL.txt"; break } }
"DONE"
'@ | Set-Content C:\rvc_boot.ps1 -Encoding utf8
Start-Process pwsh -ArgumentList '-NoProfile -ExecutionPolicy Bypass -File C:\rvc_boot.ps1' -WindowStyle Hidden
'bootstrap started'
