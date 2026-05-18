$procs = Get-Process -Name java -ErrorAction SilentlyContinue
foreach ($p in $procs) {
    $wsMB = [math]::Round($p.WorkingSet64 / 1MB, 1)
    Write-Host "PID=$($p.Id) WorkingSet=${wsMB}MB"
}

$loginBody = '{"username":"admin","password":"12345678","deviceId":"bench2","clientType":"pc","clientVersion":"1.0"}'
$resp = Invoke-RestMethod -Uri 'http://127.0.0.1:8300/api/auth/login' -Method POST -ContentType 'application/json' -Body $loginBody
$token = $resp.data.accessToken

$promData = Invoke-RestMethod -Uri 'http://127.0.0.1:8300/api/actuator/prometheus' -Headers @{Authorization="Bearer $token"}

$jvmLines = $promData -split "`n" | Where-Object { $_ -match 'jvm_memory_used_bytes' -and $_ -notmatch '#' }
Write-Host ""
Write-Host "===== JVM Memory (from Prometheus) ====="
foreach ($line in $jvmLines) {
    $parts = $line -split ' '
    $val = [math]::Round([double]$parts[-1] / 1MB, 1)
    if ($line -match 'area="heap"') {
        Write-Host "  HEAP_USED: ${val}MB  ($line)"
    } elseif ($line -match 'area="non_heap"') {
        Write-Host "  NON_HEAP_USED: ${val}MB  ($line)"
    }
}

Write-Host ""
Write-Host "===== Key Prometheus Metrics ====="
$metrics = @(
    'jvm_memory_max_bytes{area="heap"',
    'jvm_memory_used_bytes{area="heap"',
    'jvm_threads_live_threads',
    'jvm_gc_memory_promoted_bytes_total',
    'process_cpu_usage',
    'system_cpu_usage',
    'hikaricp_connections_active',
    'hikaricp_connections_idle',
    'hikaricp_connections_pending',
    'lettuce_command_completion_latency',
    'ws_connections_active',
    'alert_events_received_total',
    'alert_events_processing_latency_seconds_count',
    'alert_events_processing_latency_seconds_sum'
)

foreach ($m in $metrics) {
    $matched = $promData -split "`n" | Where-Object { $_ -match [regex]::Escape($m) -and $_ -notmatch '#' }
    foreach ($match in $matched) {
        Write-Host "  $match"
    }
}
