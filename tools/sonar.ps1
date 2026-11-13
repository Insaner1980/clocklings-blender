#Requires -Version 5.1

[CmdletBinding()]
param(
    [switch]$PlanOnly,

    [switch]$AllowExternalUpload,

    [ValidateRange(1, 86400)]
    [int]$GradleTimeoutSeconds = 3600
)

$ErrorActionPreference = "Continue"
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new()
$OutputEncoding = [Console]::OutputEncoding

function Write-SonarMessage {
    param([string[]]$Text)

    Add-Content -LiteralPath $reportPath -Encoding utf8 -Value $Text
    foreach ($line in $Text) { Write-Host $line }
}

function Invoke-SonarGradle {
    # Read both pipes concurrently so Gradle progress is visible without filling either pipe.
    $startInfo = [Diagnostics.ProcessStartInfo]::new()
    $startInfo.FileName = $env:ComSpec
    $startInfo.Arguments = '/d /s /c ".\gradlew.bat sonar --console=plain --no-daemon -Dsonar.qualitygate.wait=true"'
    $startInfo.WorkingDirectory = $repoRoot
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    $process = [Diagnostics.Process]::new()
    $process.StartInfo = $startInfo
    $started = $false
    try {
        $started = $process.Start()
        if (-not $started) { throw 'Could not start Gradle.' }
        $streams = @($process.StandardOutput, $process.StandardError)
        $pending = @($streams[0].ReadLineAsync(), $streams[1].ReadLineAsync())
        $watch = [Diagnostics.Stopwatch]::StartNew()
        $nextProgressSeconds = 30
        while (-not $process.HasExited -or $null -ne $pending[0] -or $null -ne $pending[1]) {
            for ($index = 0; $index -lt 2; $index++) {
                if ($null -ne $pending[$index] -and $pending[$index].IsCompleted) {
                    $line = $pending[$index].GetAwaiter().GetResult()
                    if ($null -eq $line) {
                        $pending[$index] = $null
                    } else {
                        Write-SonarMessage $line
                        $pending[$index] = $streams[$index].ReadLineAsync()
                    }
                }
            }
            if ($watch.Elapsed.TotalSeconds -ge $GradleTimeoutSeconds) {
                return [pscustomobject]@{ TimedOut = $true; ExitCode = 2 }
            }
            if ($watch.Elapsed.TotalSeconds -ge $nextProgressSeconds) {
                Write-SonarMessage "Sonar run still active ($([int]$watch.Elapsed.TotalSeconds) s elapsed; limit $GradleTimeoutSeconds s)."
                $nextProgressSeconds += 30
            }
            Start-Sleep -Milliseconds 10
        }
        return [pscustomobject]@{ TimedOut = $false; ExitCode = $process.ExitCode }
    }
    finally {
        try {
            if ($started -and -not $process.HasExited) {
                # Only stop this invocation and its descendants, including on Ctrl+C.
                & taskkill.exe /PID $process.Id /T /F | Out-Null
                [void]$process.WaitForExit(10000)
            }
        }
        finally { $process.Dispose() }
    }
}

function Get-SonarProjectProperties {
    param([string]$Path)

    $properties = @{}
    foreach ($line in Get-Content -LiteralPath $Path -Encoding utf8) {
        $trimmed = $line.Trim()
        if ([string]::IsNullOrWhiteSpace($trimmed) -or $trimmed.StartsWith("#")) {
            continue
        }

        $separator = $trimmed.IndexOf("=")
        if ($separator -gt 0) {
            $properties[$trimmed.Substring(0, $separator).Trim()] = $trimmed.Substring($separator + 1).Trim()
        }
    }

    return $properties
}

function Test-SonarTokenConfigured {
    param([string]$RepoRoot)

    if (-not [string]::IsNullOrWhiteSpace($env:SONAR_TOKEN)) {
        return $true
    }

    foreach ($path in @(
        (Join-Path $env:USERPROFILE ".gradle\gradle.properties"),
        (Join-Path $RepoRoot "gradle.properties")
    )) {
        if (
            (Test-Path -LiteralPath $path -PathType Leaf) -and
            (Select-String -LiteralPath $path -Pattern "^\s*systemProp\.sonar\.token\s*=" -Quiet)
        ) {
            return $true
        }
    }

    return $false
}

$repoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..")).Path
$propertiesPath = Join-Path $repoRoot "sonar-project.properties"
if (-not (Test-Path -LiteralPath $propertiesPath -PathType Leaf)) {
    throw "sonar-project.properties ei loytynyt: $propertiesPath"
}

