$ErrorActionPreference = "Stop"

$projectRoot = "E:\Smart Maintenance Ticket System"
$dockerDesktop = "C:\Program Files\Docker\Docker\Docker Desktop.exe"

if (-not (Get-Process -Name "Docker Desktop" -ErrorAction SilentlyContinue)) {
    Start-Process -FilePath $dockerDesktop
}

Set-Location -LiteralPath $projectRoot

$maxAttempts = 24
for ($attempt = 1; $attempt -le $maxAttempts; $attempt++) {
    docker info *> $null
    if ($LASTEXITCODE -eq 0) {
        break
    }

    if ($attempt -eq $maxAttempts) {
        throw "Docker daemon did not become ready in time."
    }

    Start-Sleep -Seconds 5
}

docker compose up --build -d
docker compose ps

