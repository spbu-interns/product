# PowerShell скрипт для запуска Users Service на Windows
# Использование:
#   .\scripts\run-server.ps1          # Обычный запуск
#   .\scripts\run-server.ps1 -Reset   # Полный сброс БД

param(
    [switch]$Reset
)

Write-Host "🚀 Запуск Users Service" -ForegroundColor Green
Write-Host "======================================" -ForegroundColor Gray

# Перейти в корень users-service (родительская папка от scripts/)
Set-Location (Join-Path $PSScriptRoot "..")

# Проверка флага -Reset
if ($Reset) {
    Write-Host ""
    Write-Host "⚠️  РЕЖИМ: Полный сброс базы данных" -ForegroundColor Yellow
    Write-Host "======================================" -ForegroundColor Gray
}

# 1. Остановка и очистка (если -Reset)
if ($Reset) {
    Write-Host ""
    Write-Host "1️⃣  Остановка и удаление старой БД..." -ForegroundColor Cyan
    docker compose down -v
    Write-Host "   ✅ Старые данные удалены" -ForegroundColor Green
}

# 2. Запуск PostgreSQL
Write-Host ""
if ($Reset) {
    Write-Host "2️⃣  Запуск PostgreSQL в Docker..." -ForegroundColor Cyan
} else {
    Write-Host "1️⃣  Запуск PostgreSQL в Docker..." -ForegroundColor Cyan
}
docker compose up -d

# Ожидание готовности БД с улучшенной проверкой
Write-Host "   Ожидание готовности PostgreSQL..." -ForegroundColor Gray
$maxAttempts = 30
$attempt = 1

while ($attempt -le $maxAttempts) {
    $ready = docker exec users_pg pg_isready -U userdb -d userdb 2>$null
    if ($LASTEXITCODE -eq 0) {
        break
    }
    
    if ($attempt -ge $maxAttempts) {
        Write-Host "   ❌ База данных не запустилась за $maxAttempts секунд" -ForegroundColor Red
        docker compose logs
        exit 1
    }
    
    Write-Host "   Попытка $attempt/$maxAttempts..." -ForegroundColor Gray
    Start-Sleep -Seconds 1
    $attempt++
}

Write-Host "   ✅ PostgreSQL запущен и готов" -ForegroundColor Green

# Проверка, первый ли это запуск (пустая БД) или режим -Reset
$userCount = docker exec users_pg psql -U userdb -d userdb -t -c "SELECT COUNT(*) FROM users WHERE email LIKE '%@test.com';" 2>$null
if ($LASTEXITCODE -ne 0) {
    $userCount = "0"
}
$userCount = $userCount.Trim()

if ($userCount -eq "0" -or $Reset) {
    Write-Host ""
    if ($Reset) {
        Write-Host "3️⃣  Применение SQL миграций..." -ForegroundColor Cyan
    } else {
        Write-Host "📊 Обнаружена пустая БД, применяю все миграции..." -ForegroundColor Cyan
    }
    
    Get-ChildItem "sql\*.sql" | Sort-Object Name | ForEach-Object {
        $filename = $_.Name
        Write-Host "   Применяю $filename..." -ForegroundColor Gray
        Get-Content $_.FullName | docker exec -i users_pg psql -U userdb -d userdb | Out-Null
    }
    
    Write-Host "   ✅ Миграции применены" -ForegroundColor Green
    
    # Статистика созданных данных
    if ($Reset) {
        Write-Host ""
        Write-Host "4️⃣  Статистика созданных данных:" -ForegroundColor Cyan
        
        $clinicsCount = (docker exec users_pg psql -U userdb -d userdb -t -c "SELECT COUNT(*) FROM clinics;" 2>$null).Trim()
        $usersCount = (docker exec users_pg psql -U userdb -d userdb -t -c "SELECT COUNT(*) FROM users;" 2>$null).Trim()
        $doctorsCount = (docker exec users_pg psql -U userdb -d userdb -t -c "SELECT COUNT(*) FROM doctors;" 2>$null).Trim()
        $clientsCount = (docker exec users_pg psql -U userdb -d userdb -t -c "SELECT COUNT(*) FROM clients;" 2>$null).Trim()
        
        Write-Host "   📊 Клиники: $clinicsCount | Доктора: $doctorsCount | Клиенты: $clientsCount | Всего пользователей: $usersCount" -ForegroundColor Gray
    }
    
    # Подсказка о загрузке аватарок
    if (Test-Path "test_avatars") {
        Write-Host ""
        Write-Host "💡 Для загрузки тестовых аватарок запустите после старта API:" -ForegroundColor Yellow
        Write-Host "   python scripts\upload_avatars_via_api.py"
    }
} else {
    Write-Host "   ℹ️  БД уже содержит данные ($userCount тестовых пользователей)" -ForegroundColor Gray
}

# Активация виртуального окружения
Write-Host ""
if ($Reset) {
    Write-Host "5️⃣  Активация виртуального окружения..." -ForegroundColor Cyan
} else {
    Write-Host "2️⃣  Активация виртуального окружения..." -ForegroundColor Cyan
}
if (-not (Test-Path ".venv")) {
    Write-Host "   ⚠️  Виртуальное окружение не найдено. Создаю..." -ForegroundColor Yellow
    python -m venv .venv
}

.\.venv\Scripts\Activate.ps1
Write-Host "   ✅ Виртуальное окружение активировано" -ForegroundColor Green

# Установка/обновление зависимостей
Write-Host ""
if ($Reset) {
    Write-Host "6️⃣  Проверка зависимостей..." -ForegroundColor Cyan
} else {
    Write-Host "3️⃣  Проверка зависимостей..." -ForegroundColor Cyan
}
pip install -q -r requirements.txt
Write-Host "   ✅ Зависимости установлены" -ForegroundColor Green

# Создание папки avatars (если нет)
if (-not (Test-Path "avatars")) {
    New-Item -ItemType Directory -Path "avatars" | Out-Null
    Write-Host "   ✅ Создана папка avatars/" -ForegroundColor Green
}

# Запуск API
Write-Host ""
if ($Reset) {
    Write-Host "7️⃣  Запуск API сервера..." -ForegroundColor Cyan
} else {
    Write-Host "4️⃣  Запуск API сервера..." -ForegroundColor Cyan
}
Write-Host "======================================" -ForegroundColor Gray
Write-Host ""
Write-Host "📡 API будет доступен на:" -ForegroundColor Green
Write-Host "   http://localhost:8001"
Write-Host "   http://localhost:8001/docs (Swagger UI)"
Write-Host ""
Write-Host "🛑 Для остановки нажмите Ctrl+C" -ForegroundColor Yellow
Write-Host ""

uvicorn app.main:app --reload --port 8001
