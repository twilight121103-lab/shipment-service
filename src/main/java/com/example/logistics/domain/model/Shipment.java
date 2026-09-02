package com.example.logistics.domain.model;

import com.example.logistics.domain.exception.IllegalStateTransitionException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Агрегатный корень, представляющий отправление и обеспечивающий его жизненный цикл.
 *
 * <p>Агрегат содержит все бизнес-правила (конечный автомат), чтобы поведение можно было
 * тестировать изолированно и переиспользовать любым адаптером. Он ничего не знает о
 * Spring, JPA или обмене сообщениями.
 */
public class Shipment {

    private final UUID id;
    private TrackingNumber trackingNumber;
    private ShipmentStatus status;

    private Party sender;
    private Party recipient;
    private Address pickupAddress;
    private Address deliveryAddress;

    private Dimensions dimensions;
    private DeliveryType deliveryType;
    private LocalDate estimatedDeliveryDate;

    private Instant createdAt;
    private Instant updatedAt;

    /** Счётчик версий для оптимистичной блокировки. Управляется слоем персистентности. */
    private long version;

    protected Shipment() {
        this.id = UUID.randomUUID();
    }

    private Shipment(Builder b) {
        this.id = b.id != null ? b.id : UUID.randomUUID();
        this.trackingNumber = b.trackingNumber;
        this.status = b.status != null ? b.status : ShipmentStatus.CREATED;
        this.sender = Objects.requireNonNull(b.sender);
        this.recipient = Objects.requireNonNull(b.recipient);
        this.pickupAddress = Objects.requireNonNull(b.pickupAddress);
        this.deliveryAddress = Objects.requireNonNull(b.deliveryAddress);
        this.dimensions = Objects.requireNonNull(b.dimensions);
        this.deliveryType = Objects.requireNonNull(b.deliveryType);
        this.estimatedDeliveryDate = Objects.requireNonNull(b.estimatedDeliveryDate);
        this.createdAt = b.createdAt != null ? b.createdAt : Instant.now();
        this.updatedAt = this.createdAt;
    }

    /** Фабрика для нового отправления, создаваемого клиентским запросом. */
    public static Shipment create(Party sender, Party recipient, Address pickupAddress,
                                  Address deliveryAddress, Dimensions dimensions,
                                  DeliveryType deliveryType, LocalDate estimatedDeliveryDate) {
        return new Builder()
                .sender(sender)
                .recipient(recipient)
                .pickupAddress(pickupAddress)
                .deliveryAddress(deliveryAddress)
                .dimensions(dimensions)
                .deliveryType(deliveryType)
                .estimatedDeliveryDate(estimatedDeliveryDate)
                .build();
    }

    /** Фабрика, используемая при восстановлении из персистентности (из репозитория). */
    public static Shipment reconstitute(UUID id, TrackingNumber trackingNumber, ShipmentStatus status,
                                        Party sender, Party recipient, Address pickupAddress, Address deliveryAddress,
                                        Dimensions dimensions, DeliveryType deliveryType,
                                        LocalDate estimatedDeliveryDate, Instant createdAt, Instant updatedAt,
                                        long version) {
        return new Builder()
                .id(id)
                .trackingNumber(trackingNumber)
                .status(status)
                .sender(sender)
                .recipient(recipient)
                .pickupAddress(pickupAddress)
                .deliveryAddress(deliveryAddress)
                .dimensions(dimensions)
                .deliveryType(deliveryType)
                .estimatedDeliveryDate(estimatedDeliveryDate)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .version(version)
                .build();
    }

    /**
     * Пытается перевести отправление в указанный статус.
     *
     * @throws IllegalStateTransitionException если переход нарушает бизнес-правила.
     */
    public void changeStatus(ShipmentStatus target) {
        if (this.status == target) {
            throw new IllegalStateTransitionException(
                    "Cannot repeat transition to the same status '%s'".formatted(target));
        }
        if (!transitionAllowed(this.status, target)) {
            throw new IllegalStateTransitionException(
                    "Transition from '%s' to '%s' is not allowed".formatted(this.status, target));
        }
        this.status = target;
        this.updatedAt = Instant.now();
    }

    /**
     * Отменяет отправление согласно бизнес-правилам. Доставленное или уже терминальное
     * отправление нельзя отменить.
     */
    public void cancel(String reason) {
        if (this.status.isTerminal()) {
            throw new IllegalStateTransitionException(
                    "Cannot cancel a shipment in terminal state '%s'".formatted(this.status));
        }
        changeStatus(ShipmentStatus.CANCELLED);
    }

