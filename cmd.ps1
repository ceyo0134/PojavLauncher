# 1794207563.1008012
netstat -ano | findstr "LISTENING" | findstr ":786"
foreach($p in 7865,7866){ try{ "$p -> "+(Invoke-WebRequest "http://127.0.0.1:$p/" -UseBasicParsing -TimeoutSec 8).StatusCode }catch{ "$p -> fail" } }
Get-CimInstance Win32_Process -Filter "Name='python.exe'" | ForEach-Object { $_.ProcessId.ToString()+' '+$_.CommandLine.Substring(0,[Math]::Min($_.CommandLine.Length,110)) }
