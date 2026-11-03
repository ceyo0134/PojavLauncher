$t = "hello"
foreach ($d in @("C:\Users\Public\Desktop", "C:\Users\runneradmin\Desktop")) {
  New-Item -ItemType Directory -Path $d -Force | Out-Null
  Set-Content -Path (Join-Path $d "hello.txt") -Value $t -Encoding ascii
}
"--- whoami"; whoami
"--- sessions"; query user 2>&1
"--- files"
Get-ChildItem "C:\Users\Public\Desktop", "C:\Users\runneradmin\Desktop" -ErrorAction SilentlyContinue | Select-Object FullName
