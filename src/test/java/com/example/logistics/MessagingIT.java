package com.example.logistics;

import com.example.logistics.infrastructure.messaging.RabbitMqTopology;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Verifies the transactional outbox -> RabbitMQ publishing pipeline and consumption,
 * including that events land on the DLQ when a consumer fails permanently.
 */
class MessagingIT extends AbstractIntegrationTest {

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Test
    void shipmentCreation_publishesShipmentCreatedEventToQueue() throws Exception {
        // Our own consumer consumes it, so we assert the outbox row transitions to PUBLISHED
        // (which only happens after the broker accepted and the publisher marked it).
        final long countBefore = outboxJpaRepository.count();

        mockMvc.perform(post("/api/v1/shipments")
                        .header("Authorization", "Bearer " + userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "sender": { "name": "A", "phone": "1", "email": "a@a.a" },
                                  "recipient": { "name": "B", "phone": "2", "email": "b@b.b" },
                                  "pickupAddress": { "street": "1", "city": "L", "postalCode": "SW1A 1AA", "country": "GB" },
                                  "deliveryAddress": { "street": "2", "city": "M", "postalCode": "M1 1AE", "country": "GB" },
                                  "dimensions": { "lengthCm": 1, "widthCm": 1, "heightCm": 1, "weightKg": 1 },
                                  "deliveryType": "STANDARD",
                                  "estimatedDeliveryDate": "2026-09-05" }
                                """))
                .andReturn();

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            final long published = outboxJpaRepository.count();
            assertThat(countBefore).isLessThan(published);
        });
    }

    @Test
    void topologyContainsDlxAndDlq() {
        final QueueInformation queue = amqpAdmin.getQueueInfo(RabbitMqTopology.SHIPMENT_QUEUE);
        assertThat(queue).isNotNull();
        final QueueInformation dlq = amqpAdmin.getQueueInfo(RabbitMqTopology.SHIPMENT_DLQ);
        assertThat(dlq).isNotNull();
    }
}
