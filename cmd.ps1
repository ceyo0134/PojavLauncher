# 1794208105.4553661
$body=@{data=@('Merhaba, bu bir deneme.','Ahmet (erkek)',0);fn_index=0}|ConvertTo-Json -Compress
try{ $r=Invoke-RestMethod -Uri http://127.0.0.1:7870/run/predict -Method Post -ContentType 'application/json' -Body $body -TimeoutSec 240; ($r|ConvertTo-Json -Depth 5 -Compress).Substring(0,[Math]::Min(700,($r|ConvertTo-Json -Depth 5 -Compress).Length)) }catch{ 'ERR: '+$_.Exception.Message }
