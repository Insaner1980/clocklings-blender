#Requires -Version 5.1
# Runs the real wrapper against local stand-ins; never starts an emulator or uploads code.
$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$testRoot = Join-Path $repoRoot ('reports\sonar-script-tests\' + [guid]::NewGuid().ToString('N'))
$fixtureTools = New-Item -ItemType Directory -Force (Join-Path $testRoot 'tools')
$fakeBin = New-Item -ItemType Directory -Force (Join-Path $testRoot 'bin')
Copy-Item (Join-Path $PSScriptRoot 'sonar.ps1') $fixtureTools.FullName
Set-Content (Join-Path $testRoot 'sonar-project.properties') @(
    'sonar.projectKey=wrapper-test'
    'sonar.host.url=https://example.invalid'
)
foreach ($command in @('adb', 'android')) {
    Set-Content (Join-Path $fakeBin.FullName "$command.cmd") @'
@echo off
echo Unexpected device command >> "%~dp0device-command-called.txt"
exit /b 99
'@
}
Set-Content (Join-Path $testRoot 'gradlew.bat') @'
@echo off
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0fake-gradle.ps1"
exit /b %errorlevel%
'@
Set-Content (Join-Path $testRoot 'fake-gradle.ps1') @'
[Console]::WriteLine('LIVE_STDOUT')
[Console]::Error.WriteLine('LIVE_STDERR')
Set-Content (Join-Path $PSScriptRoot 'child.pid') $PID
Start-Sleep -Seconds 5
Set-Content (Join-Path $PSScriptRoot 'child-completed.txt') 'done'
if ($env:SONAR_TEST_MODE -eq 'failure') { exit 7 }
if ($env:SONAR_TEST_MODE -ne 'missing-receipt') {
    New-Item -ItemType Directory -Force (Join-Path $PSScriptRoot 'build\sonar') | Out-Null
    Set-Content (Join-Path $PSScriptRoot 'build\sonar\report-task.txt') @(
        'projectKey=wrapper-test'
        'ceTaskId=test-task'
        'dashboardUrl=https://example.invalid/dashboard?id=wrapper-test'
    )
}
exit 0
'@
$previousPath = $env:PATH
$previousToken = $env:SONAR_TOKEN
$previousSerial = $env:ANDROID_SERIAL
$previousMode = $env:SONAR_TEST_MODE
try {
    $env:PATH = $fakeBin.FullName + ';' + $env:PATH
    $env:SONAR_TOKEN = 'local-test-placeholder'
    $env:ANDROID_SERIAL = 'unrelated-device'
    foreach ($mode in @('success', 'failure', 'timeout', 'missing-receipt')) {
        $env:SONAR_TEST_MODE = $mode
        $completedPath = Join-Path $testRoot 'child-completed.txt'
        Remove-Item -LiteralPath $completedPath -ErrorAction SilentlyContinue
        $timeout = if ($mode -eq 'timeout') { 2 } else { 30 }
        $stdout = Join-Path $testRoot "$mode.stdout.txt"
        $stderr = Join-Path $testRoot "$mode.stderr.txt"
        $process = Start-Process -FilePath (Get-Process -Id $PID).Path -WindowStyle Hidden -PassThru `
            -ArgumentList @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File',
                ('"' + (Join-Path $fixtureTools.FullName 'sonar.ps1') + '"'),
                '-AllowExternalUpload', '-GradleTimeoutSeconds', $timeout) `
            -RedirectStandardOutput $stdout -RedirectStandardError $stderr
        # Retain the handle so Windows PowerShell 5.1 can read ExitCode after exit.
        $null = $process.Handle
        try {
            $watch = [Diagnostics.Stopwatch]::StartNew()
            $liveOutput = $false
            while (-not $process.HasExited -and $watch.Elapsed.TotalSeconds -lt 40) {
                $report = Join-Path $testRoot 'reports\sonar.txt'
                if ((Test-Path $report) -and -not (Test-Path $completedPath)) {
                    $log = Get-Content -Raw $report
                    $console = Get-Content -Raw $stdout
                    if ($log -match 'LIVE_STDOUT' -and $log -match 'LIVE_STDERR' -and
                        $console -match 'LIVE_STDOUT' -and $console -match 'LIVE_STDERR') {
                        $liveOutput = $true
                    }
                }
                Start-Sleep -Milliseconds 100
            }
            if (-not $process.HasExited) { throw "$mode did not finish within 40 seconds" }
            $process.WaitForExit()
            $console = Get-Content -Raw $stdout
            if (Test-Path (Join-Path $fakeBin.FullName 'device-command-called.txt')) {
                throw 'Sonar must not invoke adb or android'
            }
            if (-not $liveOutput) { throw "$mode did not stream stdout/stderr before completion" }
            if ($mode -eq 'success') {
                if ($process.ExitCode -ne 0 -or $console -notmatch 'SONAR_ANALYSIS_UPLOAD_COMPLETED') {
                    throw 'Success was not reported to the console'
                }
            } else {
                if ($process.ExitCode -ne 2 -or $console -notmatch 'ERROR: SONAR_') {
                    throw "$mode did not report a visible failure with exit code 2"
                }
                if ($console -match 'SONAR_ANALYSIS_UPLOAD_COMPLETED') { throw "$mode claimed success" }
            }
            if ($mode -eq 'timeout') {
                $childId = [int](Get-Content (Join-Path $testRoot 'child.pid'))
                if (Get-Process -Id $childId -ErrorAction SilentlyContinue) { throw 'Timed-out child still running' }
            }
            Write-Output "PASS: $mode"
        } finally {
            if (-not $process.HasExited) { & taskkill.exe /PID $process.Id /T /F | Out-Null }
            $process.Dispose()
        }
    }
} finally {
    $env:PATH = $previousPath
    $env:SONAR_TOKEN = $previousToken
    $env:ANDROID_SERIAL = $previousSerial
    $env:SONAR_TEST_MODE = $previousMode
}
Write-Output "Evidence: $testRoot"
