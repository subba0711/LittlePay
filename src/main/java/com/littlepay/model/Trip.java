package com.littlepay.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a matched passenger journey with its fare and status.
 */
public record Trip(
        LocalDateTime started,
        LocalDateTime finished,
        long durationSecs,
        String fromStopId,
        String toStopId,
        BigDecimal chargeAmount,
        String companyId,
        String busId,
        String pan,
        TripStatus status) {
}
