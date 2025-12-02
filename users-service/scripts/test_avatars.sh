#!/bin/bash
# Скрипт для быстрого тестирования системы аватарок

echo "🚀 Проверка системы аватарок"
echo "======================================"

# Проверка API
echo ""
echo "1️⃣  Проверка доступности API..."
if curl -s http://localhost:8001/health > /dev/null 2>&1; then
    echo "✅ API доступен на http://localhost:8001"
else
    echo "❌ API недоступен. Запустите: uvicorn app.main:app --reload --port 8001"
    exit 1
fi

# Создание тестового изображения (1x1 пиксель красный)
echo ""
echo "2️⃣  Создание тестового изображения..."
echo -e "\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR\x00\x00\x00\x01\x00\x00\x00\x01\x08\x02\x00\x00\x00\x90wS\xde\x00\x00\x00\x0cIDATx\x9cc\xf8\xcf\xc0\x00\x00\x00\x03\x00\x01\x00\x18\xdd\x8d\xb4\x00\x00\x00\x00IEND\xaeB`\x82" > test.png
echo "✅ test.png создан"

# Загрузка аватарки для пользователя 1
echo ""
echo "3️⃣  Загрузка аватарки для user_id=1..."
UPLOAD_RESPONSE=$(curl -s -X POST "http://localhost:8001/users/1/avatar" \
    -F "file=@test.png")

if echo "$UPLOAD_RESPONSE" | grep -q "avatar uploaded successfully"; then
    echo "✅ Аватарка загружена"
    echo "   $UPLOAD_RESPONSE"
else
    echo "❌ Ошибка загрузки"
    echo "   $UPLOAD_RESPONSE"
fi

# Получение аватарки
echo ""
echo "4️⃣  Получение аватарки..."
if curl -s "http://localhost:8001/users/1/avatar" -o downloaded_test.png; then
    echo "✅ Аватарка получена и сохранена в downloaded_test.png"
    ls -lh downloaded_test.png
else
    echo "❌ Ошибка получения аватарки"
fi

# Проверка файла в папке avatars
echo ""
echo "5️⃣  Проверка файлов в папке avatars/..."
AVATAR_COUNT=$(ls -1 avatars/ 2>/dev/null | grep -v ".gitkeep" | wc -l | tr -d ' ')
echo "   Найдено файлов: $AVATAR_COUNT"
if [ "$AVATAR_COUNT" -gt 0 ]; then
    echo "   Файлы:"
    ls -lh avatars/ | grep -v ".gitkeep" | grep -v "^total"
fi

# Удаление аватарки
echo ""
echo "6️⃣  Удаление аватарки..."
DELETE_RESPONSE=$(curl -s -X DELETE "http://localhost:8001/users/1/avatar" -w "\nHTTP_CODE:%{http_code}")
HTTP_CODE=$(echo "$DELETE_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)

if [ "$HTTP_CODE" = "204" ]; then
    echo "✅ Аватарка удалена"
else
    echo "❌ Ошибка удаления (HTTP $HTTP_CODE)"
fi

# Очистка
echo ""
echo "7️⃣  Очистка тестовых файлов..."
rm -f test.png downloaded_test.png
echo "✅ Тестовые файлы удалены"

echo ""
echo "======================================"
echo "✨ Тестирование завершено!"
echo ""
echo "📖 Подробная документация: AVATARS_README.md"
echo "🧪 Python тесты: python test_avatars.py"
