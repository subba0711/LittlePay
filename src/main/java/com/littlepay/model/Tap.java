package com.littlepay.model;

import java.time.LocalDateTime;

/**
 * Represents a single bus tap event (ON or OFF).
 */
public record Tap(
        long id,
        LocalDateTime dateTime,
        TapType tapType,
        String stopId,
        String companyId,
        String busId,
        String pan) {
}
