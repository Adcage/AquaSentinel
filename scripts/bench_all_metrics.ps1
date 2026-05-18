$loginBody = '{"username":"admin","password":"12345678","deviceId":"bench4","clientType":"pc","clientVersion":"1.0"}'
$resp = Invoke-RestMethod -Uri 'http://127.0.0.1:8300/api/auth/login' -Method POST -ContentType 'application/json' -Body $loginBody
$token = $resp.data.accessToken
$promData = Invoke-RestMethod -Uri 'http://127.0.0.1:8300/api/actuator/prometheus' -Headers @{Authorization="Bearer $token"}
$lines = $promData -split "`n"

Write-Host "===== Process Uptime ====="
foreach ($l in $lines) {
    if ($l -match 'process_uptime_seconds' -and $l -notmatch '#') {
        Write-Host "  $l"
    }
}

Write-Host ""
Write-Host "===== JVM Heap Summary ====="
$heapUsed = 0
$heapMax = 0
foreach ($l in $lines) {
    if ($l -match 'jvm_memory_used_bytes.*area="heap"' -and $l -notmatch '#') {
        $parts = $l -split ' '
        $val = [double]$parts[-1]
        $heapUsed += $val
    }
    if ($l -match 'jvm_memory_max_bytes.*area="heap"' -and $l -notmatch '#') {
        $parts = $l -split ' '
        $val = [double]$parts[-1]
        $heapMax += $val
    }
}
Write-Host "  Heap Used:  $([math]::Round($heapUsed/1MB, 1)) MB"
Write-Host "  Heap Max:   $([math]::Round($heapMax/1MB, 1)) MB"

Write-Host ""
Write-Host "===== Connection Pool ====="
foreach ($l in $lines) {
    if ($l -match 'hikaricp_connections' -and $l -notmatch '#') {
        Write-Host "  $l"
    }
}

Write-Host ""
Write-Host "===== WebSocket Connections ====="
foreach ($l in $lines) {
    if ($l -match 'ws_connections' -and $l -notmatch '#') {
        Write-Host "  $l"
    }
}

Write-Host ""
Write-Host "===== HTTP Server Metrics ====="
foreach ($l in $lines) {
    if ($l -match 'tomcat_threads|server_default' -and $l -notmatch '#') {
        Write-Host "  $l"
    }
}

Write-Host ""
Write-Host "===== Custom Business Metrics ====="
foreach ($l in $lines) {
    if ($l -match 'alert_events|ai_analysis|ai_embedding' -and $l -notmatch '#') {
        Write-Host "  $l"
    }
}

Write-Host ""
Write-Host "===== Frontend dist/ size ====="
$distPath = "E:\Programme\Project\AquaSentinel\frontend\dist"
if (Test-Path $distPath) {
    $totalSize = (Get-ChildItem -Recurse $distPath | Measure-Object -Property Length -Sum).Sum
    $totalKB = [math]::Round($totalSize / 1KB, 0)
    $totalMB = [math]::Round($totalSize / 1MB, 2)
    Write-Host "  Total: $totalKB KB ($totalMB MB)"
    
    $assetsPath = Join-Path $distPath "assets"
    if (Test-Path $assetsPath) {
        $jsFiles = Get-ChildItem $assetsPath -Filter "*.js" | Sort-Object Length -Descending | Select-Object -First 5
        Write-Host "  Top 5 JS chunks:"
        foreach ($f in $jsFiles) {
            $sizeKB = [math]::Round($f.Length / 1KB, 0)
            Write-Host "    $($f.Name): ${sizeKB}KB"
        }
    }
}
