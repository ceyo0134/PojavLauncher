# 1794208061.685865
Get-Content C:\rvc\tts_boot.log -ErrorAction SilentlyContinue | Where-Object { $_ -match '^STEP|^UP|^DONE|GH_REPO|rror|fatal' } | Select-Object -Last 10
if(Test-Path C:\rvc\TTS_URL.txt){'TTS_URL: '+(Get-Content C:\rvc\TTS_URL.txt)}
'--- tts.err'; Get-Content C:\rvc\tts.err -Tail 5 -ErrorAction SilentlyContinue | ForEach-Object { $_.Substring(0,[Math]::Min($_.Length,160)) }