    /**
     * Присваивает трек-номер. Возможно только пока отправление в состоянии CREATED
     * и номер ещё не присвоен.
     */
    public void assignTrackingNumber(TrackingNumber trackingNumber) {
        if (this.status != ShipmentStatus.CREATED) {
            throw new IllegalStateTransitionException(
                    "Cannot assign a tracking number to a shipment in state '%s'".formatted(this.status));
        }
        if (this.trackingNumber != null) {
            throw new IllegalStateTransitionException("Tracking number is already assigned");
        }
        this.trackingNumber = Objects.requireNonNull(trackingNumber);
        this.updatedAt = Instant.now();
    }

    // ------------------------------------------------------------------
    // Конечный автомат
    // ------------------------------------------------------------------

    private static boolean transitionAllowed(ShipmentStatus from, ShipmentStatus to) {
        return switch (from) {
            case CREATED -> to == ShipmentStatus.CONFIRMED || to == ShipmentStatus.CANCELLED;
            case CONFIRMED -> to == ShipmentStatus.PICKUP_ASSIGNED || to == ShipmentStatus.CANCELLED;
            case PICKUP_ASSIGNED -> to == ShipmentStatus.PICKED_UP || to == ShipmentStatus.CANCELLED;
            case PICKED_UP -> to == ShipmentStatus.IN_TRANSIT;
            case IN_TRANSIT -> to == ShipmentStatus.OUT_FOR_DELIVERY
                    || to == ShipmentStatus.DELIVERY_FAILED;
            case OUT_FOR_DELIVERY -> to == ShipmentStatus.DELIVERED
                    || to == ShipmentStatus.DELIVERY_FAILED
                    || to == ShipmentStatus.RETURNED;
            case DELIVERY_FAILED -> to == ShipmentStatus.RETURNED
                    || to == ShipmentStatus.OUT_FOR_DELIVERY;   // попытка повторной доставки
            case DELIVERED, RETURNED, CANCELLED -> false;        // терминальные состояния
        };
    }

    // ------------------------------------------------------------------
    // Доступ к полям (accessors)
    // ------------------------------------------------------------------

    public UUID getId() {
        return id;
    }

    public TrackingNumber getTrackingNumber() {
        return trackingNumber;
    }

    public ShipmentStatus getStatus() {
        return status;
    }

    public Party getSender() {
        return sender;
    }

    public Party getRecipient() {
        return recipient;
    }

    public Address getPickupAddress() {
        return pickupAddress;
    }

    public Address getDeliveryAddress() {
        return deliveryAddress;
    }

    public Dimensions getDimensions() {
        return dimensions;
    }

    public DeliveryType getDeliveryType() {
        return deliveryType;
    }

    public LocalDate getEstimatedDeliveryDate() {
        return estimatedDeliveryDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }

    /**
     * Изменяемый builder. Не «флуidно-пирамидальный»: открывает только поля, которые
     * имеет смысл задавать при конструировании, сохраняя агрегат неизменяемым далее.
     */
    public static class Builder {
        private UUID id;
        private TrackingNumber trackingNumber;
        private ShipmentStatus status;
        private Party sender;
        private Party recipient;
        private Address pickupAddress;
        private Address deliveryAddress;
        private Dimensions dimensions;
        private DeliveryType deliveryType;
        private LocalDate estimatedDeliveryDate;
        private Instant createdAt;
        private Instant updatedAt;
        private long version;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder trackingNumber(TrackingNumber trackingNumber) { this.trackingNumber = trackingNumber; return this; }
        public Builder status(ShipmentStatus status) { this.status = status; return this; }
        public Builder sender(Party sender) { this.sender = sender; return this; }
        public Builder recipient(Party recipient) { this.recipient = recipient; return this; }
        public Builder pickupAddress(Address pickupAddress) { this.pickupAddress = pickupAddress; return this; }
        public Builder deliveryAddress(Address deliveryAddress) { this.deliveryAddress = deliveryAddress; return this; }
        public Builder dimensions(Dimensions dimensions) { this.dimensions = dimensions; return this; }
        public Builder deliveryType(DeliveryType deliveryType) { this.deliveryType = deliveryType; return this; }
        public Builder estimatedDeliveryDate(LocalDate estimatedDeliveryDate) { this.estimatedDeliveryDate = estimatedDeliveryDate; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
        public Builder version(long version) { this.version = version; return this; }

        public Shipment build() {
            return new Shipment(this);
        }
    }
}
