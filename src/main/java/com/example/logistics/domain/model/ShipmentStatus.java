package com.example.logistics.domain.model;

/**
 * Состояния жизненного цикла отправления.
 *
 * <p>Агрегат {@link Shipment} обеспечивает допустимость переходов между этими
 * состояниями через {@link Shipment#changeStatus(ShipmentStatus, String)}. Правила,
 * реализованные здесь:
 * <ul>
 *     <li>Основной поток строго прямой: CREATED -&gt; CONFIRMED -&gt; PICKUP_ASSIGNED
 *         -&gt; PICKED_UP -&gt; IN_TRANSIT -&gt; OUT_FOR_DELIVERY -&gt; DELIVERED.</li>
 *     <li>Отправление можно ОТМЕНИТЬ (CANCELLED) только пока оно ещё не доставлено и не терминально.</li>
 *     <li>Доставка может не УДАСТЬСЯ (FAIL) из состояний IN_TRANSIT / OUT_FOR_DELIVERY.</li>
 *     <li>Состояние RETURNED достижимо из OUT_FOR_DELIVERY / DELIVERY_FAILED.</li>
 *     <li>Повтор того же перехода или движение назад запрещены.</li>
 * </ul>
 */
public enum ShipmentStatus {

    CREATED,
    CONFIRMED,
    PICKUP_ASSIGNED,
    PICKED_UP,
    IN_TRANSIT,
    OUT_FOR_DELIVERY,
    DELIVERED,
    DELIVERY_FAILED,
    RETURNED,
    CANCELLED;

    /** Состояния, после которых дальнейшая смена статуса недопустима. */
    public boolean isTerminal() {
        return this == DELIVERED || this == RETURNED || this == CANCELLED;
    }
}
