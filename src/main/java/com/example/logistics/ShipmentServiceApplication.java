package com.example.logistics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point of the Shipment Service microservice.
 *
 * <p>The application is split into four layers:
 * <ul>
 *     <li>{@code domain} - pure business logic, no framework dependencies</li>
 *     <li>{@code application} - use cases / orchestration (service layer)</li>
 *     <li>{@code infrastructure} - persistence, messaging, security adapters</li>
 *     <li>{@code interfaces} - REST controllers and API error mapping</li>
 * </ul>
 */
@SpringBootApplication
@EnableScheduling
public class ShipmentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShipmentServiceApplication.class, args);
    }
}
