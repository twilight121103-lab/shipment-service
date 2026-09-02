package com.example.logistics.domain.model;

import java.util.Objects;

/**
 * Value object, описывающий физические габариты отправления.
 *
 * <p>Все значения должны быть строго положительными. Длины выражаются в сантиметрах.
 * Предоставляет удобные фабричные методы для типичных «коробочных» габаритов.
 */
public record Dimensions(double lengthCm, double widthCm, double heightCm, double weightKg) {

    public Dimensions {
        if (lengthCm <= 0 || widthCm <= 0 || heightCm <= 0) {
            throw new IllegalArgumentException("dimensions must all be strictly positive");
        }
        if (weightKg <= 0) {
            throw new IllegalArgumentException("weight must be strictly positive");
        }
    }

    /**
     * Создаёт value object {@code Dimensions}. Null-safe перегрузка канонического конструктора.
     *
     * @return новый экземпляр; {@code null} недопустим.
     */
    public static Dimensions of(double lengthCm, double widthCm, double heightCm, double weightKg) {
        return new Dimensions(lengthCm, widthCm, heightCm, weightKg);
    }

    public Dimensions withWeightKg(double newWeightKg) {
        return new Dimensions(lengthCm, widthCm, heightCm, newWeightKg);
    }

    /** Объёмный вес в килограммах (для тарифных расчётов грузоперевозок). */
    public double volumetricWeightKg() {
        final double cbm = (lengthCm * widthCm * heightCm) / 1_000_000.0;
        return cbm * 250.0; // стандартный делитель объёмного веса 250 кг/м^3
    }
}
