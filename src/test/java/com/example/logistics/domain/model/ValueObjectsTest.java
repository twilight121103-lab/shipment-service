package com.example.logistics.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValueObjectsTest {

    @Test
    void trackingNumberNormalizesAndValidates() {
        assertThat(TrackingNumber.of("slv-2026-000042").value()).isEqualTo("SLV-2026-000042");
        assertThatThrownBy(() -> TrackingNumber.of("not-valid")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TrackingNumber.of(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void addressRejectsBadCountry() {
        assertThatThrownBy(() -> new Address("s", "c", "SW1A", "GBR1"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Address("s", "c", "SW1A", ""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addressTrimsAndUpperCasesCountry() {
        Address a = new Address(" s ", " c ", "SW1A", "gb ");
        assertThat(a.street()).isEqualTo("s");
        assertThat(a.country()).isEqualTo("GB");
    }

    @Test
    void dimensionsMustBePositive() {
        assertThatThrownBy(() -> new Dimensions(-1, 20, 10, 2)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Dimensions(10, 20, 10, 0)).isInstanceOf(IllegalArgumentException.class);
        Dimensions d = new Dimensions(50, 40, 30, 10);
        assertThat(d.volumetricWeightKg()).isEqualTo(15.0); // 0.06 m3 * 250
    }

    @Test
    void volumetricWeightCompute() {
        // 100 x 100 x 100 cm = 1.0 cubic metre
        Dimensions d = new Dimensions(100, 100, 100, 5);
        assertThat(d.volumetricWeightKg()).isEqualTo(250.0); // 1.0 m3 * 250
    }

    @Test
    void partyValidation() {
        assertThatThrownBy(() -> new Party("", "1", "a@b.com")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Party(null, "1", null)).isInstanceOf(NullPointerException.class);
    }
}
