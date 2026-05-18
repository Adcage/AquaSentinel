$loginBody = '{"username":"admin","password":"12345678","deviceId":"bench-test","clientType":"pc","clientVersion":"1.0"}'
try {
    $resp = Invoke-RestMethod -Uri 'http://127.0.0.1:8300/api/auth/login' -Method POST -ContentType 'application/json' -Body $loginBody
    $token = $resp.data.accessToken
    Write-Host "LOGIN_OK token length=$($token.Length)"
} catch {
    Write-Host "LOGIN_FAILED: $($_.Exception.Message)"
    $stream = $_.Exception.Response.GetResponseStream()
    $reader = New-Object System.IO.StreamReader($stream)
    Write-Host "Response: $($reader.ReadToEnd())"
    exit 1
}

$results = @()
for ($i = 0; $i -lt 100; $i++) {
    $body = '{"current":1,"pageSize":10}'
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        Invoke-RestMethod -Uri 'http://127.0.0.1:8300/api/alert-records/list/page/vo' -Method POST -ContentType 'application/json' -Headers @{Authorization="Bearer $token"} -Body $body | Out-Null
        $sw.Stop()
        $results += $sw.ElapsedMilliseconds
    } catch {
        $sw.Stop()
        Write-Host "  AlertRecords request $i failed"
    }
}

if ($results.Count -gt 0) {
    $sorted = $results | Sort-Object
    $avg = ($results | Measure-Object -Average).Average
    Write-Host ""
    Write-Host "===== AlertRecords Pagination (100 req) ====="
    Write-Host "  Count: $($results.Count)"
    Write-Host "  Min:   $($sorted[0]) ms"
    Write-Host "  Avg:   $([math]::Round($avg,1)) ms"
    Write-Host "  P50:   $($sorted[[int]($results.Count * 0.50)]) ms"
    Write-Host "  P95:   $($sorted[[int]($results.Count * 0.95)]) ms"
    Write-Host "  P99:   $($sorted[[int]($results.Count * 0.99)]) ms"
    Write-Host "  Max:   $($sorted[-1]) ms"
}

$results2 = @()
for ($i = 0; $i -lt 100; $i++) {
    $body = '{"current":1,"pageSize":10}'
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        Invoke-RestMethod -Uri 'http://127.0.0.1:8300/api/cameras/list/page/vo' -Method POST -ContentType 'application/json' -Headers @{Authorization="Bearer $token"} -Body $body | Out-Null
        $sw.Stop()
        $results2 += $sw.ElapsedMilliseconds
    } catch {
        $sw.Stop()
        Write-Host "  Cameras request $i failed"
    }
}

if ($results2.Count -gt 0) {
    $sorted2 = $results2 | Sort-Object
    $avg2 = ($results2 | Measure-Object -Average).Average
    Write-Host ""
    Write-Host "===== Cameras Pagination (100 req) ====="
    Write-Host "  Count: $($results2.Count)"
    Write-Host "  Min:   $($sorted2[0]) ms"
    Write-Host "  Avg:   $([math]::Round($avg2,1)) ms"
    Write-Host "  P50:   $($sorted2[[int]($results2.Count * 0.50)]) ms"
    Write-Host "  P95:   $($sorted2[[int]($results2.Count * 0.95)]) ms"
    Write-Host "  P99:   $($sorted2[[int]($results2.Count * 0.99)]) ms"
    Write-Host "  Max:   $($sorted2[-1]) ms"
}

$results3 = @()
for ($i = 0; $i -lt 100; $i++) {
    $body = '{"current":1,"pageSize":10}'
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        Invoke-RestMethod -Uri 'http://127.0.0.1:8300/api/venues/list/page/vo' -Method POST -ContentType 'application/json' -Headers @{Authorization="Bearer $token"} -Body $body | Out-Null
        $sw.Stop()
        $results3 += $sw.ElapsedMilliseconds
    } catch {
        $sw.Stop()
        Write-Host "  Venues request $i failed"
    }
}

if ($results3.Count -gt 0) {
    $sorted3 = $results3 | Sort-Object
    $avg3 = ($results3 | Measure-Object -Average).Average
    Write-Host ""
    Write-Host "===== Venues Pagination (100 req) ====="
    Write-Host "  Count: $($results3.Count)"
    Write-Host "  Min:   $($sorted3[0]) ms"
    Write-Host "  Avg:   $([math]::Round($avg3,1)) ms"
    Write-Host "  P50:   $($sorted3[[int]($results3.Count * 0.50)]) ms"
    Write-Host "  P95:   $($sorted3[[int]($results3.Count * 0.95)]) ms"
    Write-Host "  P99:   $($sorted3[[int]($results3.Count * 0.99)]) ms"
    Write-Host "  Max:   $($sorted3[-1]) ms"
}
