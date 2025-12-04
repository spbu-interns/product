#!/usr/bin/env python3
"""
Скрипт для загрузки аватарок тестовым пользователям через API.
Использует маппинг тестовых аватарок на конкретных пользователей.
"""

import requests
from pathlib import Path
import sys
import time

# Настройки
API_BASE = "http://localhost:8001"
SCRIPT_DIR = Path(__file__).parent.parent
AVATARS_DIR = SCRIPT_DIR / "test_avatars"

# Маппинг: файл аватарки -> ID пользователя в БД
# ID основаны на порядке вставки в 016_test_data.sql
# Всего: 22 пользователя (1 админ + 7 докторов + 14 клиентов)
AVATAR_MAPPING = {
    "test-avatar-1.jpg": [1, 10, 16],  # admin_ivanov, alex_smirnov, maxim_karpov
    "test-avatar-2.jpg": [2, 11, 17],  # dr_petrov, olga_popova, yulia_mikhailova
    "test-avatar-3.jpg": [3, 12, 18],  # dr_sidorova, dmitry_novikov, roman_orlov
    "test-avatar-4.jpg": [4, 19],      # dr_kuznetsov, elena_fedorova
    "test-avatar-5.jpg": [5, 20],      # dr_volkova, sergey_kozlov
    "test-avatar-6.jpg": [6, 21],      # dr_sokolov, oksana_belova
    "test-avatar-7.jpg": [7, 13],      # dr_morozov, anna_sokolova
    "test-avatar-8.jpg": [8, 14],      # dr_lebedeva, igor_vasiliev
    "test-avatar-9.jpg": [9, 15, 22],  # maria_ivanova, vera_petrova, pavel_nikitin
}

def wait_for_api(max_attempts=30):
    """Ожидание готовности API."""
    for attempt in range(1, max_attempts + 1):
        try:
            response = requests.get(f"{API_BASE}/health", timeout=1)
            if response.status_code == 200:
                return True
        except:
            pass
        
        if attempt < max_attempts:
            print(f"   Попытка {attempt}/{max_attempts}...")
            time.sleep(1)
    
    return False

def upload_avatar(user_id: int, avatar_path: Path) -> bool:
    """Загружает аватарку для пользователя."""
    try:
        with open(avatar_path, 'rb') as f:
            files = {'file': (avatar_path.name, f, 'image/jpeg')}
            response = requests.post(
                f"{API_BASE}/users/{user_id}/avatar",
                files=files,
                timeout=10
            )
        
        if response.status_code in [200, 201]:
            return True
        else:
            print(f"      ⚠️  HTTP {response.status_code}: {response.text[:100]}")
            return False
    except Exception as e:
        print(f"      ❌ Ошибка: {e}")
        return False

def main():
    """Загружает аватарки через API."""
    
    print("🌐 Загрузка аватарок через API...")
    print("=" * 60)
    
    # Проверка папки с аватарками
    if not AVATARS_DIR.exists():
        print(f"❌ Папка {AVATARS_DIR} не найдена!")
        sys.exit(1)
    
    # Ожидание API
    print("Проверка доступности API...")
    if not wait_for_api():
        print("❌ API недоступен после 30 попыток")
        sys.exit(1)
    
    print("✅ API доступен")
    print()
    
    success_count = 0
    fail_count = 0
    
    # Загружаем аватарки
    for avatar_file, user_ids in AVATAR_MAPPING.items():
        avatar_path = AVATARS_DIR / avatar_file
        
        if not avatar_path.exists():
            print(f"⚠️  Файл {avatar_file} не найден, пропускаем")
            continue
        
        for user_id in user_ids:
            print(f"   Загружаю {avatar_file} для user_id={user_id}...", end=" ")
            
            if upload_avatar(user_id, avatar_path):
                print("✅")
                success_count += 1
            else:
                print("❌")
                fail_count += 1
    
    print()
    print("=" * 60)
    print(f"✅ Успешно: {success_count}")
    if fail_count > 0:
        print(f"❌ Ошибок: {fail_count}")
    print()
    print("🎉 Загрузка завершена!")

if __name__ == '__main__':
    main()
