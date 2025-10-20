Set-Location -Path (Split-Path $MyInvocation.MyCommand.Path -Parent)
Set-Location ..

Write-Host "Loading environment variables from server/.env..."
$envFile = "server/.env"
if (Test-Path $envFile) {
    Get-Content $envFile | Where-Object { $_ -notmatch '^#' } | ForEach-Object {
        $parts = $_ -split '='
        if ($parts.Count -eq 2) {
            [System.Environment]::SetEnvironmentVariable($parts[0], $parts[1])
        }
    }
} else {
    Write-Warning "server/.env not found!"
}

Write-Host "Building project..."
./gradlew.bat clean build -x test

Write-Host "Running server..."
./gradlew.bat :server:run
