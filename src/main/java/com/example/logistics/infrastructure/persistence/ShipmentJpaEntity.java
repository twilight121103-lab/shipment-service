package com.example.logistics.infrastructure.persistence;

import com.example.logistics.domain.model.DeliveryType;
import com.example.logistics.domain.model.ShipmentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * JPA persistence representation of the {@code Shipment} aggregate.
 *
 * <p>Deliberately separated from the domain aggregate: the domain layer stays
 * framework-free, and this entity is an adapter detail. It is never exposed through
 * the REST API.
 */
@Entity
@Table(name = "shipments")
public class ShipmentJpaEntity {

    @Id
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "tracking_number", unique = true)
    private String trackingNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ShipmentStatus status;

    // sender
    @Column(name = "sender_name", nullable = false)
    private String senderName;
    @Column(name = "sender_phone")
    private String senderPhone;
    @Column(name = "sender_email")
    private String senderEmail;

    // recipient
    @Column(name = "recipient_name", nullable = false)
    private String recipientName;
    @Column(name = "recipient_phone")
    private String recipientPhone;
    @Column(name = "recipient_email")
    private String recipientEmail;

    // pickup address
    @Column(name = "pickup_street", nullable = false)
    private String pickupStreet;
    @Column(name = "pickup_city", nullable = false)
    private String pickupCity;
    @Column(name = "pickup_postal_code", nullable = false)
    private String pickupPostalCode;
    @Column(name = "pickup_country", nullable = false)
    private String pickupCountry;

    // delivery address
    @Column(name = "delivery_street", nullable = false)
    private String deliveryStreet;
    @Column(name = "delivery_city", nullable = false)
    private String deliveryCity;
    @Column(name = "delivery_postal_code", nullable = false)
    private String deliveryPostalCode;
    @Column(name = "delivery_country", nullable = false)
    private String deliveryCountry;

    // dimensions
    @Column(name = "length_cm", nullable = false)
    private double lengthCm;
    @Column(name = "width_cm", nullable = false)
    private double widthCm;
    @Column(name = "height_cm", nullable = false)
    private double heightCm;
    @Column(name = "weight_kg", nullable = false)
    private double weightKg;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_type", nullable = false)
    private DeliveryType deliveryType;

    @Column(name = "estimated_delivery_date", nullable = false)
    private LocalDate estimatedDeliveryDate;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected ShipmentJpaEntity() {
        // for JPA
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public ShipmentStatus getStatus() {
        return status;
    }

    public void setStatus(ShipmentStatus status) {
        this.status = status;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderPhone() {
        return senderPhone;
    }

    public void setSenderPhone(String senderPhone) {
        this.senderPhone = senderPhone;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getRecipientPhone() {
        return recipientPhone;
    }

    public void setRecipientPhone(String recipientPhone) {
        this.recipientPhone = recipientPhone;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public String getPickupStreet() {
        return pickupStreet;
    }

    public void setPickupStreet(String pickupStreet) {
        this.pickupStreet = pickupStreet;
    }

    public String getPickupCity() {
        return pickupCity;
    }

    public void setPickupCity(String pickupCity) {
        this.pickupCity = pickupCity;
    }

    public String getPickupPostalCode() {
        return pickupPostalCode;
    }

    public void setPickupPostalCode(String pickupPostalCode) {
        this.pickupPostalCode = pickupPostalCode;
    }

    public String getPickupCountry() {
        return pickupCountry;
    }

    public void setPickupCountry(String pickupCountry) {
        this.pickupCountry = pickupCountry;
    }

    public String getDeliveryStreet() {
        return deliveryStreet;
    }

    public void setDeliveryStreet(String deliveryStreet) {
        this.deliveryStreet = deliveryStreet;
    }

    public String getDeliveryCity() {
        return deliveryCity;
    }

    public void setDeliveryCity(String deliveryCity) {
        this.deliveryCity = deliveryCity;
    }

    public String getDeliveryPostalCode() {
        return deliveryPostalCode;
    }

    public void setDeliveryPostalCode(String deliveryPostalCode) {
        this.deliveryPostalCode = deliveryPostalCode;
    }

    public String getDeliveryCountry() {
        return deliveryCountry;
    }

    public void setDeliveryCountry(String deliveryCountry) {
        this.deliveryCountry = deliveryCountry;
    }

    public double getLengthCm() {
        return lengthCm;
    }

    public void setLengthCm(double lengthCm) {
        this.lengthCm = lengthCm;
    }

    public double getWidthCm() {
        return widthCm;
    }

    public void setWidthCm(double widthCm) {
        this.widthCm = widthCm;
    }

    public double getHeightCm() {
        return heightCm;
    }

    public void setHeightCm(double heightCm) {
        this.heightCm = heightCm;
    }

    public double getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(double weightKg) {
        this.weightKg = weightKg;
    }

    public DeliveryType getDeliveryType() {
        return deliveryType;
    }

    public void setDeliveryType(DeliveryType deliveryType) {
        this.deliveryType = deliveryType;
    }

    public LocalDate getEstimatedDeliveryDate() {
        return estimatedDeliveryDate;
    }

    public void setEstimatedDeliveryDate(LocalDate estimatedDeliveryDate) {
        this.estimatedDeliveryDate = estimatedDeliveryDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }
}
