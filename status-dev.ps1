$ErrorActionPreference = "Stop"

$projectRoot = "E:\\smart event ticket system"

Set-Location -LiteralPath $projectRoot

Write-Host "== Docker =="
docker info --format "{{.ServerVersion}}"

Write-Host ""
Write-Host "== Compose =="
docker compose ps

