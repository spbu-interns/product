# PowerShell скрипт для запуска Users Service на Windows

Write-Host "🚀 Запуск Users Service" -ForegroundColor Green
Write-Host "======================================" -ForegroundColor Gray

# Перейти в корень users-service (родительская папка от scripts/)
Set-Location (Join-Path $PSScriptRoot "..")

# 1. Запуск PostgreSQL
Write-Host ""
Write-Host "1️⃣  Запуск PostgreSQL в Docker..." -ForegroundColor Cyan
docker compose up -d

# Ожидание готовности БД
Write-Host "   Ожидание готовности PostgreSQL..." -ForegroundColor Gray
Start-Sleep -Seconds 3

# Проверка статуса
$containers = docker compose ps
if ($containers -match "users_pg.*Up") {
    Write-Host "   ✅ PostgreSQL запущен" -ForegroundColor Green
} else {
    Write-Host "   ❌ Ошибка запуска PostgreSQL" -ForegroundColor Red
    docker compose logs
    exit 1
}

# 2. Активация виртуального окружения
Write-Host ""
Write-Host "2️⃣  Активация виртуального окружения..." -ForegroundColor Cyan
if (-not (Test-Path ".venv")) {
    Write-Host "   ⚠️  Виртуальное окружение не найдено. Создаю..." -ForegroundColor Yellow
    python -m venv .venv
}

.\.venv\Scripts\Activate.ps1
Write-Host "   ✅ Виртуальное окружение активировано" -ForegroundColor Green

# 3. Установка/обновление зависимостей
Write-Host ""
Write-Host "3️⃣  Проверка зависимостей..." -ForegroundColor Cyan
pip install -q -r requirements.txt
Write-Host "   ✅ Зависимости установлены" -ForegroundColor Green

# 4. Создание папки avatars (если нет)
if (-not (Test-Path "avatars")) {
    New-Item -ItemType Directory -Path "avatars" | Out-Null
    Write-Host "   ✅ Создана папка avatars/" -ForegroundColor Green
}

# 5. Запуск API
Write-Host ""
Write-Host "4️⃣  Запуск API сервера..." -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Gray
Write-Host ""
Write-Host "📡 API будет доступен на:" -ForegroundColor Green
Write-Host "   http://localhost:8001"
Write-Host "   http://localhost:8001/docs (Swagger UI)"
Write-Host ""
Write-Host "🛑 Для остановки нажмите Ctrl+C" -ForegroundColor Yellow
Write-Host ""

uvicorn app.main:app --reload --port 8001
