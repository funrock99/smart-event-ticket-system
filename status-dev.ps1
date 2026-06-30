$ErrorActionPreference = "Stop"

$projectRoot = "E:\Smart Maintenance Ticket System"

Set-Location -LiteralPath $projectRoot

Write-Host "== Docker =="
docker info --format "{{.ServerVersion}}"

Write-Host ""
Write-Host "== Compose =="
docker compose ps
