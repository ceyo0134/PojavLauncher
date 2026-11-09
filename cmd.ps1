# 1794207279.4574397
$l=Get-Content C:\rvc\boot.log -ErrorAction SilentlyContinue | Where-Object { $_ -notmatch 'TerminatingError' -and $_ -notmatch '^\*|^Start time|^Username|^RunAs|^Configuration|^Machine|^Host|^Process ID|^PS|^OS:|^Platform|^Serial|^WSMan|^GitCommit|^End time' }
$l | Select-Object -Last 40 | ForEach-Object { $_.Substring(0,[Math]::Min($_.Length,200)) }
Get-ChildItem C:\rvc\app\assets\rmvpe,C:\rvc\app\assets\hubert_base -ErrorAction SilentlyContinue | ForEach-Object { $_.Name+' '+$_.Length }
if(Test-Path C:\rvc\web.err){'--- web.err'; Get-Content C:\rvc\web.err -Tail 8}
