"--- tailscale status"
& "C:\Program Files\Tailscale\tailscale.exe" status
"--- ping phone path (netcheck)"
& "C:\Program Files\Tailscale\tailscale.exe" netcheck 2>&1 | Select-Object -First 12
"--- vnc connections"
netstat -ano | findstr ":5900"
"--- top cpu (5s sample)"
$a = Get-Process | Select-Object Id,ProcessName,CPU
Start-Sleep 5
$b = Get-Process | Select-Object Id,ProcessName,CPU
foreach ($p in $b) { $o = $a | ? { $_.Id -eq $p.Id } | select -First 1; if ($o -and $p.CPU -ne $null -and $o.CPU -ne $null) { [pscustomobject]@{N=$p.ProcessName; Pct=[math]::Round(($p.CPU-$o.CPU)/5*100,0)} } } | sort Pct -desc | select -First 8 | ft -auto | Out-String
"--- tvnserver reg"
Get-ItemProperty "HKLM:\SOFTWARE\TightVNC\Server" | select PollingInterval,GrabTransparentWindows,RemoveWallpaper,UseD3D | fl | Out-String
