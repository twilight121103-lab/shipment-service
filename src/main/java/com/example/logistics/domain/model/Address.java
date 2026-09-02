package com.example.logistics.domain.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value object for a postal address.
 *
 * <p>Using a dedicated type avoids primitive obsession and lets the domain validate
 * address shape once, rather than scattering string checks across services.
 */
public record Address(
        String street,
        String city,
        String postalCode,
        String country) {

    private static final Pattern POSTAL_CODE = Pattern.compile("^[\\p{Alnum} .-]{3,10}$");

    public Address {
        Objects.requireNonNull(street, "street must not be null");
        Objects.requireNonNull(city, "city must not be null");
        Objects.requireNonNull(country, "country must not be null");
        if (street.isBlank() || street.length() > 200) {
            throw new IllegalArgumentException("street must be between 1 and 200 characters");
        }
        if (city.isBlank() || city.length() > 100) {
            throw new IllegalArgumentException("city must be between 1 and 100 characters");
        }
        if (postalCode == null || !POSTAL_CODE.matcher(postalCode).matches()) {
            throw new IllegalArgumentException("postalCode must contain 3-10 alphanumeric characters");
        }
        country = country.trim().toUpperCase();
        final int countryLen = country.length();
        if (country.isBlank() || (countryLen != 2 && countryLen != 3) || !country.chars().allMatch(Character::isLetter)) {
            throw new IllegalArgumentException("country must be a 2 or 3 letter ISO code");
        }
        street = street.trim();
        city = city.trim();
    }
}
