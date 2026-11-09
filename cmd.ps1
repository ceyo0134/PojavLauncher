# 1794208192.7390137
@'

Start-Transcript -Path C:\rvc\ff.log | Out-Null
choco install ffmpeg -y --no-progress 2>&1 | Select-Object -Last 5
$f=(Get-Command ffmpeg -ErrorAction SilentlyContinue).Source
if(-not $f){ $f=(Get-ChildItem 'C:\ProgramData\chocolatey' -Recurse -Filter ffmpeg.exe -ErrorAction SilentlyContinue | Select-Object -First 1).FullName }
"ffmpeg at: $f"
if($f){ Copy-Item $f C:\Windows\System32\ffmpeg.exe -Force; $p=Join-Path (Split-Path $f) 'ffprobe.exe'; if(Test-Path $p){ Copy-Item $p C:\Windows\System32\ffprobe.exe -Force } }
& C:\Windows\System32\ffmpeg.exe -version 2>&1 | Select-Object -First 1
"FFDONE"
'@ | Set-Content C:\ff_boot.ps1 -Encoding utf8
Start-Process pwsh -ArgumentList '-NoProfile -ExecutionPolicy Bypass -File C:\ff_boot.ps1' -WindowStyle Hidden
'ffmpeg install started'
