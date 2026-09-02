# ADR-007: Наблюдаемость

## Статус
Принято

## Контекст
Сервис должен быть эксплуатируемым в production: health, метрики, структурированные
логи и трассировка запросов, без раскрытия чувствительных данных.

## Решение
Использовать Spring Boot Actuator + Micrometer + OpenTelemetry + структурированное
логирование.

- **Health**: группы проверок `/actuator/health/liveness` и
  `/actuator/health/readiness`. Readiness включает health-индикаторы `db` и `rabbit`;
  liveness — это состояние виртуальной машины JVM. Детали скрыты
  (`show-details: never`), чтобы не раскрывать строки подключения.
- **Метрики** (Prometheus на `/actuator/prometheus`):
  - Количество/задержка/ошибки HTTP-запросов — нормализованные `http.server.requests`
    Micrometer.
  - Пул соединений БД — Micrometer bindings HikariCP (регистрируются автоматически).
  - Сбои публикации в RabbitMQ и состояние consumer'ов — счётчики, подключённые в
    `OutboxPublisher` (`outbox.events.published`, `publish_failures`,
    `permanent_failures`) и gauge `outbox.events.pending`.
- **Трассировка**: starter OpenTelemetry инструментирует HTTP/JDBC/RabbitMQ,
  распространяя `traceId` / `spanId`.
- **Correlation ID**: сервлетный фильтр читает/распространяет `X-Correlation-Id`,
  связывая его с MDC; ответы об ошибках возвращают его.
- **Структурированное логирование**: паттерн логов встраивает `traceId`, `spanId`,
  `correlationId`; профиль prod пишет в stdout (готово к JSON-агрегации через
  container log drivers).

Чувствительные данные (токены, пароли, персональные данные без необходимости)
никогда не логируются.

## Альтернативы
- **Только Zipkin-трассировка**: OpenTelemetry выбран за вендорнейтральность и
  автоинструментирование.
- **Собственный формат метрик**: Micrometer/Prometheus стандартны и из коробки
  интегрируются с Grafana.

## Последствия
- Поверх можно развернуть стандартные дашборды и алертинг (Prometheus + Grafana + Loki).
- Минимум дополнительного кода времени выполнения; большая часть инструментирования —
  авто-конфигурация.
- Correlation-фильтр добавляет UUID на запрос, если он не передан (стоимостью можно
  пренебречь).
