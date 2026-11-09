# 1794207514.9666111
'--- pip/steps'; Get-Content C:\rvc\boot.log | Where-Object { $_ -match '^STEP|ERROR|Could not|No matching|failed' } | Select-Object -First 12 | ForEach-Object { $_.Substring(0,[Math]::Min($_.Length,180)) }
'--- web.out'; Get-Content C:\rvc\web.out -Tail 12 -ErrorAction SilentlyContinue
'--- web.err'; Get-Content C:\rvc\web.err -Tail 12 -ErrorAction SilentlyContinue
'--- proc'; Get-Process python -ErrorAction SilentlyContinue | ForEach-Object { $_.Id.ToString()+' '+$_.Path }
cd C:\rvc\app; python -c "import torch,gradio;print(torch.__version__,gradio.__version__)" 2>&1 | Select-Object -Last 3
