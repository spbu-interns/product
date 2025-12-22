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
# Всего: 46 пользователей (3 админа + 14 докторов + 29 клиентов)
# Мужские аватарки (male_*.jpg): для пользователей с gender='male'
# Женские аватарки (female_*.jpg): для пользователей с gender='female'

# ID пользователей по порядку:
# 1-admin (M), 2-dr_petrov (M), 3-dr_sidorova (F), 4-dr_kuznetsov (M), 5-dr_volkova (F),
# 6-dr_sokolov (M), 7-dr_morozov (M), 8-dr_lebedeva (F), 9-maria_ivanova (F), 10-alex_smirnov (M),
# 11-olga_popova (F), 12-dmitry_novikov (M), 13-elena_fedorova (F), 14-sergey_kozlov (M),
# 15-anna_sokolova (F), 16-igor_vasiliev (M), 17-vera_petrova (F), 18-maxim_karpov (M),
# 19-yulia_mikhailova (F), 20-roman_orlov (M), 21-oksana_belova (F), 22-pavel_nikitin (M),
# 23-admin_sidorov (M), 24-admin_nikolaeva (F), 25-dr_ivanov_derm (M), 26-dr_gromova (F),
# 27-dr_belov (M), 28-dr_romanova (F), 29-dr_zaitsev (M), 30-dr_krasnova (F), 31-dr_titov (M),
# 32-denis_antonov (M), 33-svetlana_andreeva (F), 34-konstantin_borisov (M), 35-natalia_gerasimova (F),
# 36-viktor_danilov (M), 37-tatiana_egorova (F), 38-oleg_frolov (M), 39-inna_galkina (F),
# 40-artem_ilyin (M), 41-larisa_kiseleva (F), 42-grigoriy_lebedev (M), 43-kristina_makarova (F),
# 44-andrey_nesterov (M), 45-veronika_orlova (F), 46-boris_pavlov (M)

AVATAR_MAPPING = {
    # Мужские аватарки (male_test-avatar-*.jpg)
    "male_test-avatar-1.jpg": [1, 10, 18, 32, 40],     # admin, alex, maxim, denis, artem
    "male_test-avatar-3.jpg": [4, 12, 22, 34, 42],     # dr_kuznetsov, dmitry, pavel, konstantin, grigoriy
    "male_test-avatar-4.jpg": [6, 14, 27, 36, 44],     # dr_sokolov, sergey, dr_belov, viktor, andrey
    "male_test-avatar-9.jpg": [2, 7, 16, 20, 23, 25, 29, 31, 38, 46],  # dr_petrov, dr_morozov, igor, roman, admin2, dr_ivanov, dr_zaitsev, dr_titov, oleg, boris
    
    # Женские аватарки (female_test-avatar-*.jpg)
    "female_test-avatar-2.jpg": [3, 11, 19, 24, 33],   # dr_sidorova, olga, yulia, admin3, svetlana
    "female_test-avatar-5.jpg": [5, 9, 17, 26, 35],    # dr_volkova, maria, vera, dr_gromova, natalia
    "female_test-avatar-6.jpg": [8, 13, 21, 28, 37],   # dr_lebedeva, elena, oksana, dr_romanova, tatiana
    "female_test-avatar-7.jpg": [15, 30, 39, 43],      # anna, dr_krasnova, inna, kristina
    "female_test-avatar-8.jpg": [41, 45],              # larisa, veronika
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
