package com.littlepay.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * Calculates trip fares based on the stops travelled between.
 */
public class FareCalculator {

    private final Map<String, Map<String, BigDecimal>> fares = new HashMap<>();

    public FareCalculator() {
        addFare("Stop1", "Stop2", "3.25");
        addFare("Stop2", "Stop3", "5.50");
        addFare("Stop1", "Stop3", "7.30");
    }

    public BigDecimal completedFare(String fromStopId, String toStopId) {
        BigDecimal fare = fares.getOrDefault(fromStopId, Map.of()).get(toStopId);
        if (fare == null) {
            throw new IllegalArgumentException("No fare defined between " + fromStopId + " and " + toStopId);
        }
        return fare;
    }

    public BigDecimal maxFareFrom(String stopId) {
        return fares.getOrDefault(stopId, Map.of()).values().stream()
                .max(Comparator.naturalOrder())
                .orElseThrow(() -> new IllegalArgumentException("Unknown stop: " + stopId));
    }

    private void addFare(String stopA, String stopB, String amount) {
        BigDecimal fare = new BigDecimal(amount);
        fares.computeIfAbsent(stopA, k -> new HashMap<>()).put(stopB, fare);
        fares.computeIfAbsent(stopB, k -> new HashMap<>()).put(stopA, fare);
    }
}
