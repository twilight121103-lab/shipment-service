package com.example.logistics.infrastructure.persistence;

import com.example.logistics.domain.model.TrackingNumber;
import com.example.logistics.domain.repository.TrackingNumberGenerator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Year;

/**
 * Generates tracking numbers from a dedicated DB sequence, producing a readable,
 * collision-free value of the form {@code SLV-<year>-<6+ digits>}.
 *
 * <p>Using a PostgreSQL sequence gives uniqueness that would be hard (and racy) to
 * reproduce in application memory under concurrency.
 */
@Component
public class SequenceTrackingNumberGenerator implements TrackingNumberGenerator {

    private static final String PREFIX = "SLV";

    private final JdbcTemplate jdbcTemplate;

    public SequenceTrackingNumberGenerator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public TrackingNumber next() {
        final long seq = jdbcTemplate.queryForObject("SELECT nextval('tracking_number_seq')", Long.class);
        return TrackingNumber.of("%s-%d-%06d".formatted(PREFIX, Year.now().getValue(), seq));
    }
}
