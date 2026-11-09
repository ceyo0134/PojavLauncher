# 1794207610.9585907
Stop-Process -Id 7276 -Force -ErrorAction SilentlyContinue
$u=(Get-Content C:\rvc\SITE_URL.txt).Trim()
'URL: '+$u
try{ 'tunnel -> '+(Invoke-WebRequest $u -UseBasicParsing -TimeoutSec 20).StatusCode }catch{ 'tunnel fail: '+$_.Exception.Message }
try{ 'local -> '+(Invoke-WebRequest http://127.0.0.1:7865/ -UseBasicParsing -TimeoutSec 8).StatusCode }catch{ 'local fail' }
Get-ChildItem C:\rvc\app\assets\weights | ForEach-Object { 'model: '+$_.Name }
