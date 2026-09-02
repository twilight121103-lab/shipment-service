# ADR-003: Сообщения в RabbitMQ

## Статус
Принято

## Контекст
Сервис должен публиковать доменные события (`ShipmentCreated`, `ShipmentStatusChanged`,
`ShipmentCancelled`, `ShipmentDelivered`) и потреблять события. RabbitMQ обеспечивает
модель доставки at-least-once, что соответствует нашим требованиям к надёжности.
Нужна удобная топология с ограниченным retry и dead-lettering, а consumers должны быть
идемпотентны.

## Решение
Использовать **RabbitMQ** (Spring AMQP) со следующей топологией:

- Один durable **direct exchange** `logistics.events.direct`.
- Одна **durable queue** `q.shipment.events`, связанная с routing-ключами
  `shipment.event.ShipmentCreated|ShipmentStatusChanged|ShipmentCancelled|ShipmentDelivered`.
- **Dead-letter exchange** `logistics.events.dlx` и **DLQ**
  `q.shipment.events.dlq`; основная очередь настроена с
  `x-dead-letter-exchange` / `x-dead-letter-routing-key`, чтобы отклонённые сообщения
  попадали в DLQ.
- Publish-подтверждения (`publisher-confirm-type: correlated`), чтобы успешная
  публикация гарантировала принятие сообщения брокером.
- Ручные (consumer) подтверждения: сообщение подтверждается (ack) только после
  обработки.
- Ограниченный, экспоненциальный retry через `RetryOperationsInterceptor`
  (`max-attempts`), далее DLQ при постоянном сбое — poison-сообщения не крутятся
  бесконечно.

Модель доставки явно **at-least-once**; мы никогда не рассчитываем на exactly-once.
Consumers дедуплицируются через стор обработанных событий, чтобы нейтрализовать
повторные доставки.

## Альтернативы
- **Kafka**: даёт более строгую упорядоченность/партиционирование, но тяжелее в
  эксплуатации; RabbitMQ уже есть в стеке организации.
- **Exactly-once через транзакции брокера**: с RabbitMQ недостижимо; отклонено.
- **Auto-ack**: отклонено; при сбое обработки события были бы потеряны.

## Последствия
- Ограниченный retry + DLQ дают ясную семантику надёжности.
- Публикация надёжна (подтверждения), потребление идемпотентно.
- Модель at-least-once означает, что consumers обязаны дедуплицироваться (они это
  делают, см. ADR по consumer) и корректно обрабатывать дубликаты.
