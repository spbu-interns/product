# Users Service


##  Быстрый старт

### 1. Клонировать проект
```bash
git clone <repo-url>
cd users-service
```
2. Поднять базу данных (Postgres в Docker)
```bash
docker compose up -d
```

- сервис Postgres будет доступен на localhost:5432
- база данных: usersdb
- логин/пароль: app/secret (см. .env)

Проверить контейнер:
```bash
docker compose ps
```
3. Настроить окружение Python
```bash
python -m venv .venv
source .venv/bin/activate   # Windows: .venv\Scripts\Activate.ps1
pip install -r requirements.txt
```
4. Запустить API
```bash
uvicorn app.main:app --reload
```
API будет доступен на:
	•	Swagger ui → http://127.0.0.1:8000/docs
	•	Redoc → http://127.0.0.1:8000/redoc

⸻

📂 Структура проекта
```bash
users-service/
├─ docker-compose.yml       # Postgres в контейнере
├─ .env                     # настройки подключения к БД
├─ sql/                     # схемы и сиды БД
│  ├─ 001_schema.sql
│  └─ 002_seed.sql
├─ app/                     # Python-код
│  ├─ main.py               # FastAPI-приложение
│  ├─ db.py                 # подключение к БД
│  ├─ models.py             # Pydantic-модели
│  └─ repository.py         # SQL-запросы
├─ requirements.txt         # зависимости Python
└─ README.md                # документация
```

⸻

🛠 Методы API
```
	•	GET /users/by-email/{email} → найти пользователя по email
	•	GET /users/by-login/{login} → найти пользователя по login
	•	GET /users/exists/email/{email} → проверить существование email
	•	GET /users/exists/login/{login} → проверить существование login
	•	POST /users → добавить нового пользователя

{
  "email": "new@ex.com",
  "login": "newbee",
  "password": "s3cret",
  "role": "CLIENT"
}


	•	GET /users → получить всех пользователей
	•	GET /users?role=CLIENT → получить всех клиентов
```
⸻

 Остановка сервисов
	•	FastAPI (uvicorn): CTRL+C
	•	Postgres (Docker):

```bash
docker compose down
```
Если нужно удалить данные тоже:
```bash
docker compose down -v
```
## 🔄 Обновление схемы БД после правок в `sql/001_schema.sql` и применение `sql/004_email_tokens.sql`, `sql/005_patient_complaints_notes.sql`
```bash
docker compose down -v
```

```bash
docker compose up -d
```
### ⚠️
- Пароли хранятся как bcrypt-хэши (не в открытом виде).
- Начальные данные загружаются из sql/002_seed.sql.
