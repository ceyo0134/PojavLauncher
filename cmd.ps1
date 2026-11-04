$msi = Join-Path $env:RUNNER_TEMP "crdhost.msi"
Invoke-WebRequest "https://dl.google.com/edgedl/chrome-remote-desktop/chromeremotedesktophost.msi" -OutFile $msi
(Get-Item $msi).Length
Start-Process msiexec.exe -Wait -ArgumentList "/i `"$msi`" /quiet /norestart"
Start-Sleep -Seconds 5
$exe = "C:\Program Files (x86)\Google\Chrome Remote Desktop\CurrentVersion\remoting_start_host.exe"
"host exe exists: $(Test-Path $exe)"
Get-Service | Where-Object { $_.Name -like "chromoting*" } | ForEach-Object { "$($_.Name) $($_.Status)" }
