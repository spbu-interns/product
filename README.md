# INTERNS

 Interns - медицинская платформа, цель которой соединить пациентов, врачей и администраторов клиник; гарантировать безопасность и единое хранение истории болезней.

🔗 [Figma](https://left-horse-59061146.figma.site/)  
📖 [GitBook](https://app.gitbook.com/invite/EqtRMZ09gU5WjTQBpnhe/UU9QKF5uThAdszLQO3Oa)  

---

## Структура проекта

- **/composeApp** — общий код для приложений (UI).  
  - *commonMain* — общий для всех платформ.  
  - *iosMain, jvmMain* и др. — специфичный для платформ.  
- **/server** — приложение на Ktor.  
- **/shared** — общий код для всех целей (главный модуль: *commonMain*).  

---

## 🚀 Запуск

### Desktop (JVM)
```bash
./gradlew :composeApp:run      # macOS/Linux
.\gradlew.bat :composeApp:run  # Windows
```

### Server
```bash
./gradlew :server:run          # macOS/Linux
.\gradlew.bat :server:run      # Windows
```

### Web
```bash
./gradlew :composeApp:wasmJsBrowserDevelopmentRun      # macOS/Linux
.\gradlew.bat :composeApp:wasmJsBrowserDevelopmentRun  # Windows
```