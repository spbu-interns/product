# INTERNS

 Interns - медицинская платформа, цель которой соединить пациентов, врачей и администраторов клиник; гарантировать безопасность и единое хранение истории болезней.

🔗 [Figma](https://left-horse-59061146.figma.site/)  
📖 [GitBook](https://app.gitbook.com/invite/EqtRMZ09gU5WjTQBpnhe/UU9QKF5uThAdszLQO3Oa)  

---

## Структура проекта

- **/composeApp** — общий код для приложений (ui с помощью *KVision*).   
- **/server** — приложение на Ktor.  
- **/shared** — общий код для всех целей (главный модуль: *commonMain*).  

---

## 🚀 Запуск

### Server

Для запуска лучше запустить соответствующий ОС скрипт:

`scripts/run-server.sh` для  macOS/Linux

`scripts/run-server.ps1` для  Windows

Но все так же можно запустить командами
```bash
./gradlew :server:run          # macOS/Linux
.\gradlew.bat :server:run      # Windows
```

### Web
```bash
./gradlew :composeApp:jsBrowserDevelopmentRun      # macOS/Linux
.\gradlew.bat :composeApp:jsBrowserDevelopmentRun  # Windows
```