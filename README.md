# Shipment Service

Production-ready Java-микросервис для логистической платформы. Управляет отправлениями
и их полным жизненным циклом, предоставляет защищённый REST API и публикует доменные
события в RabbitMQ через транзакционный outbox.

## Содержание
1. [Что делает сервис](#1-что-делает-сервис)
2. [Архитектура](#2-архитектура)
3. [Компонентный вид в стиле C4](#3-компонентный-вид-в-стиле-c4)
4. [Технологический стек](#4-технологический-стек)
5. [Локальный запуск](#5-локальный-запуск)
6. [Docker Compose](#6-docker-compose)
7. [Настройка Keycloak](#7-настройка-keycloak)
8. [Получение JWT](#8-получение-jwt)
9. [Примеры API](#9-примеры-api)
10. [Топология RabbitMQ](#10-топология-rabbitmq)
11. [Схема базы данных](#11-схема-базы-данных)
12. [Паттерн Outbox](#12-паттерн-outbox)
13. [Модель безопасности](#13-модель-безопасности)
14. [Тестирование](#14-тестирование)
15. [Конфигурация](#15-конфигурация)
16. [Аспекты production-эксплуатации](#16-аспекты-production-эксплуатации)
17. [Известные ограничения](#17-известные-ограничения)
18. [Возможные улучшения](#18-возможные-улучшения)

---

## 1. Что делает сервис

Сервис позволяет клиенту создавать отправления (отправитель, получатель, адреса
отправки и доставки, вес/габариты, тип доставки, желаемый срок доставки), присваивает
уникальный трек-номер и проводит отправление по контролируемому конечному автомату.
События надёжно публикуются в RabbitMQ.

Бизнес-правила:
- Строго прямой конечный автомат: `CREATED → CONFIRMED → PICKUP_ASSIGNED → PICKED_UP → IN_TRANSIT → OUT_FOR_DELIVERY → DELIVERED`.
- Поддержка `CANCELLED`, `DELIVERY_FAILED` (с повторной доставкой) и `RETURNED`.
- Нельзя вернуть доставленное/терминальное отправление назад; нельзя повторять переход;
  нельзя назначить pickup для отменённого отправления; нельзя отменить уже доставленное.
- Оптимистичная блокировка предотвращает «тихую» потерю одновременных обновлений.
- Идемпотентное создание через `Idempotency-Key`.
- Надёжная публикация событий через транзакционный outbox.

## 2. Архитектура

Pragmatic-подход к чистым/гексагональным слоям, четыре уровня:

```
com.example.logistics
├── domain          Агрегат Shipment, value objects (Address/Dimensions/TrackingNumber/Party),
│                   конечный автомат, исключения и out-порты (интерфейсы)
├── application     Use-case сервисы, команды, query-объекты, доменные события; владеет транзакциями
├── infrastructure  Адаптеры: JPA, Flyway, RabbitMQ, Keycloak/OAuth2, веб-фильтры, OpenAPI
└── interfaces      REST-контроллеры, DTO, глобальный обработчик ошибок по RFC 7807
```

Правило зависимостей: `interfaces → application → domain`, при этом `infrastructure`
реализует порты `domain`. Бизнес-логика никогда не зависит от Spring/JPA/RabbitMQ/Keycloak.

Ключевые файлы:
- `domain/model/Shipment.java` — агрегат + конечный автомат
- `domain/repository/*` — интерфейсы out-портов
- `application/service/ShipmentApplicationService.java` — use cases, транзакции, outbox
- `application/service/OutboxPublisher.java` — диспетчер outbox
- `infrastructure/messaging/*` — топология RabbitMQ, publisher, consumer
- `infrastructure/security/*` — OAuth2 resource server + RBAC
- `interfaces/rest/ShipmentController.java` — REST API

## 3. Компонентный вид в стиле C4

```
[Клиент]
   │  HTTPS / Bearer JWT
   ▼
[Shipment Service] ──► [PostgreSQL]   (shipments, outbox_events, idempotency_keys)
   │
   ├─► [RabbitMQ]   события: ShipmentCreated, ShipmentStatusChanged,
   │                ShipmentCancelled, ShipmentDelivered  (+ DLX/DLQ)
   ▼
[Keycloak]  выпускает / проверяет JWT (роли: LOGISTICS_USER/OPERATOR/ADMIN)
```

- **Контейнер**: Spring Boot-приложение shipment-service.
- **Компоненты** внутри приложения: REST-контроллер → application-сервис → доменный
  агрегат, сохраняемый через адаптер репозитория; outbox dispatcher отправляет события
  в RabbitMQ.
- **Внешние**: PostgreSQL, RabbitMQ, Keycloak.

## 4. Технологический стек

| Область | Технология |
|---|---|
| Язык | Java 21 (LTS) |
| Фреймворк | Spring Boot 3.3.x |
| Web | Spring Web MVC |
| Хранение | Spring Data JPA / Hibernate + PostgreSQL 16 |
| Миграции | Flyway |
| Сообщения | Spring AMQP / RabbitMQ + Transactional Outbox |
| Безопасность | Keycloak, OAuth2/OIDC, JWT (resource server) |
| Валидация | Jakarta Bean Validation |
| Документация API | OpenAPI 3 / springdoc (Swagger UI) |
| Тестирование | JUnit 5, Mockito, Testcontainers (Postgres/RabbitMQ/Keycloak) |
| Сборка | Maven |
| Наблюдаемость | Actuator, Micrometer/Prometheus, OpenTelemetry, структурированные логи |
| Запуск | Docker / Docker Compose |

## 5. Локальный запуск

Требования: JDK 21, Maven 3.9+, запущенный Docker Desktop.

Запуск инфраструктуры:

```bash
docker compose up -d postgres rabbitmq keycloak
```

Запуск приложения (профиль `local`):

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Приложение: `http://localhost:8080` — Swagger UI на `http://localhost:8080/swagger-ui.html`,
health на `http://localhost:8080/actuator/health`.

## 6. Docker Compose

`docker-compose.yml` поднимает полный стек (учётные данные только для разработки —
обозначены как таковые):

```bash
docker compose up --build -d
```

| Сервис | Образ | Порты |
|---|---|---|
| application | build (./Dockerfile) | 8080 |
| postgres | postgres:16-alpine | 5432 |
| rabbitmq | rabbitmq:3.13-management-alpine | 5672, 15672 |
| keycloak | quay.io/keycloak/keycloak:24.0.4 | 8000 |

Демо-учётные данные (только для разработки): БД `shipment/shipment`, Rabbit
`guest/guest`, Keycloak admin `admin/admin`, пользователи realm `user/user123`,
`operator/operator123`, `admin/admin123`.

## 7. Настройка Keycloak

Realm импортируется автоматически через `docker/keycloak/realm-export.json` вместе с
ролями и демо-пользователями (см. раздел 6). Для ручной настройки:

1. Войдите на `http://localhost:8000` (`admin` / `admin`).
2. Создайте realm `logistics`.
3. Добавьте realm-роли: `LOGISTICS_USER`, `LOGISTICS_OPERATOR`, `LOGISTICS_ADMIN`.
4. Создайте клиент `shipment-service` (confidential, direct-access grants; секрет
   `shipment-service-secret`).
5. Создайте пользователей и назначьте роли.

## 8. Получение JWT

```bash
curl -X POST http://localhost:8000/realms/logistics/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=shipment-service&client_secret=shipment-service-secret" \
  -d "grant_type=password&username=operator&password=operator123"
```

В ответе содержится `access_token`; используйте его как `Authorization: Bearer <token>`.

## 9. Примеры API

Создание отправления (идемпотентно):

```bash
curl -X POST http://localhost:8080/api/v1/shipments \
  -H "Authorization: Bearer $TOKEN" -H "Idempotency-Key: $(uuidgen)" \
  -H "Content-Type: application/json" -d '{
    "sender": {"name":"Alice","phone":"+79000000000","email":"alice@example.com"},
    "recipient": {"name":"Bob","phone":"+79000000001","email":"bob@example.com"},
    "pickupAddress": {"street":"1 Main St","city":"London","postalCode":"SW1A 1AA","country":"GB"},
    "deliveryAddress": {"street":"2 High St","city":"Manchester","postalCode":"M1 1AE","country":"GB"},
    "dimensions": {"lengthCm":30,"widthCm":20,"heightCm":10,"weightKg":2.5},
    "deliveryType":"EXPRESS",
    "estimatedDeliveryDate":"2026-09-05"
  }'
```

Прочие эндпоинты:

```http
GET    /api/v1/shipments/{id}
GET    /api/v1/shipments/tracking/{trackingNumber}
GET    /api/v1/shipments?page=0&size=20&status=CREATED&sortBy=createdAt&sortDirection=desc
PATCH  /api/v1/shipments/{id}/status    {"status":"CONFIRMED"}
POST   /api/v1/shipments/{id}/cancel    {"reason":"..."}
```

Ошибки используют RFC 7807 `application/problem+json`:

```json
{
  "type": "https://logistics.example.com/problems/conflict",
  "title": "Conflict",
  "status": 409,
  "detail": "Transition from 'CREATED' to 'DELIVERED' is not allowed",
  "instance": "GET /api/v1/shipments/.../status",
  "correlationId": "6f4b..."
}
```

## 10. Топология RabbitMQ

```
logistics.events.direct  (durable direct exchange)
   ├─ q.shipment.events  ◄── shipment.event.ShipmentCreated
   │                     ◄── shipment.event.ShipmentStatusChanged
   │                     ◄── shipment.event.ShipmentCancelled
   │                     ◄── shipment.event.ShipmentDelivered
   │       (x-dead-letter-exchange = logistics.events.dlx, routing "dead.shipment")
   ▼
logistics.events.dlx  ──►  q.shipment.events.dlq
```

- Включены publish-подтверждения; модель доставки at-least-once.
- Ручные (consumer) подтверждения; ограниченный экспоненциальный retry, затем DLQ.
- Consumers идемпотентны (дедупликация по `eventId`).

## 11. Схема базы данных

См. `src/main/resources/db/migration/V1__create_shipments.sql`. Таблицы:

- `shipments` — агрегат, уникальный трек-номер, check-ограничения, `TIMESTAMPTZ`,
  `version` для оптимистичной блокировки, индексы на `(status, created_at)`.
- `outbox_events` — PENDING/PUBLISHED/FAILED, уникальный `event_id`, поля ретраев,
  индекс для выборки.
- `idempotency_keys` — уникальный ключ, состояние `IN_FLIGHT`/`COMPLETED`, `expires_at`.
- `tracking_number_seq` — последовательность для трек-номеров `SLV-YYYY-NNNNNN`.

Схемой управляет только Flyway (`ddl-auto=validate`).

## 12. Паттерн Outbox

Мутирующие use cases пишут агрегат и строку outbox в одной транзакции БД; планировщик
выбирает подлежащие обработке строки (`FOR UPDATE SKIP LOCKED`), публикует в RabbitMQ и
помечает их `PUBLISHED`. При сбое публикации события повторяются с backoff до предела,
после чего помечаются `FAILED` (описано в `ADR-004-transactional-outbox.md`).

## 13. Модель безопасности

- OAuth2 resource server; JWT проверяется по JWK-набору Keycloak (подпись, issuer,
  audience, срок действия).
- RBAC-роли и разрешения:

| Роль | Разрешения |
|---|---|
| `LOGISTICS_USER` | создание отправлений, отмена своих |
| `LOGISTICS_OPERATOR` | просмотр всех, смена статуса, назначение pickup |
| `LOGISTICS_ADMIN` | полный доступ |

- Stateless Bearer-аутентификация; DTO (без сущностей); Bean Validation; секреты не
  логируются; безопасные ответы при ошибках; CORS только при необходимости.

## 14. Тестирование

- **Юнит-тесты**: `ShipmentTest` (конечный автомат), `ValueObjectsTest`,
  `ShipmentApplicationServiceTest` (идемпотентность, outbox), Mockito.
- **Интеграционные** (`src/test/java/.../` `*IT`): Testcontainers для PostgreSQL,
  RabbitMQ и **реального Keycloak** — покрыты в `ShipmentApiIT`, `MessagingIT`,
  `SecurityIT`.

Запуск юнит-тестов:

```bash
mvn test
```

Запуск интеграционных тестов (нужен запущенный Docker Desktop):

```bash
mvn -Pintegration-test verify
```

## 15. Конфигурация

Профили: `application.yml` (базовый), `application-local.yml`,
`application-test.yml`, `application-prod.yml`. Все секреты берутся из переменных
окружения:

```
DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD
RABBITMQ_HOST, RABBITMQ_USERNAME, RABBITMQ_PASSWORD
KEYCLOAK_ISSUER_URI
SERVER_PORT
```

Профиль `prod` падает быстро при отсутствии необходимых переменных окружения.
`ddl-auto=validate` везде.

## 16. Аспекты production-эксплуатации

- **Надёжность**: outbox, publish-подтверждения, DLQ, ограниченные ретраи, graceful
  shutdown, оптимистичная блокировка, идемпотентность.
- **Безопасность**: OAuth2/OIDC, RBAC, Bean Validation, безопасные значения по
  умолчанию, принцип наименьших привилегий, конфигурация без секретов.
- **Производительность**: пагинация + индексы, размер пула соединений, prefetch
  RabbitMQ, отсутствие N+1 (явные fetch-запросы).
- **Наблюдаемость**: `/actuator/health/{liveness,readiness}`, метрики Prometheus, трассировка
  OTel, структурированные логи с `traceId/spanId/correlationId`.
- **Сопровождаемость**: небольшие классы, SOLID, чистая архитектура, ADR.

## 17. Известные ограничения

- Хранилище идемпотентности consumer'а — in-memory (single-instance); для
  горизонтального масштабирования нужен общий стор (см. `ADR-004` /
  `InMemoryProcessedEventStore`).
- `estimatedDeliveryDate` валидируется, но не сверяется с SLA по типу доставки
  (вынесено за рамки).
- Резервное ограничение скорости (429) не включено; маппинг ошибок его поддерживает.
- Эндпоинт экспорта OpenTelemetry не настроен (спаны генерируются, но для отправки в
  бэкенд нужен экспортёр, например OTLP).
- Нет фоновой очистки/архивации старых строк outbox (только метрика).

## 18. Возможные улучшения

- Джоба удержания/очистки outbox с архивацией.
- Событийная интеграция use case *назначения pickup*.
- Распределённый (Redis/Postgres) стор дедупликации consumer'а.
- Kubernetes-манифесты + helm chart.
- Circuit breaker вокруг внешних (не-брокерских) вызовов.
- Rate limiter (Bucket4j) и журнал аудита.
- Точная настройка экспортёра OpenTelemetry (OTLP).
