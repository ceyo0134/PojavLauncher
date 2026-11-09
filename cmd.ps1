# 1794207473.3282225
$l=Get-Content C:\rvc\boot.log -ErrorAction SilentlyContinue | Where-Object { $_ -match '^STEP|^DONE|ERROR|error:|Successfully|Could not' }
$l | Select-Object -Last 12 | ForEach-Object { $_.Substring(0,[Math]::Min($_.Length,200)) }
if(Test-Path C:\rvc\SITE_URL.txt){'SITE: '+(Get-Content C:\rvc\SITE_URL.txt)}
if(Test-Path C:\rvc\web.err){'--- web.err'; Get-Content C:\rvc\web.err -Tail 6}
