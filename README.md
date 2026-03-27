# My Blog App

[![CI](https://github.com/Aberezhnoy1980/my-blog-app/actions/workflows/ci.yml/badge.svg)](https://github.com/Aberezhnoy1980/my-blog-app/actions/workflows/ci.yml)

Учебный full‑stack проект (спринт 3, Яндекс Практикум): бэкенд на Spring **без** Spring Boot, WAR в Tomcat, JDBC/JdbcTemplate, Flyway; фронт — готовый React за Nginx из материалов курса.

Бэкенд реализован на Java с использованием Spring Framework и разворачивается в сервлет‑контейнере (Tomcat).

## Цели проекта

- Реализовать бэкенд для готового фронтенда блога (React + Nginx), предоставленного Практикумом.
- Обеспечить полный набор REST‑эндпоинтов для работы с постами и комментариями, включая поиск, пагинацию, лайки и работу с картинками. 
- Показать владение Spring Framework (Core, Web, Data, AOP), работой с БД и интеграционным тестированием.

Подробное техническое задание описано в файле [`docs/technical-spec.md`](docs/technical-spec.md).

## Общая архитектура

Архитектура следует описанию из задания Практикума: 

- **Фронтенд**: React‑приложение за Nginx — UI на `http://localhost` (порт 80). Сборка Практикума обращается к API по **`http://localhost:8080/api/...`**; в Docker Tomcat проброшен на хост `:8080`, для запросов со страницы `:80` включён **CORS** в Spring.
- **Бэкенд**: Spring MVC без Spring Boot, WAR в Tomcat; в compose также `8080:8080` и прокси `/api` через Nginx (удобно для `curl` на `http://localhost/api/...`).
- **База данных**: PostgreSQL (prod / Docker) или H2 (тесты, локальный dev), хранит посты и комментарии.

### Docker Compose (PostgreSQL + Tomcat + Nginx)

Из **корня** репозитория:

```bash
docker compose up --build
```

После старта:

- UI: [http://localhost](http://localhost)
- Smoke API через прокси: `curl -sS http://localhost/api/health`

Тому же контракту `/api/...` соответствует прямой запуск Tomcat на машине: `http://localhost:8080/api/...` (профиль `prod`, переменные `DB_*` — см. `application-prod.properties`).

**Фронтенд:** образ копирует содержимое `my-blog-front-app/dist` в Nginx без изменений относительно архива Практикума (`index.html` + `assets/`). API в бандле задано как **`http://localhost:8080`** — в `docker-compose` сервис backend публикует этот порт на хост; ответы для запросов со страницы на порту 80 разрешены через **CORS** в `WebMvcConfig`.

#### База данных (Docker) и миграции

- **Персистентность:** каталог `./postgres-data` монтируется в контейнер PostgreSQL (`/var/lib/postgresql/data`), не коммитится (`.gitignore`). Пересборка образов приложения этот каталог не затрагивает.
- **Flyway:** скрипты в `db/migration/` описывают **схему** БД; состояние применённых версий хранится в `flyway_schema_history`. Повторный запуск применяет только новые миграции; пользовательский контент в таблицах миграциями не перезаписывается.
- **Сброс данных:** удаление `./postgres-data` или старт с пустым каталогом даёт новый экземпляр БД с повторным накатом миграций с `V1`.

## Функциональность бэкенда (кратко)

Бэкенд предоставляет REST‑эндпоинты, совместимые с фронтендом Практикума: 

- Работа с постами:
  - Получение списка постов с поиском и пагинацией: `GET /api/posts?search=...&pageNumber=...&pageSize=...`.
  - Получение одного поста: `POST /api/posts/{id}`.
  - Создание, редактирование и удаление постов: `POST /api/posts`, `PUT /api/posts/{id}`, `DELETE /api/posts/{id}`.
  - Лайки постов: `POST /api/posts/{id}/likes`.
  - Работа с картинками постов: `PUT /api/posts/{id}/image`, `GET /api/posts/{id}/image`. 

- Работа с комментариями:
  - Получение списка комментариев поста: `GET /api/posts/{postId}/comments`.
  - Получение одного комментария: `GET /api/posts/{postId}/comments/{id}`.
  - Создание, редактирование и удаление комментариев:
    - `POST /api/posts/{postId}/comments`
    - `PUT /api/posts/{postId}/comments/{id}`
    - `DELETE /api/posts/{postId}/comments/{id}`. 

Детальные контрактные форматы запросов/ответов и правила поиска описаны в `docs/technical-spec.md`. 

## Технологический стек

- Java 21. 
- Spring Framework 6.1+ (Core, Web, Data, AOP).
- Сервлет‑контейнер: Tomcat 10.1.52.
- База данных: PostgreSQL (prod) / H2 (тесты, dev). 
- Система сборки: Maven (war‑пакетирование, плагины для деплоя в контейнер).
- Тестирование: JUnit 5, Spring TestContext Framework, WebMvc, H2. 
- Docker, Docker Compose (`docker-compose.yml` в корне).

## Структура репозитория

```text
.
├── my-blog-back-app/          # Maven-модуль бэкенда (Spring, WAR)
├── my-blog-front-app/         # Статический фронт Практикума + Dockerfile Nginx
├── docker/
│   └── backend/
│       └── Dockerfile       # Сборка WAR + Tomcat (ROOT.war)
├── docs/
│   ├── technical-spec.md
│   └── ...
├── docker-compose.yml
├── postgres-data/             # данные PostgreSQL (Docker), в git не входит
└── README.md
```

## Локальный фронт без compose (как в задании Практикума)

Раньше в архиве был отдельный `docker-compose` только для Nginx. Сейчас оркестрация вынесена в корень; при необходимости можно собрать образ только фронта: `docker build -t my-blog-front ./my-blog-front-app`.

## Статус проекта

- [x] Анализ задания и техническое задание (`docs/technical-spec.md`).
- [x] Maven‑проект бэкенда, Tomcat, Flyway, профили dev/test/prod.
- [x] REST по контракту с фронтом, тесты, CI (GitHub Actions).
- [x] Docker Compose: PostgreSQL + Tomcat (WAR) + Nginx.