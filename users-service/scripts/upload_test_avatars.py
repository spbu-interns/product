#!/usr/bin/env python3
"""
Скрипт для копирования тестовых аватарок из test_avatars/ в avatars/.
Подготовка файлов для использования тестовых данных.
"""

import os
import sys
import shutil
from pathlib import Path

# Настройки
SCRIPT_DIR = Path(__file__).parent.parent
AVATARS_SOURCE_DIR = SCRIPT_DIR / "test_avatars"
AVATARS_TARGET_DIR = SCRIPT_DIR / "avatars"

def main():
    """Копирует все аватарки из test_avatars/ в avatars/."""
    
    print("🖼️  Копирование тестовых аватарок...")
    print("=" * 60)
    
    # Проверка наличия папок
    if not AVATARS_SOURCE_DIR.exists():
        print(f"❌ Папка {AVATARS_SOURCE_DIR} не найдена!")
        sys.exit(1)
    
    if not AVATARS_TARGET_DIR.exists():
        AVATARS_TARGET_DIR.mkdir(parents=True, exist_ok=True)
    
    # Получаем список всех файлов
    source_files = list(AVATARS_SOURCE_DIR.glob("*.jpg")) + list(AVATARS_SOURCE_DIR.glob("*.png"))
    
    if not source_files:
        print(f"❌ В папке {AVATARS_SOURCE_DIR} не найдены изображения")
        sys.exit(1)
    
    print(f"✅ Найдено {len(source_files)} файлов")
    
    copied_count = 0
    
    for source_file in source_files:
        target_file = AVATARS_TARGET_DIR / source_file.name
        
        try:
            shutil.copy2(source_file, target_file)
            print(f"✅ {source_file.name}")
            copied_count += 1
        except Exception as e:
            print(f"❌ Ошибка при копировании {source_file.name}: {e}")
    
    print()
    print("=" * 60)
    print(f"✅ Скопировано {copied_count} файлов в avatars/")

if __name__ == '__main__':
    main()
