# Start planner-app (8080) and frontend dev server (5173).
# Usage from repo root: .\scripts\dev.ps1

$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

$backend = $null
$backendLog = Join-Path $Root "scripts\.backend.log"
$backendErrLog = Join-Path $Root "scripts\.backend.err.log"
$gradlew = Join-Path $Root "gradlew.bat"

function Stop-Backend {
  if ($null -ne $backend -and -not $backend.HasExited) {
    Write-Host ""
    Write-Host "Stopping backend (PID $($backend.Id))..."
    Stop-Process -Id $backend.Id -Force -ErrorAction SilentlyContinue
    Get-CimInstance Win32_Process -ErrorAction SilentlyContinue |
      Where-Object { $_.ParentProcessId -eq $backend.Id } |
      ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }
  }
}

function Wait-ForBackend {
  $url = "http://localhost:8080/actuator/health"
  Write-Host "Waiting for backend at $url ..."

  for ($attempt = 1; $attempt -le 45; $attempt++) {
    if ($null -ne $backend -and $backend.HasExited) {
      throw "Backend process exited early (code $($backend.ExitCode)). Check $backendLog and $backendErrLog"
    }

    try {
      $response = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 2
      if ($response.StatusCode -eq 200) {
        Write-Host "Backend is ready."
        return
      }
    } catch {
      Start-Sleep -Seconds 2
    }
  }

  throw "Backend did not become ready within 90 seconds. Check $backendLog and $backendErrLog"
}

try {
  if (-not (Test-Path $gradlew)) {
    throw "gradlew.bat not found at $gradlew"
  }

  Write-Host "Starting planner-app on http://localhost:8080 ..."
  Remove-Item $backendLog, $backendErrLog -ErrorAction SilentlyContinue

  $backend = Start-Process `
    -FilePath $gradlew `
    -ArgumentList ":planner-app:bootRun" `
    -PassThru `
    -NoNewWindow `
    -WorkingDirectory $Root `
    -RedirectStandardOutput $backendLog `
    -RedirectStandardError $backendErrLog

  Wait-ForBackend

  Set-Location "$Root\frontend"
  if (-not (Test-Path "node_modules")) {
    Write-Host "Installing frontend dependencies..."
    npm install
  }

  Write-Host "Starting frontend on http://localhost:5173 ..."
  Write-Host "Press Ctrl+C to stop both servers."
  npm run dev
} finally {
  Stop-Backend
}
