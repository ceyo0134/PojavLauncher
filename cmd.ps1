# 1794208325.569537
$real=Get-ChildItem 'C:\ProgramData\chocolatey\lib\ffmpeg' -Recurse -Filter ffmpeg.exe -ErrorAction SilentlyContinue | Select-Object -First 1
'real: '+$real.FullName+' '+$real.Length
if($real){ Copy-Item $real.FullName C:\Windows\System32\ffmpeg.exe -Force; $pr=Join-Path $real.DirectoryName 'ffprobe.exe'; if(Test-Path $pr){Copy-Item $pr C:\Windows\System32\ffprobe.exe -Force} }
(Get-Item C:\Windows\System32\ffmpeg.exe).Length
(& C:\Windows\System32\ffmpeg.exe -version 2>&1 | Select-Object -First 1)
