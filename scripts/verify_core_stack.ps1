<#
.SYNOPSIS
    AquaSentinel 核心链路健康检查与联通验证脚本
.DESCRIPTION
    依次检查 Backend(8300)、YOLO(5000)、video-hub(5100) 的健康状态，
    以及 Redis(6379) 和 MySQL(3306) 的连通性。
    全部通过返回退出码 0，有失败返回 1。
#>

$ErrorActionPreference = "Continue"

$passCount = 0
$failCount = 0

function Write-Result {
    param(
        [string]$Name,
        [bool]$Ok,
        [string]$Detail = ""
    )
    if ($Ok) {
        Write-Host ("[OK]   {0}" -f $Name) -ForegroundColor Green
        $script:passCount++
    }
    else {
        $msg = if ($Detail) { " — $Detail" } else { "" }
        Write-Host ("[FAIL] {0}{1}" -f $Name, $msg) -ForegroundColor Red
        $script:failCount++
    }
}

# ---------- HTTP 健康检查 ----------
function Test-HttpHealth {
    param(
        [string]$Name,
        [string]$Url
    )
    try {
        $resp = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 5 -ErrorAction Stop
        if ($resp.StatusCode -ge 200 -and $resp.StatusCode -lt 300) {
            Write-Result -Name $Name -Ok $true
        }
        else {
            Write-Result -Name $Name -Ok $false -Detail ("HTTP $($resp.StatusCode)")
        }
    }
    catch {
        $errMsg = if ($_.Exception.Message) { $_.Exception.Message } else { "请求失败" }
        Write-Result -Name $Name -Ok $false -Detail $errMsg
    }
}

# ---------- TCP 端口连通检查 ----------
function Test-TcpPort {
    param(
        [string]$Name,
        [string]$Host_,
        [int]$Port
    )
    try {
        $tcpClient = New-Object System.Net.Sockets.TcpClient
        $iar = $tcpClient.BeginConnect($Host_, $Port, $null, $null)
        $waited = $iar.AsyncWaitHandle.WaitOne(5000, $false)
        if ($waited -and $tcpClient.Connected) {
            $tcpClient.EndConnect($iar)
            Write-Result -Name $Name -Ok $true
        }
        else {
            Write-Result -Name $Name -Ok $false -Detail "连接超时"
        }
        $tcpClient.Close()
    }
    catch {
        Write-Result -Name $Name -Ok $false -Detail $_.Exception.Message
    }
}

# ============================================================
Write-Host ""
Write-Host "===== AquaSentinel 核心链路验证 =====" -ForegroundColor Cyan
Write-Host ""

# 1. Backend (Spring Boot Actuator)
Test-HttpHealth -Name "Backend  (8300)" -Url "http://127.0.0.1:8300/api/actuator/health"

# 2. YOLO Service
Test-HttpHealth -Name "YOLO     (5000)" -Url "http://127.0.0.1:5000/health"

# 3. video-hub-service
Test-HttpHealth -Name "video-hub(5100)" -Url "http://127.0.0.1:5100/health"

# 4. Redis
Test-TcpPort -Name "Redis    (6379)" -Host_ "127.0.0.1" -Port 6379

# 5. MySQL
Test-TcpPort -Name "MySQL    (3306)" -Host_ "127.0.0.1" -Port 3306

# ============================================================
Write-Host ""
Write-Host ("----- 汇总: 通过 {0}, 失败 {1} -----" -f $passCount, $failCount) -ForegroundColor Cyan
Write-Host ""

if ($failCount -gt 0) {
    exit 1
}
exit 0
