# 1794207083.9153402
Get-Content C:\rvc\boot.log -Tail 12 -ErrorAction SilentlyContinue | Select-String 'STEP|DONE|ERROR|error' | ForEach-Object {$_.Line}
if(Test-Path C:\rvc\SITE_URL.txt){'SITE: '+(Get-Content C:\rvc\SITE_URL.txt)}
'procs: '+((Get-Process python,pip -ErrorAction SilentlyContinue).Count)
