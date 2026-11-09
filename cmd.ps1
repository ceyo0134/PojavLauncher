# 1794207130.19479
Get-Content C:\rvc\boot.log -ErrorAction SilentlyContinue | Select-Object -First 45 | ForEach-Object { $_.Substring(0,[Math]::Min($_.Length,170)) }
'--- py:'; Test-Path 'C:\Program Files\Python312\python.exe'; Get-Command python,py -ErrorAction SilentlyContinue | ForEach-Object { $_.Source }; python --version 2>&1
