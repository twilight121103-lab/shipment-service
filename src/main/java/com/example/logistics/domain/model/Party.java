package com.example.logistics.domain.model;

import java.util.Objects;

/**
 * A party (person or company) involved in a shipment: the sender or the recipient.
 */
public record Party(String name, String phone, String email) {

    public Party {
        Objects.requireNonNull(name, "name must not be null");
        if (name.isBlank() || name.length() > 200) {
            throw new IllegalArgumentException("name must be between 1 and 200 characters");
        }
        if (email != null && email.length() > 200) {
            throw new IllegalArgumentException("email must not exceed 200 characters");
        }
        name = name.trim();
    }
}
