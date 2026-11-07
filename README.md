# Client Service API

REST API для учёта клиентов компании «Лига Цифровой Экономики».

| Технология | Назначение |
|---|---|
| Java 21 | Язык программирования |
| Spring Boot 3.5.16 | Создание и запуск REST API |
| Maven | Сборка проекта и управление зависимостями |
| Spring Data JPA / Hibernate | Работа с базой данных через Java-объекты |
| PostgreSQL | Хранение данных клиентов |
| Flyway | Управление миграциями схемы базы данных |
| Swagger UI | Интерактивная документация API и отправка запросов |

## Запуск

Нужны Java 21, Maven и запущенный Docker Desktop.

1. Открыть проект в IDE как Maven-проект, выбрать **Project SDK: Java 21** и дождаться загрузки зависимостей.
2. В терминале, из папки проекта рядом с `pom.xml`, выполнить:

   ```bash
   docker compose up -d --wait
   ```

   При первом запуске Docker скачает PostgreSQL 17.6 и создаст базы `clients` и `clients_test`. Команда дождётся готовности PostgreSQL и оставит его работать в фоне. Устанавливать PostgreSQL на компьютер и создавать базы вручную не требуется.
3. Запустить `ClientServiceApplication`.
4. Открыть [Swagger UI](http://localhost:8080/swagger-ui.html).

[OpenAPI JSON](http://localhost:8080/v3/api-docs) содержит описание API.

Docker запускает PostgreSQL;

Приложение запускается в IDE.

Данные сохраняются в Docker volume `postgres_data` между запусками и пересозданиями контейнера.

Если терминал после установки Docker не находит команду `docker`, перезапустить IDE и открыть новую вкладку терминала.

## Подключение к базе

Настройки по умолчанию совпадают с `compose.yml`, для обычного запуска переменные задавать не нужно.

| Переменная | Значение по умолчанию |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5433/clients` |
| `DB_USERNAME` | `clients` |
| `DB_PASSWORD` | `clients` |
| `SERVER_PORT` | `8080` |
| `TEST_DB_URL` | `jdbc:postgresql://localhost:5433/clients_test` |
| `TEST_DB_USERNAME` | `clients` |
| `TEST_DB_PASSWORD` | `clients` |

`docker/init-test-db.sql` создаёт только базу `clients_test` и выполняется при первой инициализации пустого volume. Таблицу `client_account` в каждой базе создаёт Flyway при запуске приложения или тестов.

## API

| Метод и путь | Результат |
|---|---|
| `POST /api/clients` | `201 Created`, клиент и заголовок `Location` |
| `GET /api/clients` | `200 OK`, список по возрастанию ID; пустой список — `[]` |
| `GET /api/clients/{id}` | `200 OK` или `404 Not Found` |
| `PUT /api/clients/{id}` | `200 OK` с актуальными данными или `404 Not Found` |
| `DELETE /api/clients/{id}` | Физическое удаление, `204 No Content` или `404 Not Found` |

Создание:

```json
{"fullName":"Иванов Иван Иванович","gender":"M"}
```

`fullName` обязателен и не может быть пустым или состоять только из пробелов. Крайние пробелы удаляются. ФИО не уникально. `gender` — `M` или `F`; при создании поле можно пропустить или передать `null`. Новый клиент всегда имеет статус `ACTIVE`.

Обновление:

```json
{"status":"INACTIVE"}
```

PUT обновляет только переданные поля `fullName`, `gender`, `status`. Пропущенные поля сохраняются; явный `null` запрещён. Пустой объект и повторное присваивание прежних значений не меняют даты. Очистить пол до NULL через PUT нельзя.

Ответ содержит `id`, `fullName`, `gender`, `status`, `createdAt`, `updatedAt`. Даты представлены в ISO 8601 UTC. При создании они совпадают; при изменении обновляется только `updatedAt`.

Некорректные поля, типы, значения enum и формат ID возвращают `400`. ID и даты нельзя задать или изменить через API; статус можно передать только при обновлении. Числовой ID отсутствующей записи возвращает `404`.

Ошибки имеют формат `application/problem+json`. Ошибки валидации дополнительно содержат список `errors` с именами полей и сообщениями.

## Структура

- `client/` — сущность клиента, enum, репозиторий, сервис и контроллер.
- `client/dto/` — запросы создания и обновления, ответ API.
- `error/` — обработка ошибок.
- `config/` — OpenAPI и настройки JSON.
- `src/main/resources/db/migration/` — создание таблицы `client_account`.
- `src/test/` — интеграционные проверки.
- `compose.yml` — PostgreSQL, порт подключения, хранение данных и проверка готовности.
- `docker/init-test-db.sql` — создание отдельной базы для тестов.

В таблице хранятся `id`, `full_name`, `gender`, `status`, `create_dttm`, `modify_dttm`. Hibernate проверяет схему, а изменяет её Flyway. При ручном SQL UPDATE дату `modify_dttm` нужно обновлять самостоятельно: JPA-callback вне приложения не выполняется.