$sonarProperties = Get-SonarProjectProperties -Path $propertiesPath
$projectKey = [string]$sonarProperties["sonar.projectKey"]
$hostUrl = [string]$sonarProperties["sonar.host.url"]
if ([string]::IsNullOrWhiteSpace($projectKey)) {
    throw "sonar.projectKey puuttuu sonar-project.properties-tiedostosta."
}
if ([string]::IsNullOrWhiteSpace($hostUrl)) {
    $hostUrl = "https://sonarcloud.io"
}

if ($PlanOnly) {
    Write-Output @(
        "sonar"
        "  - Gradle debug build, JVM unit-test coverage, and SonarCloud upload: reports/sonar.txt"
        "  - no emulator or connected device required; instrumentation tests run separately"
        "  - requires SONAR_TOKEN or systemProp.sonar.token"
        "  - actual external upload requires -AllowExternalUpload"
        "  - streams progress and waits for the server's Quality Gate result"
        "  - project: $projectKey"
        "  - host: $hostUrl"
    )
    exit 0
}

$reportsDirectory = Join-Path $repoRoot "reports"
$reportPath = Join-Path $reportsDirectory "sonar.txt"
$startedAt = [DateTime]::UtcNow
New-Item -ItemType Directory -Force -Path $reportsDirectory | Out-Null
Set-Content -LiteralPath $reportPath -Encoding utf8 -Value @(
    "sonar"
    "Root: $repoRoot"
    "Project: $projectKey"
    "Command: .\gradlew.bat sonar --console=plain --no-daemon -Dsonar.qualitygate.wait=true"
    "Started: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
    ""
)

if (-not $AllowExternalUpload) {
    Add-Content -LiteralPath $reportPath -Encoding utf8 -Value @(
        "ERROR: EXTERNAL_UPLOAD_APPROVAL_REQUIRED"
        "Sonar-analyysi voi lahettaa lahdekoodia ja analyysimetatietoa ulkoiseen palveluun."
        "Kayta -AllowExternalUpload-valitsinta vain, kun ulkoinen lahetys on sallittu."
    )
    Get-Content -LiteralPath $reportPath
    exit 2
}

if (-not (Test-SonarTokenConfigured -RepoRoot $repoRoot)) {
    Add-Content -LiteralPath $reportPath -Encoding utf8 -Value "ERROR: SONAR_TOKEN_MISSING"
    Get-Content -LiteralPath $reportPath
    exit 2
}

$previousSonarHost = $env:SONAR_HOST_URL
Push-Location -LiteralPath $repoRoot
try {
    $env:SONAR_HOST_URL = if ($env:SONAR_HOST_URL) { $env:SONAR_HOST_URL } else { $hostUrl }

    try {
        Write-SonarMessage @(
            "Building, running JVM unit tests, uploading analysis and waiting for Quality Gate (timeout: $GradleTimeoutSeconds s)..."
            "Live report: $reportPath"
        )
        $result = Invoke-SonarGradle
    }
    catch {
        $message = "ERROR: SONAR_ANALYSIS_PROCESS_ERROR: $($_.Exception.Message)"
        Write-SonarMessage $message
        exit 2
    }

    if ($result.TimedOut) {
        Write-SonarMessage "ERROR: SONAR_ANALYSIS_TIMEOUT ($GradleTimeoutSeconds s). Analysis completion was not confirmed. See $reportPath"
        exit 2
    }
    if ($result.ExitCode -ne 0) {
        Write-SonarMessage "ERROR: SONAR_ANALYSIS_FAILED (exit $($result.ExitCode)). See $reportPath"
        exit 2
    }

    $receiptPath = Join-Path $repoRoot 'build\sonar\report-task.txt'
    $receipt = Get-Item -LiteralPath $receiptPath -ErrorAction SilentlyContinue
    if ($null -eq $receipt -or $receipt.LastWriteTimeUtc -lt $startedAt) {
        Write-SonarMessage 'ERROR: SONAR_UPLOAD_RECEIPT_MISSING. No fresh upload receipt; completion was not confirmed.'
        exit 2
    }
    $task = Get-SonarProjectProperties -Path $receiptPath
    if ($task['projectKey'] -ne $projectKey -or [string]::IsNullOrWhiteSpace($task['ceTaskId'])) {
        Write-SonarMessage 'ERROR: SONAR_UPLOAD_RECEIPT_INVALID. No matching project/task in the upload receipt.'
        exit 2
    }
    Write-SonarMessage @(
        "SONAR_ANALYSIS_UPLOAD_COMPLETED"
        "Quality Gate passed. Task: $($task['ceTaskId'])"
        "Dashboard: $($task['dashboardUrl'])"
        "Completed: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
        "The JaCoCo import contains JVM unit-test coverage."
    )
    exit 0
}
finally {
    $env:SONAR_HOST_URL = $previousSonarHost
    Pop-Location
}
