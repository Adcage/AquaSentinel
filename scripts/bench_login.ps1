$results = @()
for ($i = 0; $i -lt 50; $i++) {
    $body = '{"username":"admin","password":"12345678","deviceId":"bench-rt-' + $i + '","clientType":"pc","clientVersion":"1.0"}'
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        Invoke-RestMethod -Uri 'http://127.0.0.1:8300/api/auth/login' -Method POST -ContentType 'application/json' -Body $body | Out-Null
        $sw.Stop()
        $results += $sw.ElapsedMilliseconds
    } catch {
        $sw.Stop()
    }
}

if ($results.Count -gt 0) {
    $sorted = $results | Sort-Object
    $avg = ($results | Measure-Object -Average).Average
    Write-Host "===== Login API (50 req) ====="
    Write-Host "  Count: $($results.Count)"
    Write-Host "  Min:   $($sorted[0]) ms"
    Write-Host "  Avg:   $([math]::Round($avg,1)) ms"
    Write-Host "  P50:   $($sorted[[int]($results.Count * 0.50)]) ms"
    Write-Host "  P95:   $($sorted[[int]($results.Count * 0.95)]) ms"
    Write-Host "  Max:   $($sorted[-1]) ms"
}
