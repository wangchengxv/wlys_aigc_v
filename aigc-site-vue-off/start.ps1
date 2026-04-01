$ErrorActionPreference = "Stop"

$RootDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$FrontendDir = Join-Path $RootDir "aigc-site"
$BackendDir = Join-Path $RootDir "aigc-server"
$LogDir = Join-Path $RootDir ".logs"
$PidFile = Join-Path $LogDir "backend.pid"
$BackendLog = Join-Path $LogDir "backend.log"
$BackendProcess = $null

function Test-CommandExists {
    param([string]$Name)
    return [bool](Get-Command $Name -ErrorAction SilentlyContinue)
}

function Assert-Directories {
    if (-not (Test-Path $FrontendDir)) {
        throw "未找到前端目录: $FrontendDir"
    }
    if (-not (Test-Path $BackendDir)) {
        throw "未找到后端目录: $BackendDir"
    }
}

function Wait-BackendReady {
    $retries = 40
    for ($i = 1; $i -le $retries; $i++) {
        try {
            Invoke-WebRequest -Uri "http://localhost:8080/api/v1/health" -UseBasicParsing | Out-Null
            Write-Host "[后端] 健康检查通过: http://localhost:8080/api/v1/health"
            return
        } catch {
            Start-Sleep -Seconds 1
        }
    }
    Write-Warning "后端启动超时，请查看日志: $BackendLog"
}

function Prepare-FrontendEnv {
    $envFile = Join-Path $FrontendDir ".env"
    $exampleFile = Join-Path $FrontendDir ".env.example"
    if (-not (Test-Path $envFile)) {
        if (Test-Path $exampleFile) {
            Copy-Item $exampleFile $envFile
            Write-Host "[前端] 已创建 .env 文件"
        } else {
            Set-Content -Path $envFile -Value "VITE_API_BASE_URL=http://localhost:8080"
            Write-Host "[前端] 已创建默认 .env 文件"
        }
    }
}

function Install-FrontendDeps {
    $nodeModules = Join-Path $FrontendDir "node_modules"
    if (-not (Test-Path $nodeModules)) {
        Write-Host "[前端] 未检测到 node_modules，正在安装依赖..."
        Push-Location $FrontendDir
        try {
            npm install
        } finally {
            Pop-Location
        }
    }
}

function Start-Backend {
    New-Item -ItemType Directory -Path $LogDir -Force | Out-Null

    if (Test-Path $PidFile) {
        $oldPid = (Get-Content $PidFile -ErrorAction SilentlyContinue | Select-Object -First 1)
        if ($oldPid) {
            $oldProcess = Get-Process -Id $oldPid -ErrorAction SilentlyContinue
            if ($oldProcess) {
                Write-Host "[后端] 已在运行 (PID: $oldPid)"
                return
            }
        }
        Remove-Item $PidFile -Force -ErrorAction SilentlyContinue
    }

    Write-Host "[后端] 启动中..."
    $BackendProcess = Start-Process -FilePath "mvn" -ArgumentList "spring-boot:run" -WorkingDirectory $BackendDir -PassThru -RedirectStandardOutput $BackendLog -RedirectStandardError $BackendLog
    Set-Content -Path $PidFile -Value $BackendProcess.Id
    Write-Host "[后端] 已启动 (PID: $($BackendProcess.Id))"
    Wait-BackendReady
}

function Stop-BackendIfStarted {
    if ($BackendProcess -and -not $BackendProcess.HasExited) {
        Write-Host ""
        Write-Host "[清理] 正在关闭本次启动的后端服务..."
        Stop-Process -Id $BackendProcess.Id -Force -ErrorAction SilentlyContinue
    }
    if (Test-Path $PidFile) {
        Remove-Item $PidFile -Force -ErrorAction SilentlyContinue
    }
}

Assert-Directories

if (-not (Test-CommandExists "mvn")) {
    throw "未检测到 mvn，请先安装 Maven。"
}
if (-not (Test-CommandExists "npm")) {
    throw "未检测到 npm，请先安装 Node.js/NPM。"
}

Start-Backend
Prepare-FrontendEnv
Install-FrontendDeps

Write-Host "[前端] 启动中..."
Write-Host "[访问地址] http://localhost:5173"

try {
    Push-Location $FrontendDir
    npm run dev
} finally {
    Pop-Location
    Stop-BackendIfStarted
}
