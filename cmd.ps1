# 1794208283.6964152
Get-Content C:\rvc\ff.log | Where-Object { $_ -match 'ffmpeg at|ffmpeg version|FFDONE|rror' } | Select-Object -Last 6
Test-Path C:\Windows\System32\ffmpeg.exe
