"--- tailscale status"
& "C:\Program Files\Tailscale\tailscale.exe" status 2>&1 | Out-String
"--- netcheck"
& "C:\Program Files\Tailscale\tailscale.exe" netcheck 2>&1 | Select-Object -First 12 | Out-String
"--- vnc connections"
netstat -ano | findstr ":5900" | Out-String
"--- top cpu 5s"
$a = Get-Process | Select-Object Id,ProcessName,CPU
Start-Sleep 5
$b = Get-Process | Select-Object Id,ProcessName,CPU
$rows = $b | ForEach-Object { $p=$_; $o = $a | Where-Object { $_.Id -eq $p.Id } | Select-Object -First 1; if ($o -and $p.CPU -ne $null -and $o.CPU -ne $null) { [pscustomobject]@{N=$p.ProcessName; Pct=[math]::Round(($p.CPU-$o.CPU)/5*100,0)} } }
($rows | Sort-Object Pct -Descending | Select-Object -First 8 | ForEach-Object { "$($_.N)=$($_.Pct)%" }) -join ", "
"--- tvnserver reg"
(Get-ItemProperty "HKLM:\SOFTWARE\TightVNC\Server" | Out-String)
